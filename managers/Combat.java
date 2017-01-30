package RUfoo.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import RUfoo.util.Util;
import battlecode.common.BodyInfo;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;

public class Combat {

	private RobotController rc;

	public Combat(RobotController _rc) {
		rc = _rc;
	}

	public MapLocation getClosestEnemy() {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		MapLocation closest = getClosestEnemySpawn();
		
		for (RobotInfo robot : robots) {
			if (robot.location.distanceSquaredTo(rc.getLocation()) < 
					closest.distanceSquaredTo(rc.getLocation())) {
				closest = robot.location;
			}
		}
		
		return closest;
	}
	
	public MapLocation getClosestEnemySpawn() {
		MapLocation[] enemySpawns = rc.getInitialArchonLocations(rc.getTeam().opponent());

		Arrays.sort(enemySpawns, (s1, s2) -> {
			return Math.round(s1.distanceSquaredTo(rc.getLocation()) - s2.distanceSquaredTo(rc.getLocation()));
		});

		return enemySpawns[0];
	}

	public MapLocation getFurthestEnemySpawn() {
		MapLocation[] enemySpawns = rc.getInitialArchonLocations(rc.getTeam().opponent());

		Arrays.sort(enemySpawns, (s1, s2) -> {
			return Math.round(s2.distanceSquaredTo(rc.getLocation()) - s1.distanceSquaredTo(rc.getLocation()));
		});

		return enemySpawns[0];
	}

	public void shoot(RobotInfo target, RobotInfo[] enemies) {	
		shoot(target.location, enemies);
	}
	
	public void shoot(MapLocation target, RobotInfo[] enemies) {
		if (target == null) {
			return;
		}
		
		float distToTarget = target.distanceTo(rc.getLocation());
		if (shouldUsePentadShot() && (distToTarget <= rc.getType().sensorRadius / 1.5)) {
			pentadShot(target);
		} else if (shouldUseTriadShot()) {
			triadShot(target);
		} else {
			singleShotAttack(target);
		}
	}

	public void singleShotAttack(MapLocation target) {
		if (target != null && rc.canFireSingleShot() && !rc.hasAttacked()) {
			try {
				rc.fireSingleShot(rc.getLocation().directionTo(target));
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	public void pentadShot(MapLocation target) {
		if (target != null && rc.canFirePentadShot() && !rc.hasAttacked()) {
			try {
				rc.firePentadShot(rc.getLocation().directionTo(target));
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	public void triadShot(MapLocation target) {
		if (target != null && rc.canFireTriadShot() && !rc.hasAttacked()) {
			try {
				rc.fireTriadShot(rc.getLocation().directionTo(target));
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean shouldUsePentadShot() {
		return rc.canFirePentadShot() && GameConstants.VICTORY_POINTS_TO_WIN
				- rc.getTeamVictoryPoints() > GameConstants.VICTORY_POINTS_TO_WIN * 0.1f;
	}

	public boolean shouldUseTriadShot() {
		return rc.canFireTriadShot() && GameConstants.VICTORY_POINTS_TO_WIN
				- rc.getTeamVictoryPoints() > GameConstants.VICTORY_POINTS_TO_WIN * 0.1f;
	}

	public RobotInfo findTarget(RobotInfo[] enemies, RobotInfo[] friends, TreeInfo[] myTrees, TreeInfo[] neutralTrees) {
		
		// Avoid hitting friends and friendly trees because bullets buy happiness.
		BodyInfo[] bodiesToAvoid = Util.addAll(Util.addAll(myTrees, friends), neutralTrees);
		
		List<RobotInfo> hittable = new ArrayList<>(Arrays.asList(enemies));
		for (RobotInfo enemy : enemies) {			
			for (BodyInfo body : bodiesToAvoid) {
				MapLocation closestPoint = Util.distanceToSegment(rc.getLocation(), enemy.location, body.getLocation());
				Float dist = closestPoint.distanceTo(body.getLocation());
				if (dist < body.getRadius() - 0.07f) {
					// The bullet would just hit a tree or a friend if we fire it...
					// Maybe we should just save ;)
					hittable.remove(enemy);
					break;
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
			if (rc.getRoundNum() < 100 || rc.getRoundNum() > 300 || rc.getType() == RobotType.LUMBERJACK) {
				priority += 80;
			} else {
				priority += 50;
			}
			break;
		case GARDENER:
			priority += 100;
			break;
		case LUMBERJACK:
			priority += 90;
			if (rc.getLocation().distanceTo(robot.location) <= GameConstants.LUMBERJACK_STRIKE_RADIUS + 1.0f) {
				priority += 10;
			}
			break;
		case SCOUT:
			priority += 80;
			break;
		case SOLDIER:
			priority += 100;
			break;
		case TANK:
			priority += 110;
			break;
		default:
			System.out.println("Missing robot type in switch.");
			break;
		}

		// Prioritize low health!
		double percentHealth = robot.getHealth() / robot.getType().maxHealth;
		priority += 100 * (1 - percentHealth); // this will be between 0 and 100

		// The robot hasn't attacked yet!
		if (robot.getAttackCount() < 1 && robot.getType().canAttack()) {
			priority += 10;
		}

		if (robot.moveCount > 0 && robot.location.distanceTo(rc.getLocation()) <= 2.0) {
			// This bot has moved already so may not be able to dodge.
			priority += 9;
		}
		
		return priority;
	}

	// Melee

	public int meleePriority(RobotInfo robot) {
		int priority = robotPriority(robot);

		// Close enemies = better [0,100]
		priority += (rc.getType().sensorRadius - robot.getLocation().distanceTo(rc.getLocation())) * 200;

		return priority;
	}

	public RobotInfo findMeleeTarget(RobotInfo[] enemies) {
		// Sort by attack priority.
		Arrays.sort(enemies, (e1, e2) -> {
			return meleePriority(e1) - meleePriority(e2);
		});

		return enemies.length > 0 ? enemies[0] : null;
	}

	public void meleeAttack(RobotInfo[] robots) {

		if (!rc.canStrike()) {
			return;
		}

		boolean closeEnough = false;
		for (RobotInfo robot : robots) {
			if (rc.getLocation().distanceTo(robot.location) <= GameConstants.LUMBERJACK_STRIKE_RADIUS * 2.0f) {
				closeEnough = true;
				break;
			}
		}

		if (closeEnough) {
			try {
				rc.strike();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}

	}

}
