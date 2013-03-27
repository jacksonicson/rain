package radlab.rain.configuration;

public enum ScenarioConfigs {

	PROFILES_KEY("profiles"), PROFILES_CREATOR_CLASS_KEY("profilesCreatorClass"), PROFILES_CREATOR_CLASS_PARAMS_KEY(
			"profilesCreatorClassParams"), TIMING_KEY("timing"), RAMP_UP_KEY(
			"rampUp"), DURATION_KEY("duration"), RAMP_DOWN_KEY("rampDown"), VERBOSE_ERRORS_KEY(
			"verboseErrors"), SONAR_HOSTNAME("sonarHost"), USE_THRIFT(
			"useThrift"), WAIT_FOR_START_SIGNAL(
			"waitForStartSignal"), MAX_SHARED_THREADS("maxSharedThreads"), AGGREGATE_STATS(
			"aggregateStats");

	private String configuration;

	ScenarioConfigs(String config) {
		this.configuration = config;
	}

	public String toString() {
		return this.configuration;
	}
}
