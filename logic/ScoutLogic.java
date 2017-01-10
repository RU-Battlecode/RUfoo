package RUfoo.logic;

public class ScoutLogic extends RobotLogic {

	@Override
	public void logic() {
		combatManager.singleShotAttack();
		navManager.dodgeBullets();
	}

}
