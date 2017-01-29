package RUfoo.logic;

import RUfoo.managers.Census;
import RUfoo.managers.Combat;
import RUfoo.managers.Nav;
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
	protected Nav nav;
	protected Combat combat;
	protected Personality personality;
	protected Radio radio;
	protected Census census;
	protected boolean active;

	public RobotLogic(RobotController _rc) {
		rc = _rc;
		nav = new Nav(rc);
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
			if (rc.getRoundNum() == rc.getRoundLimit() - 1 || opponentAboutToWin()) {
				// End game. Just donate all.
				rc.donate(rc.getTeamBullets());
			}
			// If we can win... win.
			else if (rc.getTeamVictoryPoints()
					+ (int) (rc.getTeamBullets() / rc.getVictoryPointCost()) >= GameConstants.VICTORY_POINTS_TO_WIN) {

				rc.donate(rc.getTeamBullets());

			} else if (rc.getTeamBullets() > DONATE_AFTER) {
				rc.donate(rc.getTeamBullets() * DONATE_PERCENTAGE);
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	boolean opponentAboutToWin() {
		return rc.getOpponentVictoryPoints() > 900 && rc.getTeamVictoryPoints() > 800;
	}
}
