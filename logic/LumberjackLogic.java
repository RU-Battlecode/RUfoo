package RUfoo.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import RUfoo.managers.Channel;
import RUfoo.util.Util;
import battlecode.common.BodyInfo;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;

/**
 * LumberjackLogic.java
 * 
 * M: mother (personality.getMother())
 * T: Base team tree.
 * S: Spawn area of base.
 * 
 *     T (Note* this top could be rotated to enemy intial spawn)
 *   T M T
 *   T S T 
 * 
 * 0. Move out of the way
 * 
 * 1. Clear all trees around base.
 * 
 * 2. Read radio help to clear trees
 * 
 * Always: Protect the base.
 * 
 */
public class LumberjackLogic extends RobotLogic {

	private static final float HOME_BASE_RADIUS = 10.0f;

	private TreeInfo targetTree;
	private int treeFrustration;

	private int moveIndex;
	private List<MapLocation> moveAreas;
	private int moveFrustration;
	private float prevousDistanceToTarget;

	public LumberjackLogic(RobotController _rc) {
		super(_rc);
		moveAreas = new ArrayList<>();
		moveIndex = 0;
		moveFrustration = 0;
		prevousDistanceToTarget = 0.0f;

		for (MapLocation loc : rc.getInitialArchonLocations(rc.getTeam().opponent())) {
			moveAreas.add(loc);
		}

		targetTree = null;
		treeFrustration = 0;
	}

	@Override
	public void logic() {
		TreeInfo[] trees = rc.senseNearbyTrees();
		RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());

		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.ARCHON) {
				radio.foundEnemyArchon(enemy);
			}
		}

		moveOffSpawn();

		RobotInfo target = combat.findMeleeTarget(enemies);

		combat.meleeAttack(enemies);

		if (target != null) {
			moveTowards(target);
		}

		clearTreesAroundBase(friends, trees);

		if (!rc.hasAttacked() && target == null) {
			BodyInfo[] obstacles = Util.addAll(friends, trees);
			checkRadioTreeChannel(obstacles);

			if (!rc.hasMoved()) {
				if (moveAreas.size() > 0) {
					move(enemies, trees, friends);
				} else {
					moveAreas.add(rc.getInitialArchonLocations(rc.getTeam())[0]);
					for (MapLocation loc : rc.getInitialArchonLocations(rc.getTeam().opponent())) {
						moveAreas.add(loc);
					}
					nav.moveRandom();
				}
			}
		}

		nav.shakeTrees();
	}

	void moveOffSpawn() {
		if (personality.getMother() != null) {
			if (rc.getLocation().distanceTo(personality.getMother().location) <= rc.getType().sensorRadius / 2) {
				nav.tryHardMove(personality.getMother().location.directionTo(rc.getLocation()));
			}
		}
	}

	void clearTreesAroundBase(RobotInfo[] robots, TreeInfo[] trees) {
		final MapLocation nearest = nearestArchonOrGardener(robots);

		// Closest trees first or closest tree to archon/gardener if they
		// are near!
		if (nearest != null) {
			Arrays.sort(trees, (t1, t2) -> {
				return Math.round(t1.location.distanceSquaredTo(nearest) - t2.location.distanceSquaredTo(nearest));
			});
		}

		clearAllTreesInArea(trees);
	}

	void checkRadioTreeChannel(BodyInfo[] obstacles) {
		MapLocation requestedTree = radio.readTreeChannel();

		if (requestedTree != null && personality.getMother() != null
				&& requestedTree.distanceTo(personality.getMother().location) <= HOME_BASE_RADIUS) {
			if (rc.canChop(requestedTree)) {
				try {
					rc.chop(requestedTree);
					radio.taskComplete(Channel.TREE_CHANNEL);
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			} else {
				nav.bug(requestedTree, obstacles);
			}
		}
	}

	MapLocation nearestArchonOrGardener(RobotInfo[] robots) {
		MapLocation nearest = null;

		for (RobotInfo robot : robots) {
			if (robot.team == rc.getTeam() && robot.type == RobotType.ARCHON || robot.type == RobotType.GARDENER) {
				nearest = robot.location;
				break;
			}
		}
		return nearest;
	}

	void clearAllTreesInArea(TreeInfo[] trees) {
		if (trees.length == 0) {
			return;
		}

		for (TreeInfo tree : trees) {
			if (tree.team == rc.getTeam()) {
				// Don't attack your own trees...
				continue;
			}

			if (rc.canChop(tree.ID)) {
				try {
					rc.chop(tree.ID);
					targetTree = null;
					break;
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
		}

		if (!rc.hasAttacked()) {
			for (TreeInfo tree : trees) {
				if (tree.team == rc.getTeam()) {
					continue;
				}

				if (treeFrustration > personality.getPatience() && targetTree == tree) {
					treeFrustration = 0;
					continue;
				}

				nav.tryMoveTo(tree.location);
				targetTree = tree;
				break;
			}
		}

		if (targetTree != null && !rc.hasAttacked()) {
			treeFrustration++;
		}
	}

	void moveTowards(RobotInfo target) {
		if (rc.canStrike() && rc.getLocation().distanceTo(target.location) <= GameConstants.LUMBERJACK_STRIKE_RADIUS) {
			try {
				rc.strike();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		} else {
			nav.tryHardMove(rc.getLocation().directionTo(target.location));
		}
	}

	void move(RobotInfo[] enemies, TreeInfo[] trees, RobotInfo[] friends) {
		MapLocation loc = moveAreas.get(moveIndex % moveAreas.size());
		float distToTarget = rc.getLocation().distanceSquaredTo(loc);
		BodyInfo[] obstacles = Util.addAll(friends, trees);

		if (rc.getLocation().distanceTo(loc) < 2.0f && enemies.length == 0) {
			if (!nav.closeToArchonLocation(loc)) {
				moveAreas.remove(moveIndex % moveAreas.size());
				moveIndex++;
			}
			moveFrustration++;
		}

		if (moveFrustration > personality.getPatience()) {
			moveIndex++;
			moveFrustration = 0;
		}

		if (obstacles.length > 1) {
			Arrays.sort(obstacles, (b1, b2) -> {
				return Math.round(b1.getLocation().distanceSquaredTo(rc.getLocation())
						- b2.getLocation().distanceSquaredTo(rc.getLocation()));
			});
		}

		nav.bug(loc, obstacles);

		if (Util.equals(distToTarget, prevousDistanceToTarget, rc.getType().strideRadius - 0.1f)) {
			moveFrustration++;
		}

		prevousDistanceToTarget = distToTarget;
	}
}
