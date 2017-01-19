package RUfoo.logic;

import RUfoo.managers.Combat;
import RUfoo.managers.Navigation;
import RUfoo.managers.Personality;
import RUfoo.managers.Radio;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.TreeInfo;

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
				shakeIt();
			}

			Clock.yield();
		}
	}

	void shakeIt() {
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

	/**
	 * This method should be overridden with the unique logic for each bot type.
	 */
	public abstract void logic();

}
