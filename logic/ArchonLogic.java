package RUfoo.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import RUfoo.managers.Navigation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

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
		
		if (rc.getLocation().distanceTo(enemySpawn) < 20){
			nav.moveBest(enemySpawn.directionTo(rc.getLocation()));
		}
		
		buildBase();
		nav.dodgeBullets();
	}

	void buildBase() {
		Direction built = null;
		for (Direction dir : buildDirs) {
			Direction adjusted = dir.rotateLeftDegrees(buildOffset);
			if (buildGardener(adjusted)) {
				built = dir;
				break;
			}
		}

		if (built != null) {
			buildDirs.remove(built);
		}

		if (rc.getRoundNum() == 600 && rc.getTeamVictoryPoints() < GameConstants.VICTORY_POINTS_TO_WIN * 0.80f) {
			for (int i = 0; i < 2; i++)
				buildDirs.add(nav.randomDirection());
		}
	}

	boolean buildGardener(Direction dir) {
		if (rc.canBuildRobot(RobotType.GARDENER, dir)) {
			try {
				rc.buildRobot(RobotType.GARDENER, dir);
				return true;
			} catch (GameActionException e) {
				e.printStackTrace();
			}
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
