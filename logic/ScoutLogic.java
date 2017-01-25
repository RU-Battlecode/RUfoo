package RUfoo.logic;

import RUfoo.util.Util;
import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

/*
 * 
 * // 1. Shoot any enemies.
 * 
 * // 2. Explore in single direction 
 * 
 * // 3. Return to base when:
 * 			- reached end of map
 *          - found group of enemies 
 *          - health is too low to be out
 *          
 * // 4. count trees! and Radio to the Archon for strategy
 */

public class ScoutLogic extends RobotLogic {

	private static final float LOW_HEALTH_PERCENT = 0.15f;

	private Direction exploreDir;

	public ScoutLogic(RobotController _rc) {
		super(_rc);
		exploreDir = null;		
	}

	@Override
	public void logic() {	
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		BulletInfo[] bullets = rc.senseNearbyBullets();
		
		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.ARCHON) {
				radio.foundEnemyArchon(enemy);
			}
		}
		
		RobotInfo target = combat.findTarget(enemies);
		if (target != null) {
			nav.moveSafelyTo(target.location, bullets, enemies);
			combat.shoot(target, enemies);
		}

		if (target == null && !rc.hasAttacked()) {
			TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
			for (TreeInfo tree : trees) {
				if (tree.containedBullets > 0) {
					nav.moveAggressivelyTo(tree.location, bullets, enemies);
					break;
				}
			}
			explore();
		}
	}

	void explore() {
		if (exploreDir == null || isHome()) {
			exploreDir = nav.randomDirection();
		}

		boolean shouldExplore = shouldExplore();
		if (!shouldExplore) {
			exploreDir = rc.getLocation().directionTo(personality.getHome());
		}
		
		if (exploreDir != null) {
			nav.tryHardMove(exploreDir);
		}
	}

	boolean shouldExplore() {
		try {
			return rc.onTheMap(rc.getLocation().add(exploreDir, rc.getType().sensorRadius / 2))
					&& rc.getHealth() / rc.getType().maxHealth > LOW_HEALTH_PERCENT;
		} catch (GameActionException e) {
			e.printStackTrace();
			return true;
		}
	}

	boolean isHome() {
		return rc.getLocation().distanceTo(personality.getHome()) <= rc.getType().bodyRadius * 3;
	}
}
