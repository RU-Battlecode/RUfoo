package RUfoo.managers;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Radio {

	private RobotController rc;

	public static final int TREE_CHANNEL = 0;
	public static final int DEFENSE_CHANNEL = 1;

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

	// Defense channel
	public MapLocation readDefenseChannel() { 
		int msg = readChannel(DEFENSE_CHANNEL);
		return msg == 0 ? null : intToMapLocation(msg);
	}
	
	public void requestDefense(MapLocation loc) {
		broadcast(DEFENSE_CHANNEL, mapLocationToInt(loc));
	}
	
	// Tree channel
	public MapLocation readTreeChannel() {
		int msg = readChannel(TREE_CHANNEL);
		return msg == 0 ? null : intToMapLocation(msg);
	}

	public void requestCutTreeAt(MapLocation loc) {
		broadcast(TREE_CHANNEL, mapLocationToInt(loc));
	}

	
	public void taskComplete(int channel) {
		try {
			rc.broadcast(channel, 0);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	void broadcast(int channel, int msg) {
		try {
			if (rc.readBroadcast(channel) == 0) {
				rc.broadcast(channel, msg);
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}
	
	int readChannel(int channel) {
		int msg = 0;
		try {
			msg = rc.readBroadcast(TREE_CHANNEL);
		} catch(GameActionException e) {}
		
		return msg;
	}
	
	int mapLocationToInt(MapLocation loc) {
		return Math.round(loc.x) << 16 | Math.round(loc.y);
	}

	MapLocation intToMapLocation(int data) {
		return new MapLocation(data >> 16, data & 0xffff);
	}

}
