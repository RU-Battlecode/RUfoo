package RUfoo.logic;

import RUfoo.managers.Combat;
import RUfoo.managers.Construction;
import RUfoo.managers.Navigation;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.TreeInfo;

public abstract class RobotLogic {
    protected RobotController rc;
	protected Navigation navManager;
	protected Combat combatManager;
	protected Construction constructionManager;
    protected boolean active;
     
    public RobotLogic(RobotController _rc) {
    	rc = _rc;
    	navManager = new Navigation(_rc);
        combatManager = new Combat(_rc);
        constructionManager = new Construction(_rc);
    }
    
    /**
     * Called on robot creation in RobotPlayer.java.
     * Starts the robots active logic loop, that pauses the thread on completion.
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
					System.out.println("May have just shaken " + tree.containedBullets);
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
