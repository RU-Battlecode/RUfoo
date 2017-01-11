package RUfoo.managers;

import java.util.Random;

import RUfoo.Util;

/**
 * Personality.java - Robots with personality are way more fun.
 * 
 *
 */
public class Personality {

	// 10% of people are left handed (http://www.everydayhealth.com/healthy-living-pictures/little-known-facts-about-lefthanders.aspx)
	private static final int PERCENT_LEFT_HANDED = 10;
	
	private boolean isLeftHanded;
	
	// Number of rounds before the robot will give up a task.
	private int patience;
	
	public Personality(int seed) {
		Random rand = new Random(seed);
		isLeftHanded = rand.nextInt(101) <= PERCENT_LEFT_HANDED;
		patience = Util.random(5, 15);
	}
		
	public boolean getIsLeftHanded() {
		return isLeftHanded;
	}
	
	public int getPatience() {
		return patience;
	}
}
