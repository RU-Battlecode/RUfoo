package RUfoo.managers;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public class Radio {

	private RobotController rc;

	public Radio(RobotController _rc) {
		rc = _rc;
	}

	public int getFreeChannel() {
		int freeIndex = 0;
		try {
			while (rc.readBroadcast(freeIndex) != 0 && freeIndex < GameConstants.BROADCAST_MAX_CHANNELS) {
				freeIndex += 1;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return freeIndex;
	}
}
