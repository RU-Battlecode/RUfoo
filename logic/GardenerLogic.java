package RUfoo.logic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import RUfoo.managers.Navigation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

/**
 * GardenerLogic.java - Gardeners will try to find a perfect base location
 * that will let them build gardens that look like:
 * 
 *					T T T
 *					T G T
 *					T B T
 * T: Bullet tree
 * G: Gardener
 * B: buildLocation 
 * 
 * Gardeners will rotate the north tree to always face the closest enemy
 * initial Archon location.
 * 
 * @author Ben
 *
 */
public class GardenerLogic extends RobotLogic {

	private static final int MIN_STEPS_BEFORE_SETTLE = 5;
	private int stepsBeforeGiveUp = 70;

	private static final Direction[] TREE_BUILD_DIRS = { Direction.getNorth(), Direction.getEast(), Direction.getWest(),
			Navigation.NORTH_WEST.rotateLeftDegrees(15), Navigation.NORTH_EAST.rotateRightDegrees(15),
			Navigation.SOUTH_WEST.rotateRightDegrees(15), Navigation.SOUTH_EAST.rotateLeftDegrees(15), };

	private float buildOffset;
	private Direction buildDirection;
	private int steps;
	private boolean settled;
	private MapLocation baseLocation;
	private boolean hasPlantedFront;
	private boolean hasPlantedMiddle;
	private boolean hasFinishedPlanting;
	private int plantFailCount;
	private Map<RobotType, Integer> typeCount;
	
	
	public GardenerLogic(RobotController _rc) {
		super(_rc);
		Direction pointAt = rc.getLocation().directionTo(combat.getClosestEnemySpawn());
		buildOffset = TREE_BUILD_DIRS[0].degreesBetween(pointAt);
		buildDirection = TREE_BUILD_DIRS[0].opposite().rotateLeftDegrees(buildOffset);
		typeCount = new HashMap<>();
		baseLocation = rc.getLocation();
		hasPlantedFront = hasPlantedMiddle = hasFinishedPlanting = false;
		plantFailCount = 0;

		stepsBeforeGiveUp = Math.round(combat.getClosestEnemySpawn().distanceTo(rc.getLocation()) / 1.6f);
	}

	@Override
	public void logic() {

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

	void findBaseLocation() {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
		RobotInfo archon = nearest(RobotType.ARCHON, robots);
		RobotInfo gardener = nearest(RobotType.GARDENER, robots);

		if ((archon == null || archon.location.distanceTo(rc.getLocation()) >= 3.0f)
				&& rc.hasTreeBuildRequirements() && (rc.canPlantTree(buildDirection)
						&& rc.canPlantTree(buildDirection.opposite()) && steps > MIN_STEPS_BEFORE_SETTLE)
				|| (steps >= stepsBeforeGiveUp && (gardener == null || gardener.location.distanceTo(rc.getLocation()) > 1.0f))) {
			settled = true;
			baseLocation = rc.getLocation();
		} else {

			if (archon != null) {
				nav.moveBest(archon.location.directionTo(rc.getLocation()));
			} else if (gardener != null) {
				nav.moveBest(gardener.location.directionTo(rc.getLocation()));
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
			if (rc.getLocation().distanceTo(baseLocation) < 0.2f) {
				hasPlantedMiddle = true;
				plantFailCount = 0;
			}
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
		} else if (typeCount.getOrDefault(RobotType.SOLDIER, 0) < 1) {
			build(RobotType.SOLDIER);
		} else if (typeCount.getOrDefault(RobotType.SCOUT, 0) < 1) {
			build(RobotType.SCOUT);
		} else if (typeCount.getOrDefault(RobotType.LUMBERJACK, 0) < 2) {
			build(RobotType.LUMBERJACK);
		}
	}

	void build(RobotType type) {
		if (rc.isBuildReady() && rc.hasRobotBuildRequirements(type) && rc.canBuildRobot(type, buildDirection)) {
			try {
				rc.buildRobot(type, buildDirection);
				typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
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

	RobotInfo nearest(RobotType type, RobotInfo[] robots) {
		RobotInfo archon = null;
		
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				archon = robot;
				break;
			}
		}

		return archon;
	}

	boolean moveAndPlant(Direction dir) {
		boolean success = false;
		// Move forward to plant.
		final MapLocation firstThreeSteps = baseLocation.add(dir, rc.getType().strideRadius * 3.0f);
		final MapLocation endPoint = firstThreeSteps.add(dir, rc.getType().strideRadius + 0.1f);

		nav.tryMove(endPoint);
		if (!rc.hasMoved() && !rc.canMove(dir)) {
			plantFailCount++;
			return false;
		}

		if (rc.getLocation().distanceTo(endPoint) > 0.3f) {
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
