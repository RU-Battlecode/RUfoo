package RUfoo.logic;

import java.util.Arrays;

import RUfoo.managers.Navigation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

// 0.  Find a good build spot. (Spread)

// 1. Build trees... 
/*
 *   T T T      T T T       
 *       T  or  T      or  
 *   T T T      T T T
 *   
 *   T  T  T
 *   T     T   
 *   T     T
 */

// 2. build scout...
//

// 3.

public class GardenerLogic extends RobotLogic {


	private static final float DONATE_AFTER = 500; // bullets
	private static final float DONATE_PERCENTAGE = 0.10f;
	private static final int STEPS_BEFORE_SETTLE = 5;
	private int stepsBeforeGiveUp = 70;

	private static final Direction[] TREE_BUILD_DIRS = { Direction.getNorth(), Direction.getEast(), Direction.getWest(),

			Navigation.NORTH_WEST.rotateLeftDegrees(15), Navigation.NORTH_EAST.rotateRightDegrees(15),
			Navigation.SOUTH_WEST.rotateRightDegrees(15), Navigation.SOUTH_EAST.rotateLeftDegrees(15), };

	private float buildOffset;
	private Direction buildDirection;
	private int scoutCount;
	private int lumberjackCount;
	private int steps;
	private boolean settled;
	private MapLocation baseLocation;
	private boolean hasPlantedFront;
	private boolean hasPlantedMiddle;
	private boolean hasFinishedPlanting;
	private int plantFailCount;

	public GardenerLogic(RobotController _rc) {
		super(_rc);
		Direction pointAt = rc.getLocation().directionTo(combat.getClosestEnemySpawn());
		buildOffset = TREE_BUILD_DIRS[0].degreesBetween(pointAt);
		buildDirection = TREE_BUILD_DIRS[0].opposite().rotateLeftDegrees(buildOffset);
		scoutCount = lumberjackCount = 0;
		baseLocation = rc.getLocation();
		hasPlantedFront = hasPlantedMiddle = hasFinishedPlanting = false;
		plantFailCount = 0;

		stepsBeforeGiveUp = Math.round(combat.getClosestEnemySpawn().distanceTo(rc.getLocation()) / 1.6f);
	}

	@Override
	public void logic() {
		donateToWin();
		
		if (settled) {
			TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
			plantTrees();
			waterTrees();
			buildRobots(trees);
			orderClearTrees(trees);
		} else {
			findBaseLocation();
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
	
	void findBaseLocation() {
		RobotInfo archon = nearestArchon();

		if ((archon == null || archon.location.distanceTo(rc.getLocation()) >= 3.0f)
				&& rc.hasTreeBuildRequirements() && (rc.canPlantTree(buildDirection)
						&& rc.canPlantTree(buildDirection.opposite()) && steps > STEPS_BEFORE_SETTLE)
				|| steps >= stepsBeforeGiveUp) {
			settled = true;
			baseLocation = rc.getLocation();
		} else {

			if (archon != null) {
				nav.moveBest(archon.location.directionTo(rc.getLocation()));
			}

			nav.moveBest(buildDirection.opposite());
			steps++;
		}

	}

	void plantTrees() {	
		// We have to plant the front two first to maximize plants
		if (!hasPlantedFront) {
			hasPlantedFront = moveAndPlant(buildDirection.opposite());
		} else if (!hasPlantedMiddle) {
			nav.tryMove(baseLocation);
			hasPlantedMiddle = true;
		} else if (!hasFinishedPlanting) {
			hasFinishedPlanting = moveAndPlant(buildDirection);
		} else {
			nav.tryMove(baseLocation);

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

	void buildRobots(TreeInfo[] trees) {
		if (trees.length >= 25) {
			build(RobotType.LUMBERJACK);
		} else if (scoutCount < 1) {
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

	void orderClearTrees(TreeInfo[] trees) {
		for (TreeInfo tree : trees) {
			radio.requestCutTreeAt(tree.location);
			break;
		}
	}

	RobotInfo nearestArchon() {
		RobotInfo archon = null;

		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());

		for (RobotInfo robot : robots) {
			if (robot.getType() == RobotType.ARCHON) {
				archon = robot;
				break;
			}
		}

		return archon;
	}

	boolean moveAndPlant(Direction dir) {
		boolean success = false;
		// Move forward to plant.
		final MapLocation firstStep = baseLocation.add(dir, rc.getType().strideRadius);
		final MapLocation endPoint = firstStep.add(dir, rc.getType().strideRadius + 0.01f);

		nav.tryMove(endPoint);
		if (!rc.hasMoved() && !rc.canMove(dir)) {
			plantFailCount++;
		}

		if (rc.getLocation().distanceTo(endPoint) > 0.5f) {
			plantFailCount++;
			success = false;
		} else {
			// Plant the left and right plants!
			try {
				// Try plant left.
				tryPlant(Direction.getWest().rotateLeftDegrees(buildOffset));

				// Try plant right.
				if (tryPlant(Direction.getEast().rotateLeftDegrees(buildOffset))) {
					success = true;
				}

			} catch (GameActionException e) {
				e.printStackTrace();
				plantFailCount++;
			}
		}

		if (plantFailCount > personality.getPatience()) {
			plantFailCount = 0;
			success = true;
		}

		return success;
	}

	boolean tryPlant(Direction dir) throws GameActionException {
		if (rc.hasTreeBuildRequirements() && rc.isBuildReady() && rc.canPlantTree(dir)) {
			rc.plantTree(dir);
			return true;
		}

		if (rc.isBuildReady() && rc.hasTreeBuildRequirements() && !rc.canPlantTree(dir)) {
			this.plantFailCount++;
		}
		
		return false;
	}
}
