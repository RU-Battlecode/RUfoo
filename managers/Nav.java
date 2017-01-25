package RUfoo.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import RUfoo.util.Util;
import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.TreeInfo;

public class Nav {

	private RobotController rc;

	public static final Direction NORTH_EAST = Direction.getNorth().rotateRightDegrees(45);
	public static final Direction SOUTH_EAST = Direction.getEast().rotateRightDegrees(45);
	public static final Direction SOUTH_WEST = Direction.getSouth().rotateRightDegrees(45);
	public static final Direction NORTH_WEST = Direction.getWest().rotateRightDegrees(45);

	public static final Direction[] DIRECTIONS = { Direction.getNorth(), NORTH_EAST, Direction.getEast(), SOUTH_EAST,
			Direction.getSouth(), SOUTH_WEST, Direction.getWest(), NORTH_WEST };

	public Nav(RobotController _rc) {
		rc = _rc;
	}

	/**
	 * Try to move directly onto the target location If failed then move in the
	 * direction of the target location
	 * 
	 * @param loc
	 * @return
	 */
	public boolean tryMoveTo(MapLocation loc) {
		// Check we have not moved
		if (rc.hasMoved()) {
			return false;
		}

		try {
			if (!rc.canSenseLocation(loc) || !rc.onTheMap(loc)) {
				return false;
			}

			// Move directly to location
			if (rc.canMove(loc)) {
				rc.move(loc);
				return true;
			} else {
				// Try to move in the direction to the target location.
				// Get distance we can/need to travel.
				float dist = Math.min(rc.getType().strideRadius, Math.max(0, rc.getLocation().distanceTo(loc)));
				return tryMove(rc.getLocation().directionTo(loc), dist);
			}
		} catch (GameActionException e1) {
			e1.printStackTrace();
		}
		return false;
	}

	public boolean tryMove(Direction dir) {
		return tryMove(dir, rc.getType().strideRadius);
	}

