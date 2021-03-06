package RUfoo.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import RUfoo.model.Circle;
import RUfoo.util.Util;
import battlecode.common.BodyInfo;
import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
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

	public boolean tryHardMove(Direction dir) {
		return tryHardMove(dir, rc.getType().strideRadius);
	}

	public boolean tryHardMove(Direction dir, float dist) {
		return tryHardMove(dir, dist, 180.0f);
	}

	public boolean tryHardMove(Direction dir, float dist, float maxDegreesOff) {
		if (rc.hasMoved() || dist <= 0.1f || dir == null) {
			return false;
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
				offset += 10.0f;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}

		if (rc.hasMoved()) {
			return true;
		} else {
			return tryHardMove(dir, dist / 2, maxDegreesOff);
		}
	}

	public boolean tryHardMoveClosestTo(Direction dir, float dist, float maxDegreesOff, Direction targetDir) {
		if (rc.hasMoved() || dist <= 0.1f) {
			return false;
		}

		boolean isLeft = Math.abs(dir.rotateRightDegrees(0.1f).degreesBetween(targetDir)) > Math.abs(dir.rotateLeftDegrees(0.1f)
				.degreesBetween(targetDir));
		
		int sign = isLeft ? -1 : 1;
		try {
			float offset = 0.0f;
			while (offset < maxDegreesOff) {

				if (rc.canMove(dir.rotateRightDegrees(sign * offset), dist)) {
					rc.move(dir.rotateRightDegrees(sign * offset), dist);
					//rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(dir, 20), 10, 200, 200);
					break;
				}

				offset += 5.0f;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}

		if (rc.hasMoved()) {
			return true;
		} else {
			return tryHardMoveClosestTo(dir, dist / 2, maxDegreesOff, targetDir);
		}
	}
	
	public void moveByTrees(TreeInfo[] trees) {
		if (rc.hasMoved()) {
			return;
		}
		
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
			if (futureBulletLoc.isWithinDistance(targetPosition, rc.getType().bodyRadius + 0.01f)
					|| Util.closeEnough(bullet.getDir(), dir, 4.0f)) {
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
					tryHardMove(best, dist);
				} else if (Math.abs(degrees) <= 30 && rc.canMove(direct, dist)) {
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

	public boolean isLocationFree(MapLocation loc) {
		try {
			return !rc.isCircleOccupiedExceptByThisRobot(rc.getLocation(),
					GameConstants.BULLET_TREE_RADIUS + GameConstants.GENERAL_SPAWN_OFFSET);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean isLocationFree(Direction dir) {
		return isLocationFree(rc.getLocation().add(dir));
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

	public boolean isBugging;
	Direction bugDir;
	Direction lastDir;
	float bugDistance;

	public void bug(MapLocation target, BodyInfo[] bodies) {
		bug(target, bodies, false);
	}
	
	public void bug(MapLocation target, BodyInfo[] bodies, boolean addNormal) {
		MapLocation location = rc.getLocation();
		float totalDist = location.distanceTo(target);

		// Already there or cannot move?
		if (rc.hasMoved() || totalDist <= 0.1f) {
			isBugging = false;
			return;
		}

		// Calculate the travel distance and direction.
		float dist = Math.min(totalDist, rc.getType().strideRadius);
		Direction dirToTarget = location.directionTo(target);

		// rc.setIndicatorLine(location, target, 100, 0, 1);
		//rc.setIndicatorLine(location, location.add(bugDir, 5), 0, 100, 1);

		if (!isBugging) {
			if (!tryMove(dirToTarget, dist)) {
				bugDistance = totalDist;
				bugDir = dirToTarget;
				isBugging = true;
				bug(target, bodies);
			}
		} else {
			// Bugging means we need to follow tree tangents unless we can move
			// in the stored bugDir.

			// Can we move straight to target?
			if (Util.closeEnough(bugDir, dirToTarget, 6.0f) && totalDist < bugDistance
					&& tryHardMove(dirToTarget, dist, 100.0f)) {
				// We were able to move to the target
				bugDistance = totalDist;
				lastDir = dirToTarget;
			} else if (handleEdgeOfMap()) {

			}
			// Try to follow body tangents.
			else if (handleBodies(bodies, dist, addNormal)) {
				
			}
			// No trees... move to target?
			else {
				//tryHardMoveClosestTo(dirToTarget, dist, 180.0f, bugDir);
				tryHardMoveClosestTo(dirToTarget, dist, 180.0f,  lastDir != null ? lastDir : bugDir);
				isBugging = false;
			}
		}

		if (rc.hasMoved()) {
			lastDir = location.directionTo(rc.getLocation());
		}
	}

	boolean handleBodies(BodyInfo[] bodies, float dist, boolean addNormal) {
		int bodiesCalculated = 0;
		Direction best = null;
		Direction direction = lastDir != null ? lastDir : bugDir;
		
		for (BodyInfo body : bodies) {
			Circle treeCircle = new Circle(body.getLocation(), body.getRadius());
			MapLocation midPoint = Util.midPoint(rc.getLocation(), body.getLocation());
			Circle tangetCircle = new Circle(midPoint, rc.getLocation().distanceTo(midPoint));

			MapLocation[] intersections = tangetCircle.intersections(treeCircle);

			Arrays.sort(intersections, (i1, i2) -> {
				return Math.round(rc.getLocation().directionTo(i1).degreesBetween(direction)
						- rc.getLocation().directionTo(i2).degreesBetween(direction));
			});

			for (MapLocation intersection : intersections) {
				Direction normal = body.getLocation().directionTo(intersection);
				if (addNormal) {
					intersection = intersection.add(normal, rc.getType().bodyRadius);
				}
				Direction dir = rc.getLocation().directionTo(intersection);
				if (best == null || Math.abs(dir.degreesBetween(direction)) < Math
						.abs(best.degreesBetween(direction))) {
					best = dir;
				}
			}

			bodiesCalculated++;
			if (bodiesCalculated > 4) {
				break;
			}
		}

		if (best != null) {
			//rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(best, 5), 200, 200, 200);
			tryHardMoveClosestTo(best, dist, 180.0f, direction);
		}

		return rc.hasMoved();
	}

	boolean handleEdgeOfMap() {

		try {
			if (rc.onTheMap(rc.getLocation(), rc.getType().sensorRadius - 0.1f)) {
				return false;
			}
		} catch (GameActionException e1) {
			e1.printStackTrace();
		}
		
		try {
			for (Direction dir : DIRECTIONS) {
				MapLocation checkLoc = rc.getLocation().add(dir, rc.getType().bodyRadius + rc.getType().strideRadius);

				if (!rc.onTheMap(checkLoc)) {

					Direction tangent = rc.getLocation().directionTo(checkLoc).rotateLeftDegrees(90);
					if (tryHardMoveClosestTo(tangent.opposite(), rc.getType().strideRadius, 180.0f, bugDir)) {
						break;
					}
				}
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}

		return rc.hasMoved();
	}

	public void shakeTrees() {
		shakeTrees(rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL));
	}

	public void shakeTrees(TreeInfo[] trees) {
		for (TreeInfo tree : trees) {
			if (tree.getTeam().equals(rc.getTeam().opponent()) || tree.containedBullets == 0) {
				continue;
			}

			if (rc.canShake(tree.ID)) {
				try {
					rc.shake(tree.ID);
					break;
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void kite(RobotInfo target, BulletInfo[] bullets) {
		if (rc.hasMoved()) {
			return;
		}
		float delta = rc.getLocation().distanceTo(target.location) - rc.getType().sensorRadius / 2;

		if (!Util.equals(delta, 0.0f, 0.01f)) {

			Direction dir = rc.getLocation().directionTo(target.location);

			if (delta < 0) {
				dir = dir.opposite();
			}

			boolean isSafeFromBullets = bullets.length == 0;
			for (BulletInfo bullet : bullets) {
				// This is where the bullet will be!
				MapLocation futureBulletLoc = bullet.getLocation().add(bullet.getDir(), bullet.getSpeed());
				// Check if that bullet is in our body radius of our
				// futureLocation.
				if (futureBulletLoc.isWithinDistance(rc.getLocation().add(delta, delta), rc.getType().bodyRadius)) {
					isSafeFromBullets = false;
				}
			}
			if (isSafeFromBullets) {
				tryHardMove(dir, delta);
			} else {
				dodge(bullets);
			}
		}
	}

	public boolean closeToArchonLocation(MapLocation target) {
		for (MapLocation loc : rc.getInitialArchonLocations(rc.getTeam().opponent())) {
			if (loc.distanceTo(target) < 0.1f) {
				return true;
			}
		}

		return false;
	}
}
