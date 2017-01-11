package RUfoo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import battlecode.common.MapLocation;

public final class Util {
	private static final Random rand = new Random();
	
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
		float size = Math.max(1,  locations.size());
		return new MapLocation(x / size,
							   y / size);
	}
	
	/**
	 * Random number in range [min, max]
	 * @param min
	 * @param max
	 * @return
	 */
	public static int random(int min, int max) {
		return rand.nextInt(max - min + 1) + min;
	}
}
