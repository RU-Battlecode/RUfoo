package RUfoo.logic;

import java.util.ArrayList;
import java.util.List;

import RUfoo.managers.Navigation;
import RUfoo.util.Util;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public class ArchonLogic extends RobotLogic {

	private Direction buildDir;
	
	public ArchonLogic(RobotController _rc) {
		super(_rc);
		buildDir = nav.randomDirection();
	}

	@Override
	public void logic() {
		RobotInfo[] robots = rc.senseNearbyRobots();
		List<RobotInfo> gardeners = new ArrayList<>();

		for (RobotInfo info : robots) {
			if (info.getTeam() == rc.getTeam()) {
				if (info.getType() == RobotType.GARDENER) {
					gardeners.add(info);
				}
			} else {
				// Sensed other team...
			}
		}

		// Do we need to build more gardeners?
		if (rc.isBuildReady() && gardeners.size() < 6) {
			buildGardener();
		}

		nav.dodgeBullets();
		nav.runAway();
		
		try {
			if (!rc.onTheMap(rc.getLocation().add(buildDir, rc.getType().sensorRadius / 2)) ||
					!rc.canMove(buildDir, rc.getType().strideRadius)) {
				buildDir = personality.getIsLeftHanded() ?  buildDir.rotateLeftDegrees(45) : buildDir.rotateRightDegrees(45);
			}
		} catch (GameActionException e) {
			
			e.printStackTrace();
		}
		
		if (!rc.isBuildReady()) {
			nav.moveBest(buildDir);
		}
		
		orderClearTrees();
	}

	void buildGardener() {
		Direction dir = Util.randomChoice(Navigation.DIRECTIONS);
		if (rc.canBuildRobot(RobotType.GARDENER, dir)) {
			try {
				rc.buildRobot(RobotType.GARDENER, dir);
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
