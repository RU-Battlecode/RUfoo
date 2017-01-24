package RUfoo.logic;

import RUfoo.managers.Census;
import RUfoo.managers.Combat;
import RUfoo.managers.Navigation;
import RUfoo.managers.Personality;
import RUfoo.managers.Radio;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
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

	private static final float DONATE_AFTER = 500; // bullets
	private static final float DONATE_PERCENTAGE = 0.10f;

	protected RobotController rc;
	protected Navigation nav;
	protected Combat combat;
	protected Personality personality;
	protected Radio radio;
	protected Census census;
	protected boolean active;

	public RobotLogic(RobotController _rc) {
		rc = _rc;
		nav = new Navigation(rc);
		combat = new Combat(rc);
		personality = new Personality(rc);
		radio = new Radio(rc);
		census = new Census(rc, radio);
	}

	/**
	 * Called on robot creation in RobotPlayer.java. Starts the robots active
	 * logic loop, that pauses the thread on completion.
	 */
	public void run() {
		active = true;
		while (active) {
			census.tryTakeCensus();
			
			donateToWin();

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

	void donateToWin() {
		try {
			if (rc.getRoundNum() == rc.getRoundLimit() - 1) {
				// End game. Just donate all.
				rc.donate(rc.getTeamBullets());
			}
			// If we can win... win.
			else if (rc.getTeamVictoryPoints()
					+ (int) (rc.getTeamBullets() / 10) >= GameConstants.VICTORY_POINTS_TO_WIN) {

				rc.donate(rc.getTeamBullets());

			} else if (rc.getTeamBullets() > DONATE_AFTER) {
				rc.donate(rc.getTeamBullets() * DONATE_PERCENTAGE);
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

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
