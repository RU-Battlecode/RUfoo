package RUfoo.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import RUfoo.Util;
import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Navigation {

	private RobotController rc;

	public static final Direction NORTH_EAST = Direction.getNorth().rotateRightDegrees(45);
	public static final Direction SOUTH_EAST = Direction.getEast().rotateRightDegrees(45);
	public static final Direction SOUTH_WEST = Direction.getSouth().rotateRightDegrees(45);
	public static final Direction NORTH_WEST = Direction.getWest().rotateRightDegrees(45);

	public static final Direction[] DIRECTIONS = { Direction.getNorth(), NORTH_EAST, Direction.getEast(), SOUTH_EAST,
			Direction.getSouth(), SOUTH_WEST, Direction.getWest(), NORTH_WEST };

	public Navigation(RobotController _rc) {
		rc = _rc;
	}

	public void moveRandom() {
		moveRandom(rc.getType().strideRadius);
	}
	
	public void moveRandom(float dist) {
		if (rc.hasMoved()) {
			return;
		}
		
		Direction dir = Util.randomChoice(DIRECTIONS);
		try {
			if (rc.canMove(dir, dist)) {
				rc.move(dir, dist);
			} else {
				for (Direction direction : Util.shuffle(DIRECTIONS)) {
					if (rc.canMove(direction, dist)) {
						rc.move(direction, dist);
						break;
					}
				}
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public void dodgeBullets() {
		if (rc.hasMoved()) {
			return;
		}
		if (rc.senseNearbyBullets().length > 0) {
			List<Direction> possibleDirs = safeDirections();
			if (possibleDirs.size() > 0) {
				try {
					rc.move(possibleDirs.get(0));
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public List<Direction> safeDirections() {
		List<Direction> safeDirections = new ArrayList<>();

		// Check all the general directions
		for (Direction dir : DIRECTIONS) {
			if (!rc.canMove(dir)) {
				continue;
			}

			if (isDirectionSafe(dir)) {
				safeDirections.add(dir);
			}
		}

		return safeDirections;
	}

	public boolean isDirectionSafe(Direction dir) {
		boolean safeSoFar = true;

		// Find all the bullets near me.
		BulletInfo[] bullets = rc.senseNearbyBullets();

		// TODO: Check where enemies are
		// RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius,
		// rc.getTeam().opponent());

		// This is where I want to be
		MapLocation possibleLocation = rc.getLocation().add(dir);

		for (BulletInfo bullet : bullets) {
			// This is where the bullet will be!
			MapLocation futureBulletLoc = bullet.getLocation().add(bullet.getDir(), bullet.getSpeed());
			// Check if that bullet is in our body radius of our futureLocation.
			if (futureBulletLoc.isWithinDistance(possibleLocation, rc.getType().bodyRadius)) {
				safeSoFar = false;
				break;
			}
		}

		return safeSoFar;
	}

	public void moveToSafely(MapLocation target) {
		moveTo(target, true);
	}

	public void moveAggressively(MapLocation target) {
		moveTo(target, false);
	}
	
	void moveTo(MapLocation target, boolean prioritizeSafety) {
		
		float distToTarget = rc.getLocation().distanceTo(target);
		
		// Already there? or can't move?
		if (distToTarget < GameConstants.GENERAL_SPAWN_OFFSET || rc.hasMoved()) {
			return;
		}
		
		float dist = Math.max(0, Math.min(rc.getType().strideRadius, distToTarget));

		// The direct direction to the target location.
		Direction direct = rc.getLocation().directionTo(target);

		// Find the safest directions that is closest to the direction we want
		// to move.
		Direction best = Collections.min(safeDirections(), (dir1, dir2) -> {
			return Math.round(dir1.degreesBetween(direct) - dir2.degreesBetween(direct));
		});

		
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
				} else {
					moveRandom(dist);
				}
			} else if (rc.canMove(direct, dist)) {
				// There was no best route, just move directly toward target
				rc.move(direct, dist);
			} else {
				moveRandom(dist);
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}
	
	public void moveByTrees(boolean includeFriendly) {
		if (rc.hasMoved()) {
			return;
		}
		
		TreeInfo[] trees = rc.senseNearbyTrees();
		// Sort furthest first
		Arrays.sort(trees, (t1, t2) -> {
			return Math.round(t1.location.distanceSquaredTo(rc.getLocation()) - t2.location.distanceSquaredTo(rc.getLocation()));
		});
		
		for (TreeInfo tree : trees) {	
			if (rc.getTeam() != tree.getTeam() || includeFriendly) {
				moveAggressively(tree.location);
			}
		}

		if (!rc.hasMoved()) {
			moveRandom();
		}
	}
}
