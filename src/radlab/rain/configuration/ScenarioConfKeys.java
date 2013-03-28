package radlab.rain.configuration;

public enum ScenarioConfKeys {

	// Timing section
	TIMING_KEY("timing"),
	RAMP_UP_KEY("rampUp"),
	DURATION_KEY("duration"),
	RAMP_DOWN_KEY("rampDown"),

	// Verbose errors
	VERBOSE_ERRORS_KEY("verboseErrors"),

	// Track configuration creator
	TRACK_CONF_CREATOR_CLASS_KEY("trackConfCreatorClass"),
	TRACK_CONF_CREATOR_PARAMS_KEY("trackConfCreatorParams"),

	// Wait for a start signal
	WAIT_FOR_START_SIGNAL("waitForStartSignal"),

	// Thrift and sonar configuration
	USE_THRIFT("useThrift"),
	SONAR_HOSTNAME("sonarHost");

	private String configuration;

	ScenarioConfKeys(String config) {
		this.configuration = config;
	}

	public String toString() {
		return this.configuration;
	}
}
