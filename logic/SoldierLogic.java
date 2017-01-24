package RUfoo.logic;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class SoldierLogic extends RobotLogic {

	public SoldierLogic(RobotController _rc) {
		super(_rc);
	}

	@Override
	public void logic() {
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		if (target != null) {
			nav.moveAggressively(target.location);
			combat.shoot(target);
		} else {
			nav.dodgeBullets();
			nav.moveBest(rc.getLocation().directionTo(combat.getClosestEnemySpawn()));
		}
	}

}
