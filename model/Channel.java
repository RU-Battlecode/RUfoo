package RUfoo.model;

public enum Channel {
	
	// Holds tree locations that are annoying
	TREE_CHANNEL,
	
	// The area that needs protection
	DEFENSE_NEEDED_LOCATION_START,
	DNL1,
	DNL2,
	DNL3,
	DNL4,
	DEFENSE_NEEDED_LOCATION_END,
	
	// The count of attack units the defense location needs
	DEFENSE_NEEDED_COUNT_START,
	DNC1,
	DNC2,
	DNC3,
	DNC4,
	DEFENSE_NEEDED_COUNT_END,
	
	
	// Where is the enemy Archon and what is its id?
	ENEMY_ARCHON_CHANNEL1,
	ENEMY_ARCHON_ID_CHANNEL1,
	ENEMY_ARCHON_CHANNEL2,
	ENEMY_ARCHON_ID_CHANNEL2,
	ENEMY_ARCHON_CHANNEL3,
	ENEMY_ARCHON_ID_CHANNEL3,
	
	// Counting team robot types that are alive.
	CENSUS_ARCHON,
	CENSUS_FIRST_ARCHON,
	CENSUS_GARDENER,
	CENSUS_FIRST_GARDENER,
	CENSUS_LUMBERJACK,
	CENSUS_FIRST_LUMBERJACK,
	CENSUS_SCOUT,
	CENSUS_FIRST_SCOUT,
	CENSUS_SOLDIER,
	CENSUS_FIRST_SOLDIER,
	CENSUS_TANK,
	CENSUS_FIRST_TANK,
	
	
	// Determining the archon leader.
	ARCHON_LEADER_ID,
	ARCHON_LEADER_POLL,
	
	// Location and IDs of all the spotted gardeners
	ENEMY_GARDENER_ID_START,
	EG1,
	EG2,
	EG3,
	EG4,
	EG5,
	EG6,
	EG7,
	EG8,
	EG9,
	ENEMY_GARDENER_ID_END,
	
	ENEMY_GARDENER_LOC_START,
	EGL1,
	EGL2,
	EGL3,
	EGL4,
	EGL5,
	EGL6,
	EGL7,
	EGL8,
	EGL9,
	ENEMY_GARDENER_LOC_END,
	
	// Can scouts share there target?
	SCOUT_TARGET_LOCATION_START,
	ST1,
	ST2,
	SCOUT_TARGET_LOCATION_END,
	
	GARDENER_BASE_LOCATION_START,
	GBL1,
	GBL2,
	GBL3,
	GBL4,
	GBL5,
	GBL6,
	GBL7,
	GBL8,
	GARDENER_BASE_LOCATION_END,
	
}
