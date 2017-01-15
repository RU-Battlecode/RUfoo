package RUfoo.logic;

import java.util.Arrays;

import RUfoo.managers.Radio;
import RUfoo.util.Util;
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

	public LumberjackLogic(RobotController _rc) {
		super(_rc);
	}

	@Override
	public void logic() {
		moveOffSpawn();

		combat.meleeAttackAggressive();
		RobotInfo target = combat.findMeleeTarget();

		if (!rc.hasAttacked()) {
			if (target != null) {
				moveTowards(target);
			}
		}

		checkRadioTreeChannel();
		clearTreesAroundBase();

		if (!rc.hasAttacked() && target == null) {
			nav.moveByTrees(false);

			if (!rc.hasMoved()) {
				RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
				if (Util.contains(robots, RobotType.GARDENER)) {
					nav.moveRandom();
				} else {
					nav.moveBest(rc.getLocation().directionTo(combat.getClosestEnemySpawn()));
				}
			}
		}

	}

	void moveOffSpawn() {
		if (personality.getMother() != null) {
			if (rc.getLocation().distanceTo(personality.getMother().location) <= rc.getType().bodyRadius * 3.0) {
				nav.moveBest(personality.getMother().location.directionTo(rc.getLocation()));
			}
		}
	}

	void clearTreesAroundBase() {
		TreeInfo[] trees = rc.senseNearbyTrees();
		final MapLocation nearest = nearestArchonOrGardener();

		// Closest trees first or closest tree to archon/gardener if they
		// are near!
		Arrays.sort(trees, (t1, t2) -> {
			return Math.round(t1.location.distanceSquaredTo(nearest == null ? rc.getLocation() : nearest)
					- t2.location.distanceSquaredTo(nearest == null ? rc.getLocation() : nearest));
		});
		
		clearAllTreesInArea(trees);
	}

	void checkRadioTreeChannel() {
		MapLocation requestedTree = radio.readTreeChannel();

		if (requestedTree != null && personality.getMother() != null
				&& requestedTree.distanceTo(personality.getMother().location) <= HOME_BASE_RADIUS) {
			if (rc.canChop(requestedTree)) {
				try {
					rc.chop(requestedTree);
					radio.taskComplete(Radio.TREE_CHANNEL);
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			} else {
				nav.moveAggressively(requestedTree);
			}
		}
	}

	MapLocation nearestArchonOrGardener() {
		MapLocation nearest = null;
		RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());

		for (RobotInfo robot : friends) {
			if (robot.type == RobotType.ARCHON || robot.type == RobotType.GARDENER) {
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

				nav.moveAggressively(tree.location);
				break;
			}
		}
	}

	void moveTowards(RobotInfo target) {
		if (rc.getLocation().distanceTo(target.location) <= GameConstants.LUMBERJACK_STRIKE_RADIUS * 2.0) {
			try {
				rc.strike();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		} else {
			nav.moveAggressively(target.location);
		}
	}
}
