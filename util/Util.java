package RUfoo.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import battlecode.common.BodyInfo;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;

public final class Util {
	private static final Random rand = new Random(1337);

	private Util() {}

	public static <T> T randomChoice(T[] array) {
		int index = rand.nextInt(array.length);
		return array[index];
	}

	public static <T> T[] shuffle(T[] array) {
		Collections.shuffle(Arrays.asList(array));
		return array;
	}

	public static BodyInfo[] addAll(BodyInfo[] first, BodyInfo[] second) {
		BodyInfo[] result = new BodyInfo[first.length + second.length];

		for (int i = 0; i < first.length; i++) {
			result[i] = first[i];
		}

		for (int i = 0; i < second.length; i++) {
			result[i + first.length] = second[i];
		}

		return result;
	}

	public static MapLocation average(List<MapLocation> locations) {
		float x = 0, y = 0;
		for (MapLocation loc : locations) {
			x += loc.x;
			y += loc.y;
		}
		float size = Math.max(1, locations.size());
		return new MapLocation(x / size, y / size);
	}

	public static boolean contains(RobotInfo[] robots, RobotType type) {
		for (RobotInfo r : robots) {
			if (r.getType() == type) {
				return true;
			}
		}
		return false;
	}
	
	public static RobotInfo findType(RobotInfo[] robots, RobotType type) {
		for (RobotInfo r : robots) {
			if (r.getType() == type) {
				return r;
			}
		}
		return null;
	}

	/**
	 * Random number in range [min, max]
	 * 
	 * @param min
	 * @param max
	 * @return
	 */
	public static int random(int min, int max) {
		return rand.nextInt(max - min + 1) + min;
	}

	public static float random(float min, float max) {
		return rand.nextFloat() * (max - min) + min;
	}

	public static boolean equals(float a, float b, float e) {
		return Math.abs(a - b) <= e;
	}

	public static boolean closeEnough(Direction dir1, Direction dir2, float degreesOk) {
		if (dir1 == null || dir2 == null) {
			return false;
		}
		
		return Math.abs(dir1.degreesBetween(dir2)) <= degreesOk;
	}

	/**
	 * Find the closest maplocation to the line segment formed by p1 and p2 from
	 * p3.
	 * 
	 * @param p1
	 *            line segment start
	 * @param p2
	 *            line segment end
	 * @param p3
	 *            point to find min distance to segment
	 * @return MapLocation of the closest point on the line segment
	 */
	public static MapLocation distanceToSegment(MapLocation p1, MapLocation p2, MapLocation p3) {

		float xDelta = p2.x - p1.x;
		float yDelta = p2.y - p1.y;

		float u = ((p3.x - p1.x) * xDelta + (p3.y - p1.y) * yDelta) / (xDelta * xDelta + yDelta * yDelta);

		MapLocation closestPoint = null;
		if (u < 0) {
			closestPoint = p1;
		} else if (u > 1) {
			closestPoint = p2;
		} else {
			closestPoint = new MapLocation(p1.x + u * xDelta, p1.y + u * yDelta);
		}
		
		return closestPoint;
	}
	
	public static MapLocation midPoint(MapLocation a, MapLocation b) {
		return new MapLocation((a.x + b.x) / 2, (a.y + b.y) / 2);
	}
}
