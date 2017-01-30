package RUfoo.model;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;

/**
 * DefenseInfo.java - 
 * 
 * @author Ben
 * @version Jan 28, 2017
 */
public class DefenseInfo implements Comparable<DefenseInfo> {

	private MapLocation location;
	private int unitsNeeded;
	private Channel unitsChannel;
	
	public DefenseInfo(MapLocation location, int unitsNeeded, Channel unitsChannel) {
		this.location = location;
		this.unitsNeeded = unitsNeeded;
		this.unitsChannel = unitsChannel;
	}

	public MapLocation getLocation() {
		return location;
	}

	public int getUnitsNeeded() {
		return unitsNeeded;
	}

	public Channel getUnitsChannel() {
		return unitsChannel;
	}

	public void setUnitsChannel(Channel unitsChannel) {
		this.unitsChannel = unitsChannel;
	}
	
	public static int unitValue(RobotType robot) {
		int defValue = 0;
		switch (robot) {
		case ARCHON:
		case GARDENER:
			break;
		case SCOUT:
		case LUMBERJACK:
			defValue = 1;
			break;
		case SOLDIER:
			defValue = 2;
			break;
		case TANK:
			defValue = 4;
			break;		
		}
		
		return defValue;
	}


	@Override
	public int compareTo(DefenseInfo other) {
		return unitsNeeded - other.unitsNeeded;
	}

}
