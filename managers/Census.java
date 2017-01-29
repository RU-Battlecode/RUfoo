package RUfoo.managers;

import static RUfoo.managers.Channel.*;

import java.util.Arrays;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Census {

	private static final int[] CENSUS_DAYS = { 100, 200, 250, 300, 400, 500, 600, 700, 800, 1000, 1100, 1500, 2000, 2500 };

	private RobotController rc;
	private Radio radio;

	public Census(RobotController _rc, Radio _radio) {
		rc = _rc;
		radio = _radio;
	}

	public void increment(RobotType type) {
		Channel typeChannel = channelForType(type);
		int count = 1 + radio.readChannel(typeChannel);
		radio.broadcast(typeChannel, count);
	}

	public void tryTakeCensus() {
		if (isCensusDay(rc.getRoundNum())) {
			// Get the type channel
			Channel typeChannel = channelForType(rc.getType());
			int count = 1;
			if (firstOfType(rc.getType())) {
				// I was the first of this robot type to go!
				radio.broadcast(firstChannelForType(rc.getType()), 1);
			} else {
				count += radio.readChannel(typeChannel);
			}

			radio.broadcast(typeChannel, count);
		}
		// Archons clear census the round before.
		else if (rc.getType() == RobotType.ARCHON && isCensusDay(rc.getRoundNum() + 1)) {
			clearCensus();
		}
	}

	public boolean isCensusDay(int round) {
		return Arrays.binarySearch(CENSUS_DAYS, round) >= 0;
	}

	public void clearCensus() {
		// Reset first flag of robot types to -1.
		for (Channel c : new Channel[] { CENSUS_FIRST_GARDENER, CENSUS_FIRST_LUMBERJACK, CENSUS_FIRST_SCOUT,
				CENSUS_FIRST_SOLDIER, CENSUS_FIRST_ARCHON, CENSUS_FIRST_TANK }) {
			radio.broadcast(c, -1);
		}

		// Reset count of robot types to 0.
		for (Channel c : new Channel[] { CENSUS_GARDENER, CENSUS_LUMBERJACK, CENSUS_SCOUT, CENSUS_SOLDIER,
				CENSUS_ARCHON, CENSUS_TANK }) {
			radio.broadcast(c, 0);
		}

	}

	boolean firstOfType(RobotType type) {
		return radio.readChannel(firstChannelForType(type)) == -1;
	}

	Channel channelForType(RobotType type) {
		Channel c = CENSUS_GARDENER;

		switch (type) {
		case GARDENER:
			c = CENSUS_GARDENER;
			break;
		case LUMBERJACK:
			c = CENSUS_LUMBERJACK;
			break;
		case SCOUT:
			c = CENSUS_SCOUT;
			break;
		case SOLDIER:
			c = CENSUS_SOLDIER;
			break;
		case ARCHON:
			c = CENSUS_ARCHON;
			break;
		case TANK:
			c = CENSUS_TANK;
			break;
		default:
			break;
		}
		return c;
	}

	Channel firstChannelForType(RobotType type) {
		Channel c = CENSUS_FIRST_GARDENER;

		switch (type) {
		case GARDENER:
			c = CENSUS_FIRST_GARDENER;
			break;
		case LUMBERJACK:
			c = CENSUS_FIRST_LUMBERJACK;
			break;
		case SCOUT:
			c = CENSUS_FIRST_SCOUT;
			break;
		case SOLDIER:
			c = CENSUS_FIRST_SOLDIER;
			break;
		case ARCHON:
			c = CENSUS_FIRST_ARCHON;
			break;
		case TANK:
			c = CENSUS_FIRST_TANK;
			break;
		default:
			break;
		}
		return c;
	}

	public int count(RobotType type) {
		return radio.readChannel(channelForType(type));
	}
}