	public boolean tryMove(Direction dir, float dist) {
		if (!rc.hasMoved() && rc.canMove(dir, dist)) {
			try {
				rc.move(dir, dist);
				return true;
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public void tryHardMove(Direction dir) {
		tryHardMove(dir, rc.getType().strideRadius);
	}

	public void tryHardMove(Direction dir, float dist) {
		tryHardMove(dir, dist, 180.0f);
	}

	public void tryHardMove(Direction dir, float dist, float maxDegreesOff) {
		if (rc.hasMoved()) {
			return;
		}

		try {
			float offset = 0.0f;
			while (offset < maxDegreesOff) {
				if (rc.canMove(dir.rotateRightDegrees(offset), dist)) {
					rc.move(dir.rotateRightDegrees(offset), dist);
					break;
				} else if (rc.canMove(dir.rotateRightDegrees(-offset), dist)) {
					rc.move(dir.rotateRightDegrees(-offset), dist);
					break;
				}
				offset += 15.0f;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public void moveByTrees(TreeInfo[] trees) {
		if (rc.hasMoved()) {
			return;
		}

		Arrays.sort(trees, (t1, t2) -> {
			return t1.containedBullets - t2.containedBullets;
		});

		for (TreeInfo tree : trees) {
			if (tryMove(rc.getLocation().directionTo(tree.location))) {
				break;
			}
		}
	}

	public List<Direction> safeDirections(BulletInfo[] bullets, RobotInfo[] enemies) {
		return safeDirections(rc.getType().strideRadius, bullets, enemies);
	}

	public List<Direction> safeDirections(float dist, BulletInfo[] bullets, RobotInfo[] enemies) {
		List<Direction> safeDirections = new ArrayList<>();

		// Check all the general directions
		for (Direction dir : DIRECTIONS) {
			try {
				if (!rc.canMove(dir) || !rc.onTheMap(rc.getLocation().add(dir))) {
					continue;
				}
			} catch (GameActionException e) {
				e.printStackTrace();
				continue;
			}

			if (isDirectionSafe(dir, rc.getType().strideRadius, bullets, enemies)) {
				safeDirections.add(dir);
			}
		}

		return safeDirections;
	}

	public boolean isDirectionSafe(Direction dir, float dist, BulletInfo[] bullets, RobotInfo[] enemies) {
		boolean safeSoFar = true;

		MapLocation targetPosition = rc.getLocation().add(dir, dist);

		if (enemies != null) {
			for (RobotInfo enemy : enemies) {
				switch (enemy.getType()) {
				case SOLDIER:
				case TANK:
				case SCOUT:
					if (targetPosition.distanceTo(enemy.location) <= rc.getType().strideRadius
							&& enemy.attackCount < 1) {
						// The enemy would be too close and they have not
						// attacked.
						return false;
					}

					break;
				case LUMBERJACK:
					if (enemy.attackCount < 1
							&& enemy.location.distanceTo(targetPosition) <= GameConstants.LUMBERJACK_STRIKE_RADIUS) {
						// The lumber jack has not attacked and is way too
						// close.
						return false;
					}
					break;
				default:
					break;
				}
			}
		}

		for (BulletInfo bullet : bullets) {
			// This is where the bullet will be!
			MapLocation futureBulletLoc = bullet.getLocation().add(bullet.getDir(), bullet.getSpeed());
			// Check if that bullet is in our body radius of our futureLocation.
			if (futureBulletLoc.isWithinDistance(targetPosition, rc.getType().bodyRadius)) {
				safeSoFar = false;
				break;
			}
		}

		return safeSoFar;
	}

	public void moveSafelyTo(MapLocation target, BulletInfo[] bullets, RobotInfo[] enemies) {
		moveTo(target, bullets, enemies, true);
	}

	public void moveAggressivelyTo(MapLocation target, BulletInfo[] bullets, RobotInfo[] enemies) {
		moveTo(target, bullets, enemies, false);
	}

	void moveTo(MapLocation target, BulletInfo[] bullets, RobotInfo[] enemies, boolean prioritizeSafety) {

		float distToTarget = rc.getLocation().distanceTo(target);

		// Already there? or can't move?
		if (distToTarget < GameConstants.GENERAL_SPAWN_OFFSET || rc.hasMoved()) {
			return;
		}

		float dist = Math.max(0, Math.min(rc.getType().strideRadius, distToTarget - 0.1f));

		// The direct direction to the target location.
		Direction direct = rc.getLocation().directionTo(target);

		// Find the safest directions that is closest to the direction we want
		// to move.
		List<Direction> safeDirs = safeDirections(dist, bullets, enemies);

		Direction best = (safeDirs.size() == 0 ? null : Collections.min(safeDirs, (dir1, dir2) -> {
			return Math.round(dir1.degreesBetween(direct) - dir2.degreesBetween(direct));
		}));

		// Try to move in the safest direct, else just move directly to
		// location.
		try {
			if (best != null) {
				float degrees = best.degreesBetween(direct);
				if (prioritizeSafety) {
					// Obvious choice. Be safe.
					rc.move(best, dist);
				} else if ((degrees <= 30 || (degrees >= 330 && degrees <= 360)) && rc.canMove(direct, dist)) {
					// The safest is to far off course for someone who
					// does not care about safety. Just take the bullet.
					rc.move(direct, dist);
				}
			}

			if (!rc.hasMoved()) {
				// There was no best route, just move directly toward target
				tryHardMove(direct, dist);
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public void dodge(BulletInfo[] bullets) {
		if (rc.hasMoved()) {
			return;
		}

		if (bullets.length > 0) {
			List<Direction> possibleDirs = safeDirections(bullets, null);
			for (Direction dir : possibleDirs) {
				if (tryMove(dir)) {
					break;
				}
			}
		}
	}

	public void runAway(RobotInfo[] enemies) {
		RobotInfo scariest = null;
		int scariness = 0;
		for (RobotInfo enemy : enemies) {
			if (scaryFactor(enemy) >= scariness || scariest == null) {
				scariest = enemy;
			}
		}

		if (scariest != null) {
			tryHardMove(scariest.location.directionTo(rc.getLocation()));
		}

	}

	private int scaryFactor(RobotInfo enemy) {
		int scariness = 0;
		switch (enemy.getType()) {
		case ARCHON:
		case GARDENER:
			// ha
			break;
		case LUMBERJACK:
			scariness += 1;

			if (enemy.location.distanceTo(rc.getLocation()) <= GameConstants.LUMBERJACK_STRIKE_RADIUS) {
				scariness += 2;
				if (enemy.moveCount < 0) {
					scariness += 1;
				}
			}

			break;
		case SCOUT:
			scariness += 1;
			break;
		case SOLDIER:
			scariness += 3;
			break;
		case TANK:
			scariness += 5;
			break;
		default:
			System.out.println("Missing type in scaryFactor()");
			break;
		}

		if (enemy.attackCount < 1) {
			scariness += 1;
		}

		return scariness;
	}
	
	public void moveRandom() {
		moveRandom(rc.getType().strideRadius);
	}

	public void moveRandom(float dist) {
		if (rc.hasMoved()) {
			return;
		}

		Direction[] dirs = Util.shuffle(DIRECTIONS);
		for (Direction dir : dirs) {
			if (tryMove(dir, dist)) {
				break;
			}
		}
	}

	public Direction randomDirection() {
		return new Direction(Util.random(0.0f, 1.0f), Util.random(0.0f, 1.0f));
	}
	
	boolean bugging;
	Direction bugDirection;
	int bugFail;
	
	public void bug(MapLocation target) {
		if (rc.hasMoved()) {
			return;
		}
		
		if (bugging) {
			if (!tryMove(bugDirection)) {
				bugFail++;
				if (bugFail > 3) {
					bugFail = 0;
					bugging = false;
				}
			}
			
		} else if (!tryMoveTo(target)) {
			bugging = true;
		
			float offset = 0.0f;
			while (offset < 180.0f) {
				bugDirection = rc.getLocation().directionTo(target).rotateRightDegrees(offset);
				if (rc.canMove(bugDirection)) {
					break;
				}
				offset += 10.0f;
			}
			tryHardMove(bugDirection);		
		}
		
	}
}
