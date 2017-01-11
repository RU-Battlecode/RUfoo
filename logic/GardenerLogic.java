package RUfoo.logic;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;

public class GardenerLogic extends RobotLogic {

	private static final Direction[] SCOUT_BUILD_DIRECTIONS = { 
			Direction.getNorth(),
			Direction.getEast().rotateRightDegrees(45),
			Direction.getWest().rotateLeftDegrees(45) };

	private int scoutsMade;
	
	public GardenerLogic(RobotController _rc) {
		super(_rc);
		scoutsMade = 0;
		failedBuildCount = 0;
	}

	@Override
	public void logic() {
		navManager.dodgeBullets();
		if (scoutsMade < 2) {
			build(RobotType.SCOUT);
		} else {
			build(RobotType.SOLDIER);
		}
	}
	
	void build(RobotType type) {
		Direction dir = SCOUT_BUILD_DIRECTIONS[scoutsMade % SCOUT_BUILD_DIRECTIONS.length];
		if (rc.canBuildRobot(type, dir)) {
			try {
				rc.buildRobot(type, dir);
				if (type == RobotType.SCOUT) {
					scoutsMade++;
				}
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}
	

}
