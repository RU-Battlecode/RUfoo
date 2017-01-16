package RUfoo.logic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

/*
 * 
 * // 1. Shoot any enemies.
 * 
 * // 2. Explore in single direction 
 * 
 * // 3. Return to base when:
 * 			- reached end of map
 *          - found group of enemies 
 *          - health is too low to be out
 *          
 * // 4. count trees! and Radio to the Archon for strategy
 */

public class ScoutLogic extends RobotLogic {

	private static final float LOW_HEALTH_PERCENT = 0.25f;

	private Direction exploreDir;
	private Map<MapLocation, Integer> treeMap;

	public ScoutLogic(RobotController _rc) {
		super(_rc);
		exploreDir = null;		
		treeMap = new HashMap<>();
	}

	@Override
	public void logic() {	
		RobotInfo target = combat.findTarget();
		//nav.dodgeBullets();
		attack(target);

		if (target == null && !rc.hasAttacked()) {
			TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
			if (personality.getIsLeftHanded() || trees.length < 5) {
				moveToNewTrees(trees);
			}
			explore();
		}
		// countTrees();
	}

	void attack(RobotInfo target) {
		if (target != null) {
			if (nav.isDirectionSafe(rc.getLocation().directionTo(target.location))) {
				nav.moveAggressively(target.location);
			}
			
			combat.singleShotAttack(target);
		}
	}

	void explore() {
		if (exploreDir == null || isHome()) {
			exploreDir = nav.randomDirection();
		}

		boolean shouldExplore = shouldExplore();
		if (!shouldExplore) {
			exploreDir = rc.getLocation().directionTo(personality.getHome());
		}

		nav.moveBest(exploreDir);
	}

	private void moveToNewTrees(TreeInfo[] trees) {

		// Farthest trees first!
		Arrays.sort(trees, (t1, t2) -> {
			return Math.round(t2.getLocation().distanceSquaredTo(rc.getLocation()) 
							- t1.getLocation().distanceSquaredTo(rc.getLocation()));
		});
		
		for(TreeInfo tree : trees) {
			if (!treeMap.containsKey(tree.location)) {
				treeMap.put(tree.location, tree.ID);
				nav.moveToSafely(tree.location);
			}
		}
		
	}
	
	private void countTrees() {
		TreeInfo[] trees = rc.senseNearbyTrees();

		for (TreeInfo tree : trees) {
			if (tree.getTeam() == Team.NEUTRAL) {
				treeMap.put(tree.location, tree.ID);
			}
		}

	}

	boolean shouldExplore() {
		try {
			return rc.onTheMap(rc.getLocation().add(exploreDir, rc.getType().sensorRadius / 2))
					&& rc.getHealth() / rc.getType().maxHealth > LOW_HEALTH_PERCENT;
		} catch (GameActionException e) {
			e.printStackTrace();
			return true;
		}
	}

	boolean isHome() {
		return rc.getLocation().distanceTo(personality.getHome()) <= rc.getType().bodyRadius * 3;
	}
}
