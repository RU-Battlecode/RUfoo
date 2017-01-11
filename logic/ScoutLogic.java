package RUfoo.logic;

import battlecode.common.RobotController;

public class ScoutLogic extends RobotLogic {

	public ScoutLogic(RobotController _rc) {
		super(_rc);
	}

	@Override
	public void logic() {
		combatManager.singleShotAttack();
		navManager.dodgeBullets();
	}

}
