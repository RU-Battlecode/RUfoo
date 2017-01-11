package RUfoo.logic;

import RUfoo.managers.Combat;
import RUfoo.managers.Navigation;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.TreeInfo;

public abstract class RobotLogic {
    protected RobotController rc;
	protected Navigation navManager;
	protected Combat combatManager;
    protected boolean active;
     
    public RobotLogic(RobotController _rc) {
    	rc = _rc;
    	navManager = new Navigation(_rc);
        combatManager = new Combat(_rc);
    }
    
    /**
     * Called on robot creation in RobotPlayer.java.
     * Starts the robots active logic loop, that pauses the thead on completion.
     */
    public void run() {
    	active = true;
    	while (active) {
    		logic();
    		Clock.yield();
    	}
    }
    
    /**
     * This method should be overridden with the unique logic for each bot type.
     */
    public abstract void logic();

}
