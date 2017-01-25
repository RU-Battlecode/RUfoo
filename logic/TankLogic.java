package RUfoo.logic;

import battlecode.common.BulletInfo;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.TreeInfo;

public class TankLogic extends RobotLogic {

	public TankLogic(RobotController _rc) {
		super(_rc);
	}

	@Override
	public void logic() {
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		BulletInfo[] bullets = rc.senseNearbyBullets();
		
		RobotInfo target = combat.findTarget(enemies);
		if (target != null) {
			nav.moveAggressivelyTo(target.location.add(rc.getLocation().directionTo(target.location)), bullets, enemies);
			combat.shoot(target, enemies);
		} else {
			TreeInfo[] trees = rc.senseNearbyTrees();
			nav.dodge(bullets);
			nav.moveByTrees(trees);
			nav.tryHardMove(rc.getLocation().directionTo(combat.getClosestEnemySpawn()));
		}
	}

}
