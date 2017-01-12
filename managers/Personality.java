package RUfoo.managers;

import java.util.Random;

import RUfoo.util.Util;
import battlecode.common.RobotController;

/**
 * Personality.java - Robots with personality are way more fun!
 */
public class Personality {

	// 10% of people are left handed
	// (http://www.everydayhealth.com/healthy-living-pictures/little-known-facts-about-lefthanders.aspx)
	private static final int PERCENT_LEFT_HANDED = 10;

	private RobotController rc;

	private boolean isLeftHanded;

	// Number of rounds before the robot will give up a task.
	private int patience;

	// Round that the robot was created
	private int birthday;

	public Personality(RobotController _rc) {
		rc = _rc;
		Random rand = new Random(_rc.getID());
		isLeftHanded = rand.nextInt(101) <= PERCENT_LEFT_HANDED;
		patience = Util.random(5, 15);
		birthday = rc.getRoundNum();
	}

	public int age() {
		return rc.getRoundNum() - birthday;
	}

	public boolean getIsLeftHanded() {
		return isLeftHanded;
	}

	public int getPatience() {
		return patience;
	}
}
