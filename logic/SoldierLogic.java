package RUfoo.logic;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldierLogic extends RobotLogic {

	private boolean nothingAtEnemySpawn;
	private boolean defend;

	public SoldierLogic(RobotController _rc) {
		super(_rc);
		nothingAtEnemySpawn = false;
		defend = false;
	}

	@Override
	public void logic() {
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());

		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.ARCHON) {
				radio.foundEnemyArchon(enemy);
			}
		}

		RobotInfo target = combat.findTarget(enemies);
		if (target != null) {
			nav.moveAggressively(target.location);
			combat.shoot(target);
		} else {
			nav.dodgeBullets();
		
			if (defend) {
				nav.moveBest(rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam())[0]));
			}
			
			if (nothingAtEnemySpawn || rc.getLocation().distanceTo(combat.getClosestEnemySpawn()) < 2.0f) {
				nothingAtEnemySpawn = true;

				MapLocation[] possibleArchonLocations = radio.readEnemyArchonChannel();
				for (MapLocation archonLoc : possibleArchonLocations) {
					if (archonLoc != null) {
						
						if (rc.getLocation().distanceTo(archonLoc) < 2.0f && enemies.length == 0) {
							nav.moveRandom();
							defend = true;
						} else {
							nav.moveBest(rc.getLocation().directionTo(archonLoc));
						}
					}
				}

				nav.moveRandom();
			} else {
				nav.moveBest(rc.getLocation().directionTo(combat.getClosestEnemySpawn()));
			}
		}		
	}

}
