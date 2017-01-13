package RUfoo.logic;

import battlecode.common.RobotController;

public class ScoutLogic extends RobotLogic {

	public ScoutLogic(RobotController _rc) {
		super(_rc);
	}

	@Override
	public void logic() {
		combat.singleShotAttack();
		nav.dodgeBullets();
		nav.swarm();
	}

}
