package RUfoo.managers;

import java.util.LinkedList;
import java.util.Queue;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Construction {
	private Queue<MapLocation> plan;
	private int originX, originY;
	private RobotController rc;
	private boolean hasLoadedPlan;

	public Construction(RobotController _rc) {
		rc = _rc;
		plan = new LinkedList<>();
		hasLoadedPlan = false;
	}

	public MapLocation peekInstructionLocation() {
		return plan.peek();
	}

	public MapLocation popInstructionLocation() {
		return plan.poll();
	}

	public void loadPlan(String planString) {
		try {
			String[] planSplit = planString.split("\n");		
			String[] origin = planSplit[0].split(",");
			
			originX = Integer.parseInt(origin[0]);
			originY = Integer.parseInt(origin[1]);
			
			int row = 0;
			for (int i = 1; i < planSplit.length; i++) {
				int col = 0;
				for (String ch :  planSplit[i].split(" ")) {
					if (ch.charAt(0) != 'e') {
						
						plan.add(new MapLocation(rc.getLocation().x + rc.getType().strideRadius * (col + originX),
												 rc.getLocation().y + rc.getType().strideRadius * (row + originY)));
					}
					col++;
				}
				row++;
			}
			hasLoadedPlan = true;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}

	public boolean hasLoadedPlan() {
		return hasLoadedPlan;
	}
}
