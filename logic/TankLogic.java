package RUfoo.logic;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class TankLogic extends RobotLogic {

	public TankLogic(RobotController _rc) {
		super(_rc);
	}

	@Override
	public void logic() {
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		RobotInfo target = combat.findTarget(enemies);
		if (target != null) {
			nav.moveAggressively(target.location.add(rc.getLocation().directionTo(target.location)));
			combat.singleShotAttack(target);
		} else {
			nav.dodgeBullets();
			nav.moveByTrees(false);
			nav.moveBest(rc.getLocation().directionTo(combat.getClosestEnemySpawn()));
		}
	}

}
