package RUfoo.managers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import RUfoo.Vector2f;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.TreeInfo;

public class Combat {

	private RobotController rc;

	public Combat(RobotController _rc) {
		rc = _rc;
	}

	public MapLocation getClosestEnemySpawn() {
		MapLocation[] enemySpawns = rc.getInitialArchonLocations(rc.getTeam().opponent());

		Arrays.sort(enemySpawns, (s1, s2) -> {
			return Math.round(s1.distanceSquaredTo(rc.getLocation()) - s2.distanceSquaredTo(rc.getLocation()));
		});

		return enemySpawns[0];
	}

	// TODO: Maybe shoot to the left and the right of target?
	public void singleShotAttack() {
		singleShotAttack(findTarget());
	}

	public void singleShotAttack(RobotInfo target) {
		if (target != null && rc.canFireSingleShot()) {
			try {
				rc.fireSingleShot(rc.getLocation().directionTo(target.location));
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	// TODO: Make sure not hitting own players.
	public RobotInfo findTarget() {
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		TreeInfo[] trees = rc.senseNearbyTrees();

		List<RobotInfo> hittable = Arrays.asList(enemies);
		for (RobotInfo enemy : enemies) {
			Vector2f dirToEnemy = new Vector2f(rc.getLocation(), enemy.getLocation());
			for (TreeInfo tree : trees) {
				Vector2f dirToTree = new Vector2f(rc.getLocation(), tree.getLocation());
				MapLocation projection = dirToTree.projectOn(dirToEnemy);

				if (projection.distanceTo(tree.location) <= tree.getRadius()) {
					// The bullet would just hit a tree if we fire it...
					// Maybe we should just save ;)
					hittable.remove(enemy);
				}
			}
		}

		if (hittable.size() > 0) {
			return Collections.max(hittable, (e1, e2) -> {
				return robotPriority(e1) - robotPriority(e2);
			});
		} else {
			return null;
		}
	}

	public int robotPriority(RobotInfo robot) {
		if (robot.getTeam() == rc.getTeam()) {
			return 0;
		}

		int priority = 0;

		switch (robot.getType()) {
		case ARCHON:
			// TODO: only prioritize when it is alone!
			priority += 90;
			break;
		case GARDENER:
			priority += 90;
			break;
		case LUMBERJACK:
			priority += 80;
			break;
		case SCOUT:
			priority += 70;
			break;
		case SOLDIER:
			priority += 90;
			break;
		case TANK:
			priority += 100;
			break;
		default:
			System.out.println("Missing robot type in switch.");
			break;
		}

		// Prioritize low health!
		double percentHealth = robot.getHealth() / robot.getType().getStartingHealth();
		priority += 100 * (1 - percentHealth); // this will be between 0 and 100

		// The robot hasn't attacked yet!
		if (robot.getAttackCount() < 1) {
			priority += 10;
		}

		if (robot.moveCount > 0 && robot.location.distanceTo(rc.getLocation()) <= 2.0) {
			// This bot has moved already so may not be able to dodge.
			priority += 9;
		}

		return priority;
	}

}
