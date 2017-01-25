package RUfoo.logic;

import java.util.ArrayList;
import java.util.List;

import RUfoo.util.Util;
import battlecode.common.BulletInfo;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class TankLogic extends RobotLogic {

	private static final int MAX_AREAS = 15;
	
	// Tanks can be more patient because they do body damage to trees.
	private static final int PATIENCE_MULTIPLIER = 10;

	private int moveIndex;
	private int moveFrustration;
	private float prevousDistanceToTarget;
	private List<MapLocation> moveAreas;

	public TankLogic(RobotController _rc) {
		super(_rc);
		moveIndex = 0;
		moveFrustration = 0;
		moveAreas = new ArrayList<>();

		for (MapLocation loc : rc.getInitialArchonLocations(rc.getTeam().opponent())) {
			moveAreas.add(loc);
		}

	}

	@Override
	public void logic() {
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		BulletInfo[] bullets = rc.senseNearbyBullets();

		lookForEnemyArchons(enemies);

		RobotInfo target = combat.findTarget(enemies);

		if (target != null) {
			// Attack target aggressively!
			MapLocation closeToTarget = target.location.add(target.location.directionTo(rc.getLocation()),
					rc.getType().bodyRadius * 2.0f);
			nav.moveAggressivelyTo(closeToTarget, bullets, enemies);
			combat.shoot(target, enemies);
		} else {
			// No target.

			// Dodge any bullets
			nav.dodge(bullets);

			checkRadioForArchons();

			if (moveAreas.size() > 0) {
				move(enemies);
			}
		}

	}

	void lookForEnemyArchons(RobotInfo[] enemies) {
		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.ARCHON) {
				radio.foundEnemyArchon(enemy);
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

	void move(RobotInfo[] enemies) {
		MapLocation loc = moveAreas.get(moveIndex % moveAreas.size());
		float distToTarget = rc.getLocation().distanceSquaredTo(loc);

		if (rc.getLocation().distanceTo(loc) < 2.0f && enemies.length == 0
				|| moveFrustration > personality.getPatience() * PATIENCE_MULTIPLIER) {
			moveIndex++;
			moveFrustration = 0;
		}

		nav.tryHardMove(rc.getLocation().directionTo(loc));

		if (Util.equals(distToTarget, prevousDistanceToTarget, rc.getType().strideRadius / 2)) {
			moveFrustration++;
		}

		prevousDistanceToTarget = distToTarget;
	}
}