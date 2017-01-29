package RUfoo.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import RUfoo.managers.Channel;
import RUfoo.managers.DefenseInfo;
import RUfoo.managers.Nav;
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

	private static final float MIN_DISTANCE_TO_ENEMY_SPAWN = 20.0f;
	private static final int STRICT_GARDENER_LIMIT_UNTIL_ROUND = 122;
	private static final int GARDENER_LIMIT_UNTIL_ROUND = 600;
	private static int gardenerLimit;

	// Prioritized build directions
	private List<Direction> buildDirs = new ArrayList<>(Arrays.asList(new Direction[] { Direction.getNorth() }));

	private float buildOffset;
	private MapLocation enemySpawn;
	private Boolean isLeader;
	private boolean hasCalledForDefense;

	public ArchonLogic(RobotController _rc) {
		super(_rc);

		enemySpawn = combat.getClosestEnemySpawn();
		Direction pointAt = rc.getLocation().directionTo(enemySpawn);
		buildOffset = buildDirs.get(0).degreesBetween(pointAt);

		isLeader = rc.getInitialArchonLocations(rc.getTeam()).length == 1 ? true : null;
		
		gardenerLimit = 2 * rc.getInitialArchonLocations(rc.getTeam()).length + 6;
		hasCalledForDefense = false;
	}

	@Override
	public void logic() {

		if (isLeader == null) {
			castArchonVote();
		}

		RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);

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
		
		// Move away from enemy spawn if it is too close.
		if (rc.getLocation().distanceTo(enemySpawn) < MIN_DISTANCE_TO_ENEMY_SPAWN) {
			nav.tryHardMove(enemySpawn.directionTo(rc.getLocation()));
		}

		int gardeners = census.count(RobotType.GARDENER);

		buildBase(gardeners);

		nav.dodge(rc.senseNearbyBullets());
		nav.runAway(enemies);
		moveOffOfGardeners(friends);

		orderClearTrees(trees);
		nav.shakeTrees(trees);
		
		//readBroadCastingRobots();
	}

	void readBroadCastingRobots() {
		MapLocation[] locations = rc.senseBroadcastingRobotLocations();
		for (MapLocation loc : locations) {
			rc.setIndicatorLine(rc.getLocation(), loc, 100, 0, 0);
		}
		
	}

	void buildBase(int gardenersAlive) {
		Direction built = null;
		for (Direction dir : buildDirs) {
			Direction adjusted = dir.rotateLeftDegrees(buildOffset);
			if (hireGardener(adjusted, gardenersAlive)) {
				built = dir;
				break;
			}
		}
		
		// There are buildDirs and the Archon could not build
		if (buildDirs.size() > 0 && built == null) {
			makeRoomForGardener();
		}
		
		if (built != null) {
			buildDirs.remove(built);
		}

		// Build orders
		int round = rc.getRoundNum();
		switch (round) {
		case 20:
			buildDirs.add(Nav.NORTH_EAST);
			break;
		case 50:
			buildDirs.add(Nav.NORTH_WEST);
			break;
		case 70:
			buildDirs.add(Direction.getEast());
			buildDirs.add(Direction.getWest());
			break;
		case 100:
			buildDirs.add(Nav.SOUTH_EAST);
			buildDirs.add(Nav.SOUTH_WEST);
			break;
		default:
			Direction dir = nav.randomDirection();
			if (round > 120 && gardenersAlive < gardenerLimit && rc.canBuildRobot(RobotType.GARDENER, dir)) {
				buildDirs.add(dir);
			}
			
			if (round == 1000) {
				gardenerLimit += 5;
			}
		}
	}

	void makeRoomForGardener() {
		for (Direction dir : Nav.DIRECTIONS) {
			MapLocation gardenerSpawn = rc.getLocation().add(dir, rc.getType().bodyRadius + GameConstants.GENERAL_SPAWN_OFFSET);
			try {
				if (rc.isCircleOccupiedExceptByThisRobot(gardenerSpawn, RobotType.GARDENER.bodyRadius)) {
					if (nav.tryHardMove(dir.opposite())) {
						break;
					}
				}
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void castArchonVote() {
		int votes = 0;
		for (Direction dir : Nav.DIRECTIONS) {
			try {
				if (rc.isLocationOccupied(rc.getLocation().add(dir, rc.getType().strideRadius))) {
					votes++;
				}
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}

		int leaderId = radio.readChannel(Channel.ARCHON_LEADER_ID);
		// Get the votes for the highest archon
		int votesForOther = radio.readChannel(Channel.ARCHON_LEADER_POLL);

		if (leaderId == 0) {
			// There is no archon... so take leadership
			radio.broadcast(Channel.ARCHON_LEADER_ID, rc.getID());
			radio.broadcast(Channel.ARCHON_LEADER_POLL, votes);
			// Can't tell if I am leader yet... tho

		} else if (votes > votesForOther) {
			// I am better than the first at least (maybe better than both if
			// there are 3).
			radio.broadcast(Channel.ARCHON_LEADER_ID, rc.getID());
			radio.broadcast(Channel.ARCHON_LEADER_POLL, votes);
		} else {
			isLeader = false;
		}

		if (leaderId == rc.getID()) {
			// Boom
			isLeader = true;
		} else if (leaderId != 0 && votes <= votesForOther) {
			isLeader = false;
		}

	}

	boolean hireGardener(Direction dir, int gardeners) {

		if (rc.getRoundNum() < STRICT_GARDENER_LIMIT_UNTIL_ROUND
				&& rc.getTeamBullets() < GameConstants.BULLETS_INITIAL_AMOUNT) {
			if (gardeners >= 1) {
				return false;
			}
		} else if (rc.getRoundNum() < GARDENER_LIMIT_UNTIL_ROUND && gardeners > gardenerLimit) {
			return false;
		} else if (!rc.hasRobotBuildRequirements(RobotType.GARDENER)) {
			return false;
		}

		try {
			float offset = 0.0f;
			while (offset < 360.0f) {
				Direction hireDir = dir.rotateRightDegrees(offset);
				if (rc.canHireGardener(hireDir)) {
					rc.hireGardener(hireDir);
					census.increment(RobotType.GARDENER);
					return true;
				} else {
					System.out.println("can't build");
				}
				offset += 10.0f;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}

		return false;
	}

	void orderClearTrees(TreeInfo[] trees) {
		// Biggest trees first!
		Arrays.sort(trees, (t1, t2) -> {
			return Math.round(t2.radius - t1.radius);
		});

		for (TreeInfo tree : trees) {
			radio.requestCutTreeAt(tree.location);
			break;
		}
	}

	private void moveOffOfGardeners(RobotInfo[] robots) {
		for (RobotInfo robot : robots) {
			if (robot.type == RobotType.GARDENER
					&& robot.location.distanceTo(rc.getLocation()) <= rc.getType().bodyRadius * 2.0f) {
				nav.tryHardMove(robot.location.directionTo(rc.getLocation()));
				break;
			}
		}
	}
}
