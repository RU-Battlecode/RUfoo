package RUfoo.managers;

import java.util.ArrayList;
import java.util.List;

import RUfoo.Util;
import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

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
		Direction dir = Util.randomChoice(DIRECTIONS);
		try {
			if (rc.canMove(dir)) {
				rc.move(dir);
			} else {
				for (Direction direction : Util.shuffle(DIRECTIONS)) {
					if (rc.canMove(direction)) {
						rc.move(direction);
						break;
					}
				}
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}
	
	public void dodgeBullets() {
		if (rc.senseNearbyBullets().length > 0) {
			List<Direction> possibleDirs = safeDirections();
			if (possibleDirs.size() > 0) {
				try {
					rc.move(possibleDirs.get(0));
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
		} else {
			moveRandom();
		}
	}
	
	public List<Direction> safeDirections() {
		List<Direction> safeDirections = new ArrayList<>();
		
		BulletInfo[] bullets = rc.senseNearbyBullets();
		// TODO: Check where enemies are
		//RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		
		for (Direction dir : DIRECTIONS) {
			if (!rc.canMove(dir)) {
				continue;
			}
			
			MapLocation possibleLocation = rc.getLocation().add(dir);
			
			for (BulletInfo bullet : bullets) {
				MapLocation futureBulletLoc = bullet.getLocation().add(bullet.getDir(), bullet.getSpeed()); 
				if (!futureBulletLoc.isWithinDistance(possibleLocation, rc.getType().bodyRadius)) {
					safeDirections.add(dir);
				}
			}
		}
		
		return safeDirections;
	}
}
