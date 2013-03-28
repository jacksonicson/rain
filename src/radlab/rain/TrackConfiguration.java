package radlab.rain;

import java.util.HashMap;
import java.util.Map;

public class TrackConfiguration {
	public static long DEFAULT_MEAN_RESPONSE_TIME_SAMPLE_INTERVAL = 500;

	// Which load generation strategy to use
	public String loadGenerationStrategyClass = "";

	// Which generator to use
	public String generatorClass = "";

	// Generator for the load schedule
	public String loadScheduleCreatorClass;

	// Markov chain matrices
	public Map<String, MixMatrix> mixMatrices = new HashMap<String, MixMatrix>();

	// Execution times
	public double openLoopProbability;
	public double meanCycleTime;
	public double meanThinkTime;

	// Sampling
	public double logSamplingProbability = 1.0;
	public double metricSnapshotInterval = 60.0;
	public boolean useMetricSnapshots = false;
	public long meanResponseTimeSamplingInterval = DEFAULT_MEAN_RESPONSE_TIME_SAMPLE_INTERVAL;
}
