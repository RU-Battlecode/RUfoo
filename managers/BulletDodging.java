package RUfoo.managers;

import java.util.HashMap;
import java.util.Map;

import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * BulletDodging.java - 
 * 
 * @author Ben
 * @version Jan 29, 2017
 */
public class BulletDodging {

	private static final float ROUNDING = 10.0f; // tenth decimal place
	
	private RobotController rc;
	private Nav nav;
	
	public BulletDodging(RobotController rc, Nav nav) {
		this.rc = rc;
		this.nav = nav;
	}
	
	public void dodge(BulletInfo[] bullets) {
		if (bullets.length == 0) {
			return;
		}
		
		Map<MapLocation, Integer> mapLocations = new HashMap<>();
		MapLocation myLoc = rc.getLocation();		
		
		for (BulletInfo bullet : bullets) {
			Direction dirToMe = bullet.getLocation().directionTo(myLoc);
			float distToMe = bullet.getLocation().distanceTo(myLoc);
			
			if (Math.abs(dirToMe.degreesBetween(bullet.dir)) >= 20 || distToMe <= 1.5f) {
				continue;
			}
			
			float x = bullet.getLocation().x;
			float y = bullet.getLocation().y;
			
			MapLocation endPoint = bullet.getLocation().add(dirToMe, distToMe + rc.getType().strideRadius);
			
			// All bullets start with this amount of scariness.
			int scariness = 1; // does not matter what number
			
			while (x < endPoint.x) {
				
				// If this current bullet projection location is within the stride of our robot
				// then we need to consider its scariness.
				if (distance(myLoc.x, myLoc.y, x, y) <= rc.getType().strideRadius) {
					MapLocation moveAble = normalize(x, y);
					if (mapLocations.containsKey(moveAble)) {
						mapLocations.put(moveAble, mapLocations.get(moveAble) + scariness);
					} else {
						mapLocations.put(moveAble, scariness);
					}
				}
				
				// Each step the bullet takes it increases priority
				scariness++;
				x += bullet.getDir().getDeltaX(bullet.getSpeed());
				y += bullet.getDir().getDeltaY(bullet.getSpeed());
			}
		}
		
		if (mapLocations.size() != 0) {
			
			MapLocation moveLocation = null;
			int min = 9999;
			for (Map.Entry<MapLocation, Integer> entry : mapLocations.entrySet()) {
				if (moveLocation == null || (entry.getValue() < min && rc.canMove(moveLocation))) {
					min = entry.getValue();
					moveLocation = entry.getKey();
				}
			}
			
			if (moveLocation != null) {
				nav.tryMoveTo(moveLocation);
			}
		}
		
		
		
	}
	
	float distance(float x1, float y1, float x2, float y2) {
		return (float)Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2)); 
	}
	
	MapLocation normalize(float x, float y) {
		return new MapLocation(round(x), round(y));
	}
	
	MapLocation normalize(MapLocation x) {
		return normalize(x.x, x.y);
	}
	
	float round(float x) {
		return Math.round(x * ROUNDING) / ROUNDING;
	}
}
