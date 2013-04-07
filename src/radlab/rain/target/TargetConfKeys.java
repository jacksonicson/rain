package radlab.rain.target;

public enum TargetConfKeys {

	OPEN_LOOP_PROBABILITY_KEY("pOpenLoop"), LOG_SAMPLING_PROBABILITY_KEY("pLogSampling"), MEAN_CYCLE_TIME_KEY(
			"meanCycleTime"), MEAN_THINK_TIME_KEY("meanThinkTime"),

	// Interval to get metric snapshots
	METRIC_SNAPSHOT_INTERVAL("metricSnapshotInterval"),
	MEAN_RESPONSE_TIME_SAMPLE_INTERVAL("meanResponseTimeSamplingInterval"),

	// Markov chain behavior keys
	BEHAVIOR_KEY("behavior");

	private String value;

	TargetConfKeys(String value) {
		this.value = value;
	}

	public String toString() {
		return this.value;
	}
}
