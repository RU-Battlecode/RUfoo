package RUfoo.logic;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class SoldierLogic extends RobotLogic {

	public SoldierLogic(RobotController _rc) {
		super(_rc);
	}

	@Override
	public void logic() {
		RobotInfo target = combatManager.findTarget();
		if (target != null) {
			navManager.moveAggressively(target.location);
			combatManager.singleShotAttack(target);
		} else {
			navManager.swarm();
		}
	}
	
}
