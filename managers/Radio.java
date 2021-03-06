package RUfoo.managers;

import static RUfoo.model.Channel.DEFENSE_NEEDED_COUNT_START;
import static RUfoo.model.Channel.DEFENSE_NEEDED_LOCATION_END;
import static RUfoo.model.Channel.DEFENSE_NEEDED_LOCATION_START;
import static RUfoo.model.Channel.ENEMY_ARCHON_CHANNEL1;
import static RUfoo.model.Channel.ENEMY_ARCHON_CHANNEL2;
import static RUfoo.model.Channel.ENEMY_ARCHON_CHANNEL3;
import static RUfoo.model.Channel.ENEMY_ARCHON_ID_CHANNEL1;
import static RUfoo.model.Channel.ENEMY_ARCHON_ID_CHANNEL2;
import static RUfoo.model.Channel.ENEMY_ARCHON_ID_CHANNEL3;
import static RUfoo.model.Channel.ENEMY_GARDENER_ID_END;
import static RUfoo.model.Channel.ENEMY_GARDENER_ID_START;
import static RUfoo.model.Channel.ENEMY_GARDENER_LOC_END;
import static RUfoo.model.Channel.ENEMY_GARDENER_LOC_START;
import static RUfoo.model.Channel.GARDENER_BASE_LOCATION_END;
import static RUfoo.model.Channel.GARDENER_BASE_LOCATION_START;
import static RUfoo.model.Channel.SCOUT_TARGET_LOCATION_END;
import static RUfoo.model.Channel.SCOUT_TARGET_LOCATION_START;
import static RUfoo.model.Channel.TREE_CHANNEL;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import RUfoo.model.Channel;
import RUfoo.model.DefenseInfo;
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
	
	public MapLocation readSoldierAttackLocation() {
		int msg = readChannel(Channel.SOLDIER_ATTACK_LOCATION);
		return msg != 0 ? intToMapLocation(msg) : null;
	}
	
	public void postToSoldierAttackLocation(MapLocation loc) {
		broadcast(Channel.SOLDIER_ATTACK_LOCATION, mapLocationToInt(loc));
	}
	
	// GARDENER BASES
	public void sendSettledBase(MapLocation location) {
		for (int i = GARDENER_BASE_LOCATION_START.ordinal(); i <= GARDENER_BASE_LOCATION_END.ordinal(); i++) {
			if (readChannel(i) == 0) {
				broadcast(Channel.values()[i], mapLocationToInt(location));
				break;
			}
		}
	}

	public List<MapLocation> readGardenerBaseLocations() {
		List<MapLocation> locations = new LinkedList<>();
		for (int i = GARDENER_BASE_LOCATION_START.ordinal(); i <= GARDENER_BASE_LOCATION_END.ordinal(); i++) {
			int location = readChannel(i);
			if (location != 0) {
				locations.add(intToMapLocation(location));
			}
		}
		
		return locations;
	}
	
	// SCOUT TARGETS
	public List<MapLocation> readScoutTargetsNearMe() {
		List<MapLocation> locs = new ArrayList<>();
		for (int i = SCOUT_TARGET_LOCATION_START.ordinal(); i <= SCOUT_TARGET_LOCATION_END.ordinal(); i++) {
			int target = readChannel(Channel.values()[i]);
			if (target != -1 && target != 0) {
				MapLocation location = intToMapLocation(target);
				if (rc.getLocation().distanceTo(location) <= rc.getType().sensorRadius + rc.getType().bodyRadius) {
					locs.add(location);
				}
			}
		}
		
		return locs;
	}
	
	
	// DEFENSE 
	public void requestDefense(MapLocation loc, int numUnits) {
		for (int i = 0; i < DEFENSE_NEEDED_LOCATION_END.ordinal() - DEFENSE_NEEDED_LOCATION_START.ordinal() + 1; i++) {
			Channel defenseNeededLocation = Channel.values()[DEFENSE_NEEDED_LOCATION_START.ordinal() + i];
			Channel numUnitsChannel = Channel.values()[DEFENSE_NEEDED_COUNT_START.ordinal() + i];
			int msg = readChannel(defenseNeededLocation);
			
			if (msg == 0) {
				broadcast(defenseNeededLocation, mapLocationToInt(loc));
				broadcast(numUnitsChannel, numUnits);
				break;
			}
		}
	}
	
	public List<DefenseInfo> readLocationsThatNeedDefense() {
		List<DefenseInfo> locs = new ArrayList<>();
		
		// Read each location channel
		for (int i = 0; i < DEFENSE_NEEDED_LOCATION_END.ordinal() - DEFENSE_NEEDED_LOCATION_START.ordinal() + 1; i++) {
			Channel defenseNeededLocation = Channel.values()[DEFENSE_NEEDED_LOCATION_START.ordinal() + i];
			Channel numUnitsChannel = Channel.values()[DEFENSE_NEEDED_COUNT_START.ordinal() + i];
			
			// Check if there is a maplocation
			int mapLocation = readChannel(defenseNeededLocation);
			if (mapLocation != 0) {
				// See if they need more defense
				int numUnitsNeeded = readChannel(numUnitsChannel);
				if (numUnitsNeeded > 0) {
					// Defense is needed!!
					DefenseInfo defInfo = new DefenseInfo(intToMapLocation(mapLocation), numUnitsNeeded, numUnitsChannel);
					locs.add(defInfo);
				}	
			}
		}
		
		return locs;
	}
	
	public void respondToDefenseCall(DefenseInfo defInfo, int defenseValue) {
		broadcast(defInfo.getUnitsChannel(), defInfo.getUnitsNeeded() - defenseValue);
	}
	
	
	// ENEMY_GARDENER
	
	public void foundEnemyGardener(RobotInfo robot) {
		Channel freeIdChannel = null;
		Channel freeLocChannel = null;
		boolean isAlreadyFound = false;
		for (int i = 0; i < ENEMY_GARDENER_ID_END.ordinal() - ENEMY_GARDENER_ID_START.ordinal() + 1; i++) {
			Channel enemyIDChannel = Channel.values()[ENEMY_GARDENER_ID_START.ordinal() + i];
			Channel enemyLocChannel = Channel.values()[ENEMY_GARDENER_ID_END.ordinal() + i];
			int msg = readChannel(enemyIDChannel);
			
			// find free channel
			if (msg == 0) {
				freeIdChannel = enemyIDChannel;
				freeLocChannel = enemyLocChannel;
			} else if (msg == robot.ID) {
				isAlreadyFound = true;
				broadcast(enemyLocChannel, mapLocationToInt(robot.location));
				break;
			}
		}

		if (!isAlreadyFound && freeIdChannel != null && freeLocChannel != null) {
			broadcast(freeIdChannel, robot.ID);
			broadcast(freeLocChannel, mapLocationToInt(robot.location));
		}
	}
	
	public List<MapLocation> readEnemyGardenerLocations() {
		List<MapLocation> locs = new ArrayList<>();
		
		for (int i = ENEMY_GARDENER_LOC_START.ordinal(); i <= ENEMY_GARDENER_LOC_END.ordinal(); i++) {
			int msg = readChannel(Channel.values()[i]);
			if (msg != 0) {
				locs.add(intToMapLocation(msg));
			}
		}
		
		return locs;
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

	private int readChannel(int channel) {
		return readChannel(Channel.values()[channel]);
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

	public int mapLocationToInt(MapLocation loc) {
		return Math.round(loc.x) << 16 | Math.round(loc.y);
	}

	public MapLocation intToMapLocation(int data) {
		return new MapLocation(data >> 16, data & 0xffff);
	}
}
