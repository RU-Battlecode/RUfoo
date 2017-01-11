package RUfoo.logic;

import java.util.Arrays;

import RUfoo.Util;
import RUfoo.managers.BuildInstructions;
import RUfoo.managers.Navigation;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;

public class GardenerLogic extends RobotLogic {

	private static final Direction[] SCOUT_BUILD_DIRECTIONS = { Direction.getNorth(),
			Direction.getEast().rotateRightDegrees(45), Direction.getWest().rotateLeftDegrees(45) };
	
	private int scoutsMade;
	private int failedBuildCount;

	public GardenerLogic(RobotController _rc) {
		super(_rc);
		scoutsMade = 0;
		failedBuildCount = 0;
	}

	@Override
	public void logic() {
		navManager.dodgeBullets();
		manageTrees();
		build();
		donate();
	}

	void build() {
		if (scoutsMade < 2) {
			build(RobotType.SCOUT);
		} else {
			build(RobotType.TANK);
			build(RobotType.SOLDIER);
		}
	}

	void donate() {
		if (rc.getTeamBullets() + rc.getTeamVictoryPoints() >= GameConstants.VICTORY_POINTS_TO_WIN) {
			try {
				rc.donate(rc.getTeamBullets());
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		} else if (rc.getTeamBullets() > 100) {
			try {
				rc.donate(rc.getTeamBullets() - 100);
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	void manageTrees() {
		// Load a garden construction plan.
		if (!constructionManager.hasLoadedPlan()) {
			constructionManager.loadPlan(BuildInstructions.SQUARE);
		}

		// Get the next instruction plan location. Null if done.
		MapLocation nextBuildLocation = constructionManager.peekInstructionLocation();
				
		if (nextBuildLocation != null) {
			System.out.println("My: " + rc.getLocation() + " next: " + nextBuildLocation + " dist: " + rc.getLocation().distanceTo(nextBuildLocation) + " spawn off: " + GameConstants.GENERAL_SPAWN_OFFSET);
			
			// Are we close enough to spawn the plant?
			if (rc.getLocation().distanceTo(nextBuildLocation) <= 2.0) {
				// Direction to the build site can be null if the robot is
				// directly on the location.
				Direction dir = rc.getLocation().directionTo(nextBuildLocation);

				if (dir == null) {
					// Try to take a step back because we are right on the location we want to build.
					navManager.moveRandom(GameConstants.GENERAL_SPAWN_OFFSET / 2);
				} else if (rc.canPlantTree(dir)) {
					try {
						rc.plantTree(dir);
						constructionManager.popInstructionLocation();
						System.out.println("built one!!!!!");
					} catch (GameActionException e) {
						e.printStackTrace();
						failedBuildCount++;
					}
				} else {
					failedBuildCount++;
				}
			} else {
				System.out.println("moving to next build location... was not close enough");
				navManager.moveAggressively(nextBuildLocation.add(new Direction(0, GameConstants.GENERAL_SPAWN_OFFSET)));
			}
		}

		if (failedBuildCount > personalityManager.getPatience()) {
			failedBuildCount = 0;
			// Give up, must be something in the way.
			constructionManager.popInstructionLocation();
		}
		
		// Water trees if you can
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius, rc.getTeam());
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
			} else {
				navManager.moveAggressively(tree.location);
			}
		}
	}

	void build(RobotType type) {
		Direction dir = Util.randomChoice(Navigation.DIRECTIONS);
		if (type == RobotType.SCOUT) {
			dir = SCOUT_BUILD_DIRECTIONS[scoutsMade % SCOUT_BUILD_DIRECTIONS.length];
		}

		if (rc.canBuildRobot(type, dir)) {
			try {
				rc.buildRobot(type, dir);
				if (type == RobotType.SCOUT) {
					scoutsMade++;
				}
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

}
