package radlab.rain.configuration;

public enum TrackConfKeys {

	TRACK_CLASS_KEY("track"),
	OPEN_LOOP_PROBABILITY_KEY("pOpenLoop"),
	LOG_SAMPLING_PROBABILITY_KEY("pLogSampling"),
	MEAN_CYCLE_TIME_KEY("meanCycleTime"),
	MEAN_THINK_TIME_KEY("meanThinkTime"),
	INTERACTIVE_KEY("interactive"),
	TARGET_KEY("target"),
	METRIC_SNAPSHOT_INTERVAL("metricSnapshotInterval"),
	METRIC_SNAPSHOTS("metricSnapshots"),
	METRIC_SNAPSHOT_FILE_SUFFIX("metricSnapshotsFileSuffix"),
	METRIC_SNAPSHOT_CONFIG("metricSnapshotConfig"),
	METRIC_DB("metricDB"),
	
	// Targets keys: hostname, port
	TARGET_HOSTNAME_KEY("hostname"),
	TARGET_PORT_KEY("port"),
	GENERATOR_KEY("generator"),
	GENERATOR_PARAMS_KEY("generatorParameters"),
	LOAD_PROFILE_CLASS_KEY("loadProfileClass"),
	LOAD_PROFILE_KEY("loadProfile"),
	LOAD_SCHEDULE_CREATOR_KEY("loadScheduleCreator"),
	LOAD_SCHEDULE_CREATOR_PARAMS_KEY("loadScheduleCreatorParameters"),
	LOAD_GENERATION_STRATEGY_KEY("loadGenerationStrategy"),
	LOAD_GENERATION_STRATEGY_PARAMS_KEY("loadGenerationStrategyParams"),

	// Load behavioral hints
	BEHAVIOR_KEY("behavior"),
	RESOURCE_PATH("resourcePath"),
	OBJECT_POOL_MAX_SIZE("objectPoolMaxSize"),
	MEAN_RESPONSE_TIME_SAMPLE_INTERVAL("meanResponseTimeSamplingInterval"),
	MAX_USERS("maxUsers");

	private String configuration;
	TrackConfKeys(String config) {
		this.configuration = config;
	}

	public String toString() {
		return this.configuration;
	}
}
