package radlab.rain;

import java.util.HashMap;
import java.util.Map;

public class TrackConfiguration {
	public static long DEFAULT_MEAN_RESPONSE_TIME_SAMPLE_INTERVAL = 500;

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
