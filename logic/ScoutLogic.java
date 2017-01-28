package RUfoo.logic;

import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
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

	private Direction exploreDir;
	private MapLocation home;

	public ScoutLogic(RobotController _rc) {
		super(_rc);
		exploreDir = null;		
	}

	@Override
	public void logic() {	
		if (home == null) {
			home = rc.getLocation();
		}
		
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		BulletInfo[] bullets = rc.senseNearbyBullets();
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
		RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
		TreeInfo[] myTrees = rc.senseNearbyTrees(rc.getType().sensorRadius, rc.getTeam());

		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.ARCHON) {
				radio.foundEnemyArchon(enemy);
			} else if (enemy.type == RobotType.GARDENER) {
				radio.foundEnemyGardener(enemy);
			}
		}
		
		RobotInfo target = combat.findTarget(enemies, friends, myTrees, trees);
		if (target != null) {
			nav.moveSafelyTo(target.location, bullets, enemies);
			combat.shoot(target, enemies);
		}

		if (target == null && !rc.hasAttacked()) {
			for (TreeInfo tree : trees) {
				if (tree.containedBullets > 0) {
					nav.moveAggressivelyTo(tree.location, bullets, enemies);
					break;
				}
			}
			explore();
		}

		nav.shakeTrees(trees);
	}

	void explore() {
		if (exploreDir == null || isHome()) {
			float randomDegrees = (personality.getIsLeftHanded() ? 1 : -1) * personality.random(0.0f, 360.0f);
			exploreDir = rc.getLocation().directionTo(combat.getFurthestEnemySpawn()).rotateLeftDegrees(randomDegrees); 
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
			return rc.onTheMap(rc.getLocation().add(exploreDir, rc.getType().sensorRadius / 2));
		} catch (GameActionException e) {
			e.printStackTrace();
			return true;
		}
	}

	boolean isHome() {
		return rc.getLocation().distanceTo(home) <= rc.getType().bodyRadius * 3;
	}
}
