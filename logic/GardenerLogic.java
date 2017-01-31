package RUfoo.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import RUfoo.managers.Nav;
import RUfoo.model.DefenseInfo;
import RUfoo.util.Util;
import battlecode.common.BodyInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
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

	private static final float TOO_MUCH_TREE_SUM_RADIUS = 8.0f;
	private static final int MIN_STEPS_BEFORE_SETTLE = 6;
	private static final float MIN_DIST_TO_GARDENERS = 8.5f;

	private static int MAX_SOLDIER = 8;
	private static final int MAX_LUMBERJACK = 7;
	private static final int MAX_TANKS = 5;
	private static final int MAX_SCOUT = 3;

	private static final Direction[] TREE_BUILD_DIRS = { Direction.getNorth(), Direction.getEast(), Direction.getWest(),
			Direction.getWest().rotateLeftDegrees(2), Direction.getEast().rotateLeftDegrees(2),
			Nav.NORTH_WEST.rotateLeftDegrees(15), Nav.NORTH_EAST.rotateRightDegrees(15),
			Nav.SOUTH_WEST.rotateRightDegrees(15), Nav.SOUTH_EAST.rotateLeftDegrees(15), };

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
	private MapLocation inheritedBaseLocation;
	private List<MapLocation> forgetInheritedLocations;
	private int inheritedFrustration;

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

		float dist = rc.getInitialArchonLocations(rc.getTeam())[0]
				.distanceTo(combat.getFurthestEnemySpawn()); 
		smallMap = dist <= SMALL_MAP_SIZE;
	
		forgetInheritedLocations = new ArrayList<>();
		inheritedBaseLocation = null;
		inheritedFrustration = 0;
	}

	@Override
	public void logic() {
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
		TreeInfo[] myTrees = rc.senseNearbyTrees(rc.getType().sensorRadius, rc.getTeam());

		if (rc.getHealth() / rc.getType().maxHealth < 0.40f && Util.contains(enemies, RobotType.SOLDIER)
				&& !Util.contains(enemies, RobotType.TANK)) {
			tryHardPlant(buildDirection);
		}
		
		buildRobots(trees);

		if (settled) {
			plantTrees();
			orderClearTrees(trees);
		} else {
			findBaseLocation(enemies, friends, myTrees);
		}

		waterTrees(myTrees);

		if (enemies.length > 0) {
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

		if (Clock.getBytecodesLeft() > 300) {
			nav.shakeTrees(trees);
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

	void findBaseLocation(RobotInfo[] enemies, RobotInfo[] friends, TreeInfo[] myTrees) {

		RobotInfo archon = nearest(RobotType.ARCHON, friends);

		if (shouldSettle(archon, enemies, myTrees)) {
			settled = true;
			baseLocation = rc.getLocation();
			radio.sendSettledBase(baseLocation);
		} else {

			BodyInfo[] obstacles = Util.addAll(friends, rc.senseNearbyTrees());
			
			if (inheritedFrustration < personality.getPatience()) {
				lookForInherritableBase(friends, myTrees, obstacles);
			}
			
			if (enemies.length > 0) {
				nav.runAway(enemies);
				steps++;
			} else if (archon != null) {
				Direction awayFromArchon = rc.getLocation().directionTo(archon.location).opposite();
				nav.bug(rc.getLocation().add(awayFromArchon, stepsBeforeGiveUp * rc.getType().strideRadius * 5),
						obstacles);
				steps++;
			}

			if (myTrees.length != 0) {
				Direction awayFromTree = myTrees[0].location.directionTo(rc.getLocation());
				float dist = MIN_DIST_TO_GARDENERS - myTrees[0].location.distanceTo(rc.getLocation());
				nav.bug(rc.getLocation().add(awayFromTree, dist * 2), obstacles);
				steps++;
			}

			nav.tryMove(buildDirection.opposite());
		}
	}
	
	void lookForInherritableBase(RobotInfo[] friends, TreeInfo[] myTrees, BodyInfo[] obstacles) {
		List<MapLocation> gardenerLocs = radio.readGardenerBaseLocations();

		if (inheritedBaseLocation != null
				&& inheritedBaseLocation.isWithinDistance(rc.getLocation(), rc.getType().sensorRadius)) {
			for (RobotInfo friend : friends) {
				if (friend.type == RobotType.GARDENER && friend.location.distanceTo(inheritedBaseLocation) < 0.1f) {
					forgetInheritedLocations.add(inheritedBaseLocation);
					inheritedBaseLocation = null;
					inheritedFrustration++;
					break;
				}
			}
		}

		boolean foundBase = false;
		// Look at all my trees
		for (TreeInfo tree : myTrees) {
			
			if (foundBase) {
				break;
			}
			
			// See if it doesn't have a gardener
			if (!treeHasGardener(tree, friends)) {
				// Find the base location to this fallen gardener
				for (MapLocation loc : gardenerLocs) {

					if (!forgotten(loc)
							&& loc.distanceTo(tree.location) <= RobotType.GARDENER.strideRadius
									+ GameConstants.BULLET_TREE_RADIUS) {
						inheritedBaseLocation = loc;
						foundBase = true;
						inheritedFrustration++;
						break;
					}
				}
			}
		}

		if (inheritedBaseLocation != null) {
			if (rc.getLocation().distanceTo(inheritedBaseLocation) < 0.1f) {
				settled = true;
				baseLocation = rc.getLocation();
			} else {
				nav.bug(inheritedBaseLocation, obstacles);
				inheritedFrustration++;
			}
		}
	}
	
	boolean forgotten(MapLocation loc) {
		for (MapLocation forgotten : forgetInheritedLocations) {
			if (forgotten.distanceTo(loc) < 2.0f) {
				return true;
			}
		}
		
		return false;
	}

	boolean treeHasGardener(TreeInfo tree, RobotInfo[] friends) {
		boolean hasGardener = false;
		for (RobotInfo friend : friends) {
			if (friend.type == RobotType.GARDENER
					&& friend.location.distanceTo(tree.location) <= RobotType.GARDENER.strideRadius) {
				hasGardener = true;
				break;
			}
		}

		return hasGardener;
	}

	boolean shouldSettle(RobotInfo archon, RobotInfo[] enemies, TreeInfo[] nearestMyTree) {

		float distToGardener = nearestMyTree.length == 0 ? 0 : nearestMyTree[0].location.distanceTo(rc.getLocation());
		return ((nearestMyTree.length == 0 || distToGardener >= MIN_DIST_TO_GARDENERS
				|| (personality.age() > 400 && distToGardener >= MIN_DIST_TO_GARDENERS / 2))

				&& nav.isLocationFree(buildDirection) && nav.isLocationFree(buildDirection.opposite())

				&& steps > MIN_STEPS_BEFORE_SETTLE && farEnoughFromEdge());
	}

	boolean farEnoughFromEdge() {
		boolean farEnough = true;
		for (Direction dir : Nav.DIRECTIONS) {
			try {
				if (!rc.onTheMap(rc.getLocation().add(dir, rc.getType().bodyRadius * 2.2f))) {
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
						float offset = 0.0f;
						boolean planted = false;
						while (offset <= 4.0f && !planted) {
							if (rc.canPlantTree(dir.rotateLeftDegrees(offset))) {
								rc.plantTree(dir.rotateLeftDegrees(offset));
								planted = true;
							} else if (rc.canPlantTree(dir.rotateRightDegrees(offset))) {
								rc.plantTree(dir.rotateRightDegrees(offset));
								planted = true;
							}

							offset += 0.5f;
						}

						if (planted) {
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
		int tanks = census.count(RobotType.TANK);
		
		if (rc.getRoundNum() > 700) {
			MAX_SOLDIER = 10;
		}
		
		if (!smallMap && treeSumRadius(trees) > TOO_MUCH_TREE_SUM_RADIUS && lumberjacks < MAX_LUMBERJACK) {
			build(RobotType.LUMBERJACK);
		} else if (smallMap && treeSumRadius(trees) > TOO_MUCH_TREE_SUM_RADIUS && lumberjacks < 1) {
			build(RobotType.LUMBERJACK);
		}

		if (settled) {
			if (tanks < MAX_TANKS) {
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
			} else if (soldiers < 1) {
				build(RobotType.SOLDIER);
			} else if (scouts < 1) {
				build(RobotType.SCOUT);
			} else if (tanks < MAX_TANKS) {
				build(RobotType.TANK);
			}
		}
	}

	void build(RobotType type) {
		if (!rc.isBuildReady() || !rc.hasRobotBuildRequirements(type)) {
			return;
		}

		float offset = 0.0f;

		while (offset < 350.0f) {
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
			offset += 6.0f;
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
		final MapLocation endPoint = firstThreeSteps.add(dir, rc.getType().strideRadius + 0.21f);

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
