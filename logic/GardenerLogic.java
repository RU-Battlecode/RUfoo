package RUfoo.logic;

import java.util.Arrays;

import RUfoo.managers.Navigation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

// 0.  Find a good build spot. (Spread)

// 1. Build trees... 
/*
 *   T T          T T       
 *       T  or  T      or  
 *   T T          T T
 *   
 *      T
 *   T     T   
 *   T     T
 */

// 2. build scouts...
//

// 3.
// Build best units we can afford...

public class GardenerLogic extends RobotLogic {

	private static final float DONATE_AFTER = 500; // bullets
	private static final float DONATE_PERCENTAGE = 0.10f;

	private static final Direction[] TREE_BUILD_DIRS = { Direction.getNorth(),
			Navigation.NORTH_WEST.rotateLeftDegrees(15), Navigation.NORTH_EAST.rotateRightDegrees(15),
			Navigation.SOUTH_WEST.rotateRightDegrees(15), Navigation.SOUTH_EAST.rotateLeftDegrees(15), };

	private float buildOffset;
	private Direction buildDirection;
	private int scoutCount;
	private int lumberjackCount;
	private boolean settled;

	public GardenerLogic(RobotController _rc) {
		super(_rc);
		Direction pointAt = rc.getLocation().directionTo(combat.getClosestEnemySpawn());
		buildOffset = TREE_BUILD_DIRS[0].degreesBetween(pointAt);
		buildDirection = Direction.getSouth().rotateLeftDegrees(buildOffset);
		scoutCount = 0;
		lumberjackCount = 0;
	}

	@Override
	public void logic() {
		
		if (settled) {
			donateToWin();
			plantTrees();
			waterTrees();
			buildRobots();
			orderClearTrees();
		} else {
			findBaseLocation();
		}
	}
	
	void findBaseLocation() {
		
		if (rc.canPlantTree(buildDirection)) {
			settled = true;
		} else {
			nav.moveBest(buildDirection.opposite());
		}
		
	}

	void donateToWin() {
		try {
			if (rc.getRoundNum() == rc.getRoundLimit() - 1) {

				// End game. Just donate all.
				rc.donate(rc.getTeamBullets());

			} else {
				// If we can win... win.
				if (rc.getTeamVictoryPoints()
						+ (int) (rc.getTeamBullets() / 10) >= GameConstants.VICTORY_POINTS_TO_WIN) {

					rc.donate(rc.getTeamBullets());

				} else if (rc.getTeamBullets() > DONATE_AFTER) {
					rc.donate(rc.getTeamBullets() * DONATE_PERCENTAGE);
				}
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	void plantTrees() {
		for (Direction dir : TREE_BUILD_DIRS) {
			dir = dir.rotateLeftDegrees(buildOffset);
			if (rc.canPlantTree(dir)) {
				try {
					rc.plantTree(dir);
					break;
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
		}
	}

	void waterTrees() {
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius, rc.getTeam());

		// Lowest health trees first.
		Arrays.sort(trees, (t1, t2) -> {
			return Math.round(t1.health - t2.health);
		});

		for (TreeInfo tree : trees) {
			if (rc.canWater(tree.ID)) {
				try {
					rc.water(tree.ID);
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
		}
	}

	void buildRobots() {
		if (scoutCount < 1) {
			build(RobotType.SCOUT);
		} else if (lumberjackCount < 2) {
			build(RobotType.LUMBERJACK);
		}
	}

	void build(RobotType type) {
		if (rc.isBuildReady() && rc.hasRobotBuildRequirements(type) && rc.canBuildRobot(type, buildDirection)) {
			try {
				rc.buildRobot(type, buildDirection);
				if (type == RobotType.SCOUT) {
					scoutCount++;
				} else if (type == RobotType.LUMBERJACK) {
					lumberjackCount++;
				}
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	void orderClearTrees() {
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);

		for (TreeInfo tree : trees) {
			radio.requestCutTreeAt(tree.location);
			break;
		}
	}

}
