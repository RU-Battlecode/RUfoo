package RUfoo.logic;

import java.util.Arrays;

import RUfoo.managers.Nav;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

/**
 * GardenerLogic.java - Gardeners will try to find a perfect base location that
 * will let them build gardens that look like:
 * 
 * 		T T T
 *  	T G T
 *   	T B T 
 *   
 * T: Bullet tree 
 * G: Gardener
 * B: buildLocation
 * 
 * Gardeners will rotate the north tree to always face the closest enemy initial
 * Archon location.
 * 
 * @author Ben
 *
 */
public class GardenerLogic extends RobotLogic {

	private static final float TOO_MUCH_TREE_SUM_RADIUS = 10.1f;
	private static final int MIN_STEPS_BEFORE_SETTLE = 5;
	private static final int RESETTLE_ROUND = 600;

	private static final int MAX_SOLDIER = 2;
	private static final int MAX_LUMBERJACK = 5;
	private static final int MAX_TANKS = 3;
	private static final int MAX_SCOUT = 3;

	private static final Direction[] TREE_BUILD_DIRS = { Direction.getNorth(), Direction.getEast(), Direction.getWest(),
			Direction.getWest().rotateLeftDegrees(2), Nav.NORTH_WEST.rotateLeftDegrees(15),
			Nav.NORTH_EAST.rotateRightDegrees(15), Nav.SOUTH_WEST.rotateRightDegrees(15),
			Nav.SOUTH_EAST.rotateLeftDegrees(15), };

	private float buildOffset;
	private Direction buildDirection;
	private int steps;
	private int stepsBeforeGiveUp;
	private boolean settled;
	private MapLocation baseLocation;
	private boolean hasPlantedFront;
	private boolean hasPlantedMiddle;
	private boolean hasFinishedPlanting;
	private int plantFailCount;

	public GardenerLogic(RobotController _rc) {
		super(_rc);
		Direction pointAt = rc.getLocation().directionTo(combat.getClosestEnemySpawn()).opposite()
				.rotateLeftDegrees(42.0f);
		buildOffset = TREE_BUILD_DIRS[0].degreesBetween(pointAt);
		buildDirection = TREE_BUILD_DIRS[0].opposite().rotateLeftDegrees(buildOffset);
		baseLocation = rc.getLocation();
		hasPlantedFront = hasPlantedMiddle = hasFinishedPlanting = false;
		plantFailCount = 0;

		stepsBeforeGiveUp = Math.round(combat.getClosestEnemySpawn().distanceTo(rc.getLocation()) / 1.6f);
	}

	@Override
	public void logic() {
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
		TreeInfo[] myTrees = rc.senseNearbyTrees(rc.getType().sensorRadius, rc.getTeam());
		buildRobots(trees);

		if (settled) {
			plantTrees();
			waterTrees(myTrees);
			orderClearTrees(trees);
		} else {
			findBaseLocation();
		}

		if (rc.getRoundNum() == RESETTLE_ROUND && myTrees.length < 10) {
			plantFailCount = 0;
			settled = hasPlantedFront = hasPlantedMiddle = hasFinishedPlanting = false;
			steps = 0;
		}

		nav.shakeTrees(trees);
	}

	void findBaseLocation() {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
		RobotInfo archon = nearest(RobotType.ARCHON, robots);
		RobotInfo gardener = nearest(RobotType.GARDENER, robots);

		if (((archon == null || archon.location.distanceTo(rc.getLocation()) >= 3.0f) && rc.hasTreeBuildRequirements()
				&& nav.isLocationFree(buildDirection) && nav.isLocationFree(buildDirection.opposite())
				&& steps > MIN_STEPS_BEFORE_SETTLE)
				|| (steps >= stepsBeforeGiveUp
						&& (gardener == null || gardener.location.distanceTo(rc.getLocation()) > 1.0f))) {
			settled = true;
			baseLocation = rc.getLocation();
		} else {

			if (archon != null) {
				nav.tryMove(archon.location.directionTo(rc.getLocation()));
			}

			if (gardener != null) {
				nav.tryMove(gardener.location.directionTo(rc.getLocation()));
			}

			nav.tryMove(buildDirection.opposite());

			steps++;
		}
	}

	void plantTrees() {
		if (!rc.hasTreeBuildRequirements()) {
			return;
		}
		
		// We have to plant the front two first to maximize plants
		if (!hasPlantedFront) {
			hasPlantedFront = moveAndPlant(buildDirection.opposite());
		} else if (!hasPlantedMiddle) {
			nav.tryMoveTo(baseLocation);
			if (rc.getLocation().distanceTo(baseLocation) < 0.1f) {
				hasPlantedMiddle = true;
				plantFailCount = 0;
			}
		} else if (!hasFinishedPlanting) {
			hasFinishedPlanting = moveAndPlant(buildDirection);
		} else {
			nav.tryMoveTo(baseLocation);
			try {
				for (Direction dir : TREE_BUILD_DIRS) {
					dir = dir.rotateLeftDegrees(buildOffset);
					if (rc.canPlantTree(dir)) {
						rc.plantTree(dir);
						break;
					} else if (rc.canPlantTree(dir.rotateRightDegrees(2))) {
						rc.plantTree(dir);
						break;
					}
				}
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	void waterTrees(TreeInfo[] trees) {
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
		int lumberjacks = census.count(RobotType.LUMBERJACK);
		int soldiers = census.count(RobotType.SOLDIER);
		int scouts = census.count(RobotType.SCOUT);

		if (treeSumRadius(trees) > TOO_MUCH_TREE_SUM_RADIUS && lumberjacks < MAX_LUMBERJACK) {
			build(RobotType.LUMBERJACK);
		}

		if (settled) {
			if (census.count(RobotType.TANK) < MAX_TANKS) {
				build(RobotType.TANK);
			}

			if (soldiers < 1) {
				build(RobotType.SOLDIER);
			} else if (scouts < 1) {
				build(RobotType.SCOUT);
			}

			if (soldiers < MAX_SOLDIER) {
				build(RobotType.SOLDIER);
			} else if (scouts < MAX_SCOUT) {
				build(RobotType.SCOUT);
			} else if (lumberjacks < MAX_LUMBERJACK) {
				build(RobotType.LUMBERJACK);
			}
		} else {
			// We have not settled and are in the process of building trees.
			if (soldiers < 1) {
				build(RobotType.SOLDIER);
			} else if (scouts < 1) {
				build(RobotType.SCOUT);
			} else if (soldiers < 2) {
				build(RobotType.SOLDIER);
			}
		}
	}

	void build(RobotType type) {
		if (!rc.isBuildReady() || !rc.hasRobotBuildRequirements(type)) {
			return;
		}

		float offset = 0.0f;

		while (offset < 360.0f) {
			Direction dir = buildDirection.rotateLeftDegrees((personality.getIsLeftHanded() ? 1 : 1) * offset);
			if (rc.canBuildRobot(type, dir)) {
				try {
					rc.buildRobot(type, dir);
					census.increment(type);
					break;
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
			offset += 10.0f;
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
		final MapLocation endPoint = firstThreeSteps.add(dir, rc.getType().strideRadius + 0.2f);

		nav.tryMoveTo(endPoint);
		if (!nav.isLocationFree(endPoint)) {
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

	float treeSumRadius(TreeInfo[] trees) {
		float sum = 0.0f;
		for (TreeInfo tree : trees) {
			sum += tree.getRadius();
		}
		return sum;
	}
}
