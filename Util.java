package RUfoo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

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
}
