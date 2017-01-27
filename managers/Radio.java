package RUfoo.managers;

import static RUfoo.managers.Channel.DEFENSE_CHANNEL;
import static RUfoo.managers.Channel.ENEMY_ARCHON_CHANNEL1;
import static RUfoo.managers.Channel.ENEMY_ARCHON_CHANNEL2;
import static RUfoo.managers.Channel.ENEMY_ARCHON_CHANNEL3;
import static RUfoo.managers.Channel.ENEMY_ARCHON_ID_CHANNEL1;
import static RUfoo.managers.Channel.ENEMY_ARCHON_ID_CHANNEL2;
import static RUfoo.managers.Channel.ENEMY_ARCHON_ID_CHANNEL3;
import static RUfoo.managers.Channel.TREE_CHANNEL;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

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
	
	// ENEMY_ARCHON_CHANNEL
	public MapLocation[] readEnemyArchonChannel() {
		int msg1 = readChannel(ENEMY_ARCHON_CHANNEL1);
		int msg2 = readChannel(ENEMY_ARCHON_CHANNEL2);
		int msg3 = readChannel(ENEMY_ARCHON_CHANNEL3);
		return new MapLocation[] {
				msg1 == 0 ? null : intToMapLocation(msg1),
				msg2 == 0 ? null : intToMapLocation(msg2),
				msg3 == 0 ? null : intToMapLocation(msg3),
		};
	}
	
	public void foundEnemyArchon(RobotInfo robot) {
		
		Channel[] archonIdChannels = new Channel[] {
				ENEMY_ARCHON_ID_CHANNEL1,
				ENEMY_ARCHON_ID_CHANNEL2,
				ENEMY_ARCHON_ID_CHANNEL3
		};
		
		int[] archonIds = new int[] {
				readChannel(ENEMY_ARCHON_ID_CHANNEL1),
				readChannel(ENEMY_ARCHON_ID_CHANNEL2),
				readChannel(ENEMY_ARCHON_ID_CHANNEL3),
		};
	
		Channel[] archonLocationChannels = new Channel[] {
				ENEMY_ARCHON_CHANNEL1,
				ENEMY_ARCHON_CHANNEL2,
				ENEMY_ARCHON_CHANNEL3
		};
		
		// If this robot is unseen then mark it as 1st/2nd/3rd enemy archon
		for (int i = 0; i < archonIds.length; i++) {
			if (archonIds[i] == 0) {
				broadcast(archonIdChannels[i], robot.ID);
				break;
			}
		}
		
		// Update the location of this enemy archon!
		for (int i = 0; i < archonIds.length; i++) {
			if (archonIds[i] == robot.ID) {
				broadcast(archonLocationChannels[i], mapLocationToInt(robot.location));
				break;
			}
		}
		
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
			e.printStackTrace();
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
