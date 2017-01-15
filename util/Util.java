package RUfoo.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public final class Util {
	private static final Random rand = new Random(1337);

	private Util() {}

	public static <T extends Object> T randomChoice(T[] array) {
		int index = rand.nextInt(array.length);
		return array[index];
	}

	public static <T extends Object> T[] shuffle(T[] array) {
		Collections.shuffle(Arrays.asList(array));
		return array;
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

	public static float random(float min, float max, int seed) {
		rand.setSeed(seed);
		return rand.nextFloat() * (max - min) + min;
	}

	public static boolean closeEnough(Direction dir1, Direction dir2, float degreesOk) {
		float degreesBetween = dir1.degreesBetween(dir2);

		return (degreesBetween >= 0 && degreesBetween <= degreesOk)
				|| (degreesBetween >= 360 - degreesOk && degreesBetween <= 360);
	}
}
