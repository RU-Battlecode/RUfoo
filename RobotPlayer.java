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
				logic = new ArchonLogic(rc);
				break;
			case SCOUT:
				logic = new ScoutLogic(rc);
				break;
			case SOLDIER:
				logic = new SoldierLogic(rc);
				break;
			case GARDENER:
				logic = new GardenerLogic(rc);
				break;
			case LUMBERJACK:
				logic = new LumberjackLogic(rc);
				break;
			case TANK:
				logic = new TankLogic(rc);
				break;
			default:
				System.out.println("Missing logic");
			}

			logic.run();
	  }
	  
}
