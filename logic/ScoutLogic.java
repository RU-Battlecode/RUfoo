package RUfoo.logic;

import RUfoo.managers.Combat;

public class ScoutLogic extends RobotLogic {

	private Combat combatManager;
	
	@Override
	public void logic() {
		combatManager.singleShotAttack();
		//navManager.dodgeBullets();
	}

}
