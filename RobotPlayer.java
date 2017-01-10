package RUfoo;

import RUfoo.logic.*;
import RUfoo.logic.RobotLogic;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class RobotPlayer {
	
	  public static void run(RobotController rc) throws GameActionException {
			RobotLogic logic = null;
			switch (rc.getType()) {
			case ARCHON:
				logic = new ArchonLogic();
				break;
			case SCOUT:
				logic = new ScoutLogic();
				break;
			case SOLDIER:
				logic = new SoldierLogic();
				break;
			case GARDENER:
				logic = new GardenerLogic();
				break;
			case LUMBERJACK:
				logic = new LumberjackLogic();
				break;
			case TANK:
				logic = new TankLogic();
				break;
			default:
				System.out.println("Missing logic");
			}

			logic.setRc(rc);
			logic.run();
	  }
	  
}
