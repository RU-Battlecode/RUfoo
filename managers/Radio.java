package RUfoo.managers;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Radio {

	private RobotController rc;
	
	public static final int TREE_CHANNEL = 0;
	

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
	
	public MapLocation readTreeChannel() {
		
		try {
			int msg = rc.readBroadcast(TREE_CHANNEL);
			
			return msg == 0 ? null : intToMapLocation(msg);
			
		} catch (GameActionException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	public void requestCutTreeAt(MapLocation loc) {
		try {
			rc.broadcast(TREE_CHANNEL, mapLocationToInt(loc));
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}
	
	public void taskComplete(int channel) {
		try {
			rc.broadcast(channel, 0);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}
	
	int mapLocationToInt(MapLocation loc) {
		return Math.round(loc.x) << 16 | Math.round(loc.y);		 
	}
	
	MapLocation intToMapLocation(int data) {
		return new MapLocation(data >> 16, data & 0xffff);
	}

	
}
