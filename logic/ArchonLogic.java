package RUfoo.logic;

import java.util.ArrayList;
import java.util.List;

import RUfoo.Util;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class ArchonLogic extends RobotLogic {

	private static final Direction[] GARDENER_BUILD_DIRECTIONS = { 
			Direction.getNorth(),
			Direction.getEast().rotateRightDegrees(45),
			Direction.getWest().rotateLeftDegrees(45) };

	public ArchonLogic() {
		super();
		
	}
	
	@Override
	public void logic() {
		RobotInfo[] robots = rc.senseNearbyRobots();
		List<RobotInfo> gardeners = new ArrayList<>();

		for (RobotInfo info : robots) {
			if (info.getTeam() == rc.getTeam()) {
				if (info.getType() == RobotType.GARDENER) {
					gardeners.add(info);
				}
			} else {
				// Sensed other team...
			}
		}

		// Do we need to build more gardeners?
		if (gardeners.size() < GARDENER_BUILD_DIRECTIONS.length && rc.isBuildReady()) {
			buildGardener();
		}

		moveRelativeTo(gardeners);
	}
	
	void buildGardener() {
		Direction dir = Util.randomChoice(Navigation.DIRECTIONS);
		if (rc.canBuildRobot(RobotType.GARDENER, dir)) {
			try {
				rc.buildRobot(RobotType.GARDENER, dir);
				gardenerCount++;
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}
	
	void moveRelativeTo(List<RobotInfo> gardeners) {
		if (gardeners.size() <= 1) {
			navManager.dodgeBullets();
			navManager.moveRandom();
			return;
		}
		
		// Move to the center of all gardeners
		List<MapLocation> locations = new ArrayList<>();
		for (RobotInfo gardener : gardeners) {
			locations.add(gardener.location);
		}
		navManager.moveToSafely(Util.average(locations));
	}
}
