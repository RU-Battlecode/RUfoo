package RUfoo.logic;

import RUfoo.managers.Combat;
import RUfoo.managers.Navigation;
import RUfoo.managers.Personality;
import RUfoo.managers.Radio;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.TreeInfo;

/**
 * RobotLogic.java - Base abstract class to share common functionality between
 * robot types.
 * 
 * @author Ben
 *
 */
public abstract class RobotLogic {
	
	protected RobotController rc;
	protected Navigation nav;
	protected Combat combat;
	protected Personality personality;
	protected Radio radio;
	protected boolean active;

	public RobotLogic(RobotController _rc) {
		rc = _rc;
		nav = new Navigation(rc);
		combat = new Combat(rc);
		personality = new Personality(rc);
		radio = new Radio(rc);
	}

	/**
	 * Called on robot creation in RobotPlayer.java. Starts the robots active
	 * logic loop, that pauses the thread on completion.
	 */
	public void run() {
		active = true;
		while (active) {
			logic();

			if (Clock.getBytecodesLeft() > 200) {
				shakeTrees();
			}

			Clock.yield();
		}
	}

	/**
	 * This method should be overridden with the unique logic for each robot
	 * type.
	 */
	public abstract void logic();

	void shakeTrees() {
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius);
		for (TreeInfo tree : trees) {
			if (tree.getTeam().equals(rc.getTeam().opponent())) {
				continue;
			}

			if (tree.containedBullets > 0 && rc.canShake(tree.ID)) {
				try {
					rc.shake(tree.ID);
					break;
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
