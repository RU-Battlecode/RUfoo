package RUfoo.logic;

import java.util.Arrays;

import RUfoo.managers.Channel;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;

/*
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
 * 2. Go clear trees?
 * 
 * Always: Protect the base. 
 * 
 */

public class LumberjackLogic extends RobotLogic {

	private static final float HOME_BASE_RADIUS = 5.0f;

	private boolean nothingAtEnemySpawn;
	private MapLocation enemySpawn;
	private TreeInfo targetTree;
	private int treeFrustration;

	public LumberjackLogic(RobotController _rc) {
		super(_rc);
		enemySpawn = combat.getClosestEnemySpawn();
		nothingAtEnemySpawn = false;
		
		targetTree = null;
		treeFrustration = 0;
	}
	
	@Override
	public void logic() {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());

		moveOffSpawn();

		RobotInfo target = combat.findMeleeTarget(enemies);

		combat.meleeAttack(enemies);

		if (target != null) {
			moveTowards(target);
		}

		clearTreesAroundBase(robots);
		
		if (!rc.hasAttacked() && target == null) {
			checkRadioTreeChannel();

			nav.moveByTrees(false);

			if (!rc.hasMoved()) {

				if (rc.getLocation().distanceTo(enemySpawn) <= 1.0f || nothingAtEnemySpawn) {
					nothingAtEnemySpawn = true;
					nav.moveByTrees(false);
					nav.moveRandom();
				} else {
					nav.moveBest(rc.getLocation().directionTo(enemySpawn));
				}
			}
		}

	}

	void moveOffSpawn() {
		if (personality.getMother() != null) {
			if (rc.getLocation().distanceTo(personality.getMother().location) <= rc.getType().sensorRadius) {
				nav.moveBest(personality.getMother().location.directionTo(rc.getLocation()));
			}
		}
	}

	void clearTreesAroundBase(RobotInfo[] robots) {
		TreeInfo[] trees = rc.senseNearbyTrees();
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

	void checkRadioTreeChannel() {
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
				nav.moveBest(rc.getLocation().directionTo(requestedTree));
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
			
				nav.tryMove(tree.location);
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
			nav.moveBest(rc.getLocation().directionTo(target.location));
			//nav.moveAggressively(target.location);
		}
	}
}
