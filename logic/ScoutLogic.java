package RUfoo.logic;

import RUfoo.model.Channel;
import RUfoo.util.Util;
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
	private Channel shareTargetChannel;
	private int failChannelCount;
	
	public ScoutLogic(RobotController _rc) {
		super(_rc);
		exploreDir = null;
		shareTargetChannel = null;
		failChannelCount = 0;
	}

	@Override
	public void logic() {	
		
		if (shareTargetChannel == null && failChannelCount < 4) {
			findChannel();
			failChannelCount++;
		}
		
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
			
			broadcastTargetToFriends(target.location, friends);
			
			nav.moveSafelyTo(target.location, bullets, enemies);
			combat.shoot(target, enemies);
		} else {
			if (shareTargetChannel != null) {
				radio.broadcast(shareTargetChannel, -1);
			}
		}
		
		if (target == null && !rc.hasAttacked()) {
			for (TreeInfo tree : trees) {
				if (tree.containedBullets > 0) {
					nav.moveAggressivelyTo(tree.location, bullets, enemies);
					break;
				}
			}
			
			RobotInfo friendlyTank = Util.findType(friends, RobotType.TANK);
			if (friendlyTank == null) {
				explore();
			} else {
				nav.bug(friendlyTank.location, friends);
				//nav.tryHardMove(rc.getLocation().directionTo(friendlyTank.location));
			}
		}

		nav.shakeTrees(trees);
	}

	private void broadcastTargetToFriends(MapLocation target, RobotInfo[] friends) {
		if (shareTargetChannel != null) {
			if (friends.length > 0) {
				radio.broadcast(shareTargetChannel, radio.mapLocationToInt(target));
			}
		}
	}

	void explore() {
		if (exploreDir == null || isHome()) {
			if (census.count(RobotType.SCOUT) == 1) {
				exploreDir = rc.getLocation().directionTo(combat.getClosestEnemySpawn());
			} else {
				float randomDegrees = (personality.getIsLeftHanded() ? 1 : -1) * personality.random(0.0f, 360.0f);
				exploreDir = rc.getLocation().directionTo(combat.getFurthestEnemySpawn()).rotateLeftDegrees(randomDegrees); 
			}
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
		return rc.getLocation().distanceTo(home) <= rc.getType().sensorRadius / 3;
	}
	
	void findChannel() {
		for (int i = Channel.SCOUT_TARGET_LOCATION_START.ordinal(); i <= Channel.SCOUT_TARGET_LOCATION_END.ordinal(); i++) {
			if (radio.readChannel(Channel.values()[i]) == 0) {
				shareTargetChannel = Channel.values()[i];
				break;
			}
		}
	}
}
