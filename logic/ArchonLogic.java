package RUfoo.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import RUfoo.managers.Navigation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
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
	
	public ArchonLogic(RobotController _rc) {
		super(_rc);

		Direction pointAt = rc.getLocation().directionTo(combat.getClosestEnemySpawn());
		buildOffset = buildDirs.get(0).degreesBetween(pointAt); 
	}

	@Override
	public void logic() {
		buildBase();
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
			for (int i = 0; i < 5; i++) buildDirs.add(nav.randomDirection());
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

		for (TreeInfo tree : trees) {
			radio.requestCutTreeAt(tree.location);
			break;
		}
	}
}
