package RUfoo.managers;

import java.util.Arrays;
import java.util.Random;

import RUfoo.util.Util;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

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

	// Hopefully the robot that built this robot... but
	// things can go horribly wrong if two mothers get too
	// close. And try not to think about the Archon's mother... (null)
	private RobotInfo mother;
	
	public Personality(RobotController _rc) {
		rc = _rc;
		Random rand = new Random(_rc.getID());
		isLeftHanded = rand.nextInt(101) <= PERCENT_LEFT_HANDED;
		patience = Util.random(5, 15);
		birthday = rc.getRoundNum();
		mother = determinMother();
	}

	private RobotInfo determinMother() {
		RobotInfo mother = null;
		
		if (rc.getType() != RobotType.ARCHON) {
			RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().strideRadius, rc.getTeam());
			
			if (robots.length > 0) {
				Arrays.sort(robots, (r1, r2) -> {
					return Math.round(r1.location.distanceSquaredTo(rc.getLocation()) - r2.location.distanceSquaredTo(rc.getLocation()));
				});
				
				// Find the first Gardener/Archon
				for (RobotInfo r : robots) {
					if (r.getType().canBuild()) {
						mother = r;
						break;
					}
					
				}
			}
		}
		
		return mother;
	}

	public RobotInfo getMother() {
		return mother;
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
