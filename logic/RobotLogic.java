package RUfoo.logic;

import RUfoo.managers.Navigation;
import battlecode.common.Clock;
import battlecode.common.RobotController;

public abstract class RobotLogic {
    protected RobotController rc;
	protected Navigation navManager;
    protected boolean active;
     
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

    public void setRc(RobotController _rc) {
        rc = _rc;
        navManager = new Navigation(_rc);
    }
}
