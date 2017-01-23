package RUfoo.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * ArchonLogic.java - Archons attempt to build bases to farm bullets,
 * while avoiding bullets. A perfect Archon base looks like: 
 *        
 *             *
 *      *            *         
 *         
 *    *       /\       *
 *            \/
 *      *            *
 *         
 * Each * represents a Gardener @see GardenerLogic
 * 
 * The Archon will rotate it's base so that the north * is facing the closest
 * enemy initial Archon location.
 * 
 * @author Ben
 * 
 */
public class ArchonLogic extends RobotLogic {

	// Prioritized build directions
	private List<Direction> buildDirs = new ArrayList<>(
			Arrays.asList(new Direction[] { Direction.getNorth(), Navigation.NORTH_EAST, Navigation.NORTH_WEST,
					Direction.getEast(), Direction.getWest(), Navigation.SOUTH_EAST, Navigation.SOUTH_WEST, }));

	private float buildOffset;
	private MapLocation enemySpawn;

	public ArchonLogic(RobotController _rc) {
		super(_rc);

		enemySpawn = combat.getClosestEnemySpawn();

		Direction pointAt = rc.getLocation().directionTo(enemySpawn);
		buildOffset = buildDirs.get(0).degreesBetween(pointAt);
	}

	@Override
	public void logic() {

		if (rc.getLocation().distanceTo(enemySpawn) < 20) {
			nav.moveBest(enemySpawn.directionTo(rc.getLocation()));
		}

		buildBase();
		nav.dodgeBullets();
	}

	void buildBase() {
		Direction built = null;
		for (Direction dir : buildDirs) {
			Direction adjusted = dir.rotateLeftDegrees(buildOffset);
			if (hireGardener(adjusted)) {
				built = dir;
				break;
			}
		}

		if (built != null) {
			buildDirs.remove(built);
		}

		
		Direction dir = nav.randomDirection();
		if (countGardners() < 3 && rc.canBuildRobot(RobotType.GARDENER, dir)) {
			buildDirs.add(dir);
		}
	}

	int countGardners() {
		RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
		int gardenerCount = 0;
		for (RobotInfo robot : robots) {
			if (robot.type == RobotType.GARDENER) {
				gardenerCount++;
			}
		}

		return gardenerCount;
	}

	boolean hireGardener(Direction dir) {
		try {
			float offset = 0.0f;
			while (offset < 360.0f) {
				if (rc.canHireGardener(dir.rotateRightDegrees(personality.getIsLeftHanded() ? -offset : offset))) {
					rc.hireGardener(dir.rotateRightDegrees(offset));
					return true;
				}
				offset += 15.0f;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}

		return false;
	}

	void orderClearTrees() {
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);

		// Biggest trees first!
		Arrays.sort(trees, (t1, t2) -> {
			return Math.round(t2.radius - t1.radius);
		});

		for (TreeInfo tree : trees) {
			radio.requestCutTreeAt(tree.location);
			break;
		}
	}
}
