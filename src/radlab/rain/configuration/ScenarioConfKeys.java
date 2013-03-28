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
	TRACK_FACTORY_CLASS("trackConfCreatorClass"),
	TRACK_FACTORY_CONF("trackConfCreatorParams"),

	// Wait for a start signal
	WAIT_FOR_START_SIGNAL("waitForStartSignal"),

	// Thrift and sonar configuration
	USE_THRIFT("useThrift"),
	SONAR_HOSTNAME("sonarHost"),
	METRIC_WRITER_TYPE("metricWriterType"),
	METRIC_WRITER_CONF("metricWriterConf");

	private String configuration;

	ScenarioConfKeys(String config) {
		this.configuration = config;
	}

	public String toString() {
		return this.configuration;
	}
}
