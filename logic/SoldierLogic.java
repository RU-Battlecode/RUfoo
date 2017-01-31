package RUfoo.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import RUfoo.model.DefenseInfo;
import RUfoo.util.Util;
import battlecode.common.BodyInfo;
import battlecode.common.BulletInfo;
import battlecode.common.Clock;
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
	private boolean hasRespondedToDefense;
	private RobotInfo prevousTarget;

	public SoldierLogic(RobotController _rc) {
		super(_rc);
		moveIndex = 0;
		moveFrustration = 0;
		moveAreas = new ArrayList<>();
		prevousTarget = null;

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

		if (rc.getRoundNum() == 600) {
			for (MapLocation loc : rc.getInitialArchonLocations(rc.getTeam())) {
				addNewMoveArea(loc);
			}
		}
		
		if (personality.age() == 300) {
			for (MapLocation loc : rc.getInitialArchonLocations(rc.getTeam())) {
				moveAreas.add(loc);
			}
		}

		lookForEnemyArchons(enemies);

		RobotInfo target = combat.findTarget(enemies, friends, myTrees, neutralTrees);
		if (target != null) {
			// Attack target aggressively!
			MapLocation closeToTarget = target.location.add(target.location.directionTo(rc.getLocation()),
					rc.getType().bodyRadius * 2.0f);

			if (shouldKite(target, friends)) {
				nav.kite(target, bullets);
			} else {
				nav.moveAggressivelyTo(closeToTarget, bullets, enemies);
			}

			combat.shoot(target, enemies);
		} else {
			// No target.

			if (prevousTarget != null) {
				combat.shoot(prevousTarget, enemies);
				prevousTarget = null;
			}

			if (rc.getRoundNum() % 2 == 0 && !hasRespondedToDefense) {
				respondToDefenseCalls();
				hasRespondedToDefense = true;
			}

			// Dodge any bullets
			nav.dodge(bullets);

			checkRadioForArchons();

			if (moveAreas.size() > 0) {
				move(enemies, trees, friends);
			} else {
				for (MapLocation loc : rc.getInitialArchonLocations(rc.getTeam())) {
					moveAreas.add(loc);
				}
				nav.moveRandom();
			}
		}

		nav.shakeTrees(trees);
		prevousTarget = target;
		nav.moveRandom();
		
		if (Clock.getBytecodesLeft() > 10_000) {
			logic();
		}
	}

	boolean shouldKite(RobotInfo target, RobotInfo[] friends) {
		return target.type == RobotType.LUMBERJACK
				|| ((target.getType().canAttack() && target.getType() != RobotType.SCOUT && target.health > 20)
						&& friends.length <= 5);
	}

	void respondToDefenseCalls() {
		List<DefenseInfo> defenseNeeds = radio.readLocationsThatNeedDefense();

		if (defenseNeeds.size() > 0) {
			DefenseInfo mostNeedy = Collections.max(defenseNeeds);
			// Try to add the location to our moveAreas with interrupt
			if (addNewMoveArea(mostNeedy.getLocation(), true)) {
				// Let everyone know we got it!
				radio.respondToDefenseCall(mostNeedy, 1);
			}
		}
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
				addNewMoveArea(gardenerLoc, true);
			}
		}
	}

	boolean addNewMoveArea(MapLocation location) {
		return addNewMoveArea(location, false);
	}

	boolean addNewMoveArea(MapLocation location, boolean interrupt) {
		boolean isNew = true;
		if (!moveAreas.contains(location)) {
			for (MapLocation loc : moveAreas) {
				if (loc.distanceSquaredTo(location) < 2.0f) {
					isNew = false;
				}
			}
		}

		if (isNew || interrupt) {
			moveAreas.add(location);
			if (moveAreas.size() > MAX_AREAS) {
				moveAreas.remove(0);
			}
		}

		if (interrupt) {
			nav.isBugging = false;
			// moveIndex = moveAreas.size() - 1;
		}

		return isNew || interrupt;
	}

	void move(RobotInfo[] enemies, TreeInfo[] trees, RobotInfo[] friends) {
		MapLocation loc = moveAreas.get(moveIndex % moveAreas.size());
		float distToTarget = rc.getLocation().distanceSquaredTo(loc);
		BodyInfo[] obstacles = Util.addAll(friends, trees);

		// for (MapLocation test : moveAreas) {
		// rc.setIndicatorDot(test, rc.getTeam() == Team.A ? 200 : 1, 1,
		// rc.getTeam() == Team.A ? 1 : 200);
		// }
		// rc.setIndicatorLine(rc.getLocation(), loc, rc.getTeam() == Team.A ?
		// 200 : 1, 1, rc.getTeam() == Team.A ? 1 : 200);

		if (rc.getLocation().distanceTo(loc) <= rc.getType().sensorRadius && enemies.length == 0) {
			hasRespondedToDefense = false;
			nav.isBugging = false;
			moveIndex++;
		}

		if (moveFrustration > personality.getPatience() * 3) {
			nav.isBugging = false;
			moveIndex++;
			moveFrustration = 0;
		}

		if (obstacles.length > 1) {
			Arrays.sort(obstacles, (b1, b2) -> {
				return Math.round(b1.getLocation().distanceSquaredTo(rc.getLocation())
						- b2.getLocation().distanceSquaredTo(rc.getLocation()));
			});
		}

		nav.isBugging = false;
		nav.bug(loc, obstacles);

		if (Util.equals(distToTarget, prevousDistanceToTarget, 0.0001f)) {
			moveFrustration += 3;
			nav.isBugging = false;
		}

		prevousDistanceToTarget = distToTarget;
	}
}
