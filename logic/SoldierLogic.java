package RUfoo.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import RUfoo.util.Util;
import battlecode.common.BodyInfo;
import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public class SoldierLogic extends RobotLogic {

	private static final int MAX_AREAS = 15;

	private int moveIndex;
	private int moveFrustration;
	private float prevousDistanceToTarget;
	private List<MapLocation> moveAreas;

	public SoldierLogic(RobotController _rc) {
		super(_rc);
		moveIndex = 0;
		moveFrustration = 0;
		moveAreas = new ArrayList<>();

		// find nearest gardener.
		if (personality.getMother() != null) {
			Direction awayFromMom = rc.getLocation().directionTo(personality.getMother().location).opposite();
			moveAreas.add(rc.getLocation().add(awayFromMom, 2.0f));
		}

		for (MapLocation loc : rc.getInitialArchonLocations(rc.getTeam().opponent())) {
			moveAreas.add(loc);
		}
	}

	@Override
	public void logic() {
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
		BulletInfo[] bullets = rc.senseNearbyBullets();
		TreeInfo[] trees = rc.senseNearbyTrees();
		TreeInfo[] myTrees = rc.senseNearbyTrees(rc.getType().sensorRadius, rc.getTeam());
		TreeInfo[] neutralTrees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
		
		lookForEnemyArchons(enemies);

		RobotInfo target = combat.findTarget(enemies, friends, myTrees, neutralTrees);

		if (target != null) {
			// Attack target aggressively!
			MapLocation closeToTarget = target.location.add(target.location.directionTo(rc.getLocation()),
					rc.getType().bodyRadius * 2.0f);
			if ((target.getType().canAttack() && target.getType() != RobotType.SCOUT) && friends.length <= 2) {
				nav.kite(target, bullets);
			} else {
				nav.moveAggressivelyTo(closeToTarget, bullets, enemies);
			}
			combat.shoot(target, enemies);
		} else {
			// No target.

			// Dodge any bullets
			nav.dodge(bullets);

			checkRadioForArchons();

			if (moveAreas.size() > 0) {
				move(enemies, trees, friends);
			} else {
				moveAreas.add(rc.getInitialArchonLocations(rc.getTeam())[0]);
				for (MapLocation loc : rc.getInitialArchonLocations(rc.getTeam().opponent())) {
					moveAreas.add(loc);
				}
				nav.moveRandom();
			}
		}

		nav.shakeTrees(trees);
	}

	void lookForEnemyArchons(RobotInfo[] enemies) {
		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.ARCHON) {
				radio.foundEnemyArchon(enemy);
			} else if (enemy.type == RobotType.GARDENER) {
				radio.foundEnemyGardener(enemy);
			}
		}
	}

	private void checkRadioForArchons() {
		MapLocation[] possibleArchonLocations = radio.readEnemyArchonChannel();
		for (MapLocation archonLoc : possibleArchonLocations) {
			if (archonLoc != null) {
				addNewMoveArea(archonLoc);
			}
		}
		
		List<MapLocation> possibleGardenerLocs = radio.readEnemyGardenerLocations();
		for (MapLocation gardenerLoc : possibleGardenerLocs) {
			if (gardenerLoc != null) {
				addNewMoveArea(gardenerLoc);
			}
		}

	}

	void addNewMoveArea(MapLocation location) {
		boolean isNew = true;
		for (MapLocation loc : moveAreas) {
			if (loc.distanceSquaredTo(location) < 2.0f) {
				isNew = false;
			}
		}

		if (isNew) {
			moveAreas.add(location);
			if (moveAreas.size() > MAX_AREAS) {
				moveAreas.remove(0);
			}
		}
	}
	
	void move(RobotInfo[] enemies, TreeInfo[] trees, RobotInfo[] friends) {
		MapLocation loc = moveAreas.get(moveIndex % moveAreas.size());
		float distToTarget = rc.getLocation().distanceSquaredTo(loc);
		BodyInfo[] obstacles = Util.addAll(friends, trees);
		
		if (rc.getLocation().distanceTo(loc) < 2.0f && enemies.length == 0) {
			if (!nav.closeToArchonLocation(loc) && moveAreas.size() > 1) {
				moveAreas.remove(moveIndex % moveAreas.size());
				moveIndex++;
			}
			moveFrustration++;
		}
		
		if (moveFrustration > personality.getPatience()) {
			moveIndex++;
			moveFrustration = 0;
		}

		Arrays.sort( obstacles, (b1, b2) -> {
			return Math.round(b1.getLocation().distanceSquaredTo(rc.getLocation())
							- b2.getLocation().distanceSquaredTo(rc.getLocation()));
		}); 

		nav.bug(loc, obstacles);
			
		if (Util.equals(distToTarget, prevousDistanceToTarget, rc.getType().strideRadius - 0.1f)) {
			moveFrustration++;
		}

		prevousDistanceToTarget = distToTarget;
	}
}
