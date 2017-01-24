package RUfoo.managers;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

import static RUfoo.managers.Channel.*;

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
		broadcastSafely(TREE_CHANNEL, mapLocationToInt(loc));
	}

	public void taskComplete(Channel channel) {
		try {
			rc.broadcast(channel.ordinal(), 0);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public void broadcastSafely(Channel channel, int msg) {
		try {
			if (rc.readBroadcast(channel.ordinal()) == 0) {
				rc.broadcast(channel.ordinal(), msg);
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}
	
	public void broadcast(Channel channel, int msg) {
		try {	
			rc.broadcast(channel.ordinal(), msg);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public int readChannel(Channel channel) {
		int msg = 0;
		try {
			msg = rc.readBroadcast(channel.ordinal());
		} catch (GameActionException e) {
		}

		return msg;
	}

	int mapLocationToInt(MapLocation loc) {
		return Math.round(loc.x) << 16 | Math.round(loc.y);
	}

	MapLocation intToMapLocation(int data) {
		return new MapLocation(data >> 16, data & 0xffff);
	}
}
