package RUfoo.logic;

import java.util.Arrays;

import RUfoo.managers.Nav;
import RUfoo.model.DefenseInfo;
import RUfoo.util.Util;
import battlecode.common.BodyInfo;
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

	private static final float TOO_MUCH_TREE_SUM_RADIUS = 10.0f;
	private static final int MIN_STEPS_BEFORE_SETTLE = 6;
	private static final int RESETTLE_ROUND = 600;
	private static final float MIN_DIST_TO_GARDENERS = 8.5f;

	private static final int MAX_SOLDIER = 8;
	private static final int MAX_LUMBERJACK = 5;
	private static final int MAX_TANKS = 4;
	private static final int MAX_SCOUT = 2;

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
	private boolean hasCalledForDefense;

	private static final float SMALL_MAP_SIZE = 69.0f;
	private boolean smallMap = false;

	public GardenerLogic(RobotController _rc) {
		super(_rc);

		Direction pointAt = rc.getLocation().directionTo(combat.getClosestEnemySpawn()).opposite()
				.rotateLeftDegrees(20.0f);

		RobotInfo archon = nearest(RobotType.ARCHON, rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam()));
		if (archon != null) {
			pointAt = archon.location.directionTo(rc.getLocation()).opposite();
		}

		buildOffset = TREE_BUILD_DIRS[0].degreesBetween(pointAt);
		buildDirection = TREE_BUILD_DIRS[0].opposite().rotateLeftDegrees(buildOffset);
		baseLocation = rc.getLocation();
		hasPlantedFront = hasPlantedMiddle = hasFinishedPlanting = false;
		plantFailCount = 0;

		stepsBeforeGiveUp = Math.round(combat.getClosestEnemySpawn().distanceTo(rc.getLocation()) / 1.6f);

		smallMap = rc.getInitialArchonLocations(rc.getTeam())[0]
				.distanceTo(combat.getFurthestEnemySpawn()) <= SMALL_MAP_SIZE;
	}

	@Override
	public void logic() {
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
		TreeInfo[] myTrees = rc.senseNearbyTrees(rc.getType().sensorRadius, rc.getTeam());
		
		if (rc.getHealth() / rc.getType().maxHealth < 0.40f && Util.contains(enemies, RobotType.SOLDIER) && !Util.contains(enemies, RobotType.TANK)) {
			tryHardPlant(buildDirection);
		}
		
		buildRobots(trees);

		if (settled) {
			plantTrees();
			orderClearTrees(trees);
		} else {
			findBaseLocation(enemies);
		}

		waterTrees(myTrees);

		if (rc.getRoundNum() == RESETTLE_ROUND && myTrees.length < 10) {
			plantFailCount = 0;
			settled = hasPlantedFront = hasPlantedMiddle = hasFinishedPlanting = false;
			steps = 0;
		}

		nav.shakeTrees(trees);
		
		// Archons can report enemies archons too!
		int defenseNeed = 0;
		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.ARCHON) {
				radio.foundEnemyArchon(enemy);
			}

			defenseNeed += DefenseInfo.unitValue(enemy.type);
		}

		for (RobotInfo friend : friends) {
			defenseNeed -= DefenseInfo.unitValue(friend.type);
		}

		if (defenseNeed > 0) {
			if (!hasCalledForDefense) {
				radio.requestDefense(rc.getLocation(), defenseNeed);
				hasCalledForDefense = true;
			}
		}

	}

	void tryHardPlant(Direction dir) {
		float offset = 0.0f;
		while (offset < 360.0f) {
			if (rc.canPlantTree(dir.rotateRightDegrees(offset))) {
				try {
					rc.plantTree(dir.rotateRightDegrees(offset));
					break;
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
		}
	}

	void findBaseLocation(RobotInfo[] enemies) {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
		RobotInfo archon = nearest(RobotType.ARCHON, robots);
		RobotInfo gardener = nearest(RobotType.GARDENER, robots);

		if (shouldSettle(archon, gardener, enemies)) {
			settled = true;
			baseLocation = rc.getLocation();
		} else {

			BodyInfo[] obstacles = Util.addAll(robots, rc.senseNearbyTrees());
			if (enemies.length > 0) {
				nav.runAway(enemies);
				steps++;
			}
			
			if (archon != null) {
				Direction awayFromArchon = rc.getLocation().directionTo(archon.location).opposite();
				nav.bug(rc.getLocation().add(awayFromArchon, stepsBeforeGiveUp * rc.getType().strideRadius), obstacles);
				steps++;
			}

			if (gardener != null) {
				Direction awayFromGardener = rc.getLocation().directionTo(gardener.location).opposite();
				nav.bug(rc.getLocation().add(awayFromGardener, stepsBeforeGiveUp * rc.getType().strideRadius),
						obstacles);
				steps++;
			}

			nav.tryMove(buildDirection.opposite());

		}
	}

	boolean shouldSettle(RobotInfo archon, RobotInfo gardener, RobotInfo[] enemies) {
		return ((gardener == null || gardener.location.distanceTo(rc.getLocation()) >= MIN_DIST_TO_GARDENERS)
				&& nav.isLocationFree(buildDirection) && nav.isLocationFree(buildDirection.opposite())
				&& steps > MIN_STEPS_BEFORE_SETTLE && farEnoughFromEdge());
	}

	boolean farEnoughFromEdge() {
		boolean farEnough = true;
		for (Direction dir : Nav.DIRECTIONS) {	
			try {
				if (!rc.onTheMap(rc.getLocation().add(dir, rc.getType().bodyRadius * 2.1f))) {
					farEnough = false;
					break;
				}
			} catch (GameActionException e) {
				e.printStackTrace();
			}	
		}
		
		return farEnough;
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
			if (rc.getLocation().distanceTo(baseLocation) < 0.05f) {
				hasPlantedMiddle = true;
				plantFailCount = 0;
			}
		} else if (!hasFinishedPlanting) {
			hasFinishedPlanting = moveAndPlant(buildDirection);
		} else {
			nav.tryMoveTo(baseLocation);
			if (rc.getLocation().distanceTo(baseLocation) < 0.1f) {
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

		if (!smallMap && treeSumRadius(trees) > TOO_MUCH_TREE_SUM_RADIUS && lumberjacks < MAX_LUMBERJACK) {
			build(RobotType.LUMBERJACK);
		} else if (smallMap && treeSumRadius(trees) > TOO_MUCH_TREE_SUM_RADIUS && lumberjacks < 1) {
			build(RobotType.LUMBERJACK);
		}

		if (settled) {
			if (census.count(RobotType.TANK) < MAX_TANKS) {
				build(RobotType.TANK);
			}

			if (soldiers < 1) {
				build(RobotType.SOLDIER);
			} else if (!smallMap && scouts < 1) {
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
			} else if (!smallMap && scouts < 1) {
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
			offset += 5.0f;
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
