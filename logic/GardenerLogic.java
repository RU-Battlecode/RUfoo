package RUfoo.logic;

import battlecode.common.Direction;
import battlecode.common.GameActionException;

public class GardenerLogic extends RobotLogic {

	@Override
	public void logic() {
		navManager.dodgeBullets();
	}

}
