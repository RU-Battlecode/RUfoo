package RUfoo.logic;

import battlecode.common.RobotInfo;

public class SoldierLogic extends RobotLogic {

	@Override
	public void logic() {
		RobotInfo target = combatManager.findTarget();
		if (target != null) {
			navManager.moveAggressively(target.location);
			combatManager.singleShotAttack(target);
		} else {
			navManager.dodgeBullets();
		}
	}
	
}
