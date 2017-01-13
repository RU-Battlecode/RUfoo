package RUfoo.logic;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class SoldierLogic extends RobotLogic {

	public SoldierLogic(RobotController _rc) {
		super(_rc);
	}

	@Override
	public void logic() {
		RobotInfo target = combat.findTarget();
		if (target != null) {
			nav.moveAggressively(target.location);
			combat.singleShotAttack(target);
		} else {
			nav.swarm();
		}
	}

}
