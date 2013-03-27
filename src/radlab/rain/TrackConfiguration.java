package radlab.rain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.configuration.TrackConfKeys;

public class TrackConfiguration {

	public static long DEFAULT_OBJECT_POOL_MAX_SIZE = 50000;
	public static long DEFAULT_MEAN_RESPONSE_TIME_SAMPLE_INTERVAL = 500;
	public static String DEFAULT_LOAD_PROFILE_CLASS = "radlab.rain.LoadProfile";
	public static final String DEFAULT_LOAD_GENERATION_STRATEGY_CLASS = "radlab.rain.PartlyOpenLoopLoadGeneration";

	public String targetHostname = null;
	public int targetPort = 80;

	public String generatorClassName = "";
	public JSONObject generatorParams = null;
	public String loadProfileClassName = "";
	public String loadGenerationStrategyClassName = "";
	public JSONObject loadGenerationStrategyParams = null;
	public double openLoopProbability = 0.0;
	public String resourcePath = "resources/";
	public double meanCycleTime = 0.0; // non-stop request generation
	public double meanThinkTime = 0.0; // non-stop request generation
	public double logSamplingProbability = 1.0; // Log every executed request seen by the Scoreboard
	public double metricSnapshotInterval = 60.0; // (seconds)
	public boolean useMetricSnapshots = false;

	public String metricSnapshotFileSuffix = "";
	public long meanResponseTimeSamplingInterval = DEFAULT_MEAN_RESPONSE_TIME_SAMPLE_INTERVAL;
	public int maxUsersFromConfig = 0;
	public JSONObject loadProfileConfig;
	public long objPoolMaxSize = DEFAULT_OBJECT_POOL_MAX_SIZE;
	public String loadScheduleCreatorClass;

	public JSONObject loadSchedulerParams;
	public JSONObject metricWriterParams;

	public Map<String, MixMatrix> mixMatrices = new HashMap<String, MixMatrix>();

	public void initialize(JSONObject config) throws JSONException, Exception {
		// Open-Loop Probability
		openLoopProbability = config.getDouble(TrackConfKeys.OPEN_LOOP_PROBABILITY_KEY.toString());

		// Target Information
		JSONObject target = config.getJSONObject(TrackConfKeys.TARGET_KEY.toString());
		targetHostname = target.getString(TrackConfKeys.TARGET_HOSTNAME_KEY.toString());
		targetPort = target.getInt(TrackConfKeys.TARGET_PORT_KEY.toString());

		// Concrete Generator
		generatorClassName = config.getString(TrackConfKeys.GENERATOR_KEY.toString());
		if (config.has(TrackConfKeys.GENERATOR_PARAMS_KEY.toString()))
			generatorParams = config.getJSONObject(TrackConfKeys.GENERATOR_PARAMS_KEY.toString());

		// Log Sampling Probability
		logSamplingProbability = config.getDouble(TrackConfKeys.LOG_SAMPLING_PROBABILITY_KEY.toString());

		// Mean Cycle Time
		meanCycleTime = config.getDouble(TrackConfKeys.MEAN_CYCLE_TIME_KEY.toString());

		// Mean Think Time
		meanThinkTime = config.getDouble(TrackConfKeys.MEAN_THINK_TIME_KEY.toString());

		// Concrete Load Profile and Load Profile Array
		if (config.has(TrackConfKeys.LOAD_PROFILE_CLASS_KEY.toString()))
			loadProfileClassName = config.getString(TrackConfKeys.LOAD_PROFILE_CLASS_KEY.toString());
		else
			loadProfileClassName = DEFAULT_LOAD_PROFILE_CLASS;

		// Create the load schedule creator
		loadScheduleCreatorClass = config.getString(TrackConfKeys.LOAD_SCHEDULE_CREATOR_KEY.toString());

		// Look for load scheduler parameters if any exist
		if (config.has(TrackConfKeys.LOAD_SCHEDULE_CREATOR_PARAMS_KEY.toString()))
			loadSchedulerParams = config.getJSONObject(TrackConfKeys.LOAD_SCHEDULE_CREATOR_PARAMS_KEY.toString());

		// Load Mix Matrices/Behavior Directives
		JSONObject behavior = config.getJSONObject(TrackConfKeys.BEHAVIOR_KEY.toString());
		Iterator<String> keyIt = behavior.keys();

		// Each of the keys in the behavior section should be for some mix matrix
		while (keyIt.hasNext()) {
			String mixName = keyIt.next();

			// Now we need to get this object and parse it
			JSONArray mix = behavior.getJSONArray(mixName);
			double[][] data = null;
			for (int i = 0; i < mix.length(); i++) {
				if (i == 0) {
					data = new double[mix.length()][mix.length()];
				}
				// Each row is itself an array of doubles
				JSONArray row = mix.getJSONArray(i);
				for (int j = 0; j < row.length(); j++) {
					data[i][j] = row.getDouble(j);
				}
			}
			mixMatrices.put(mixName, new MixMatrix(data));
		}

		// Snapshot interval
		if (config.has(TrackConfKeys.METRIC_SNAPSHOT_INTERVAL.toString()))
			metricSnapshotInterval = config.getDouble(TrackConfKeys.METRIC_SNAPSHOT_INTERVAL.toString());

		// Snapshot file suffix
		if (config.has(TrackConfKeys.METRIC_SNAPSHOT_FILE_SUFFIX.toString()))
			metricSnapshotFileSuffix = config.getString(TrackConfKeys.METRIC_SNAPSHOT_FILE_SUFFIX.toString());

		if (config.has(TrackConfKeys.METRIC_SNAPSHOT_CONFIG.toString()))
			metricWriterParams = config.getJSONObject(TrackConfKeys.METRIC_SNAPSHOT_CONFIG.toString());

		// Maximum size of the object pool
		if (config.has(TrackConfKeys.OBJECT_POOL_MAX_SIZE.toString()))
			objPoolMaxSize = config.getLong(TrackConfKeys.OBJECT_POOL_MAX_SIZE.toString());

		// Configure the response time sampler
		if (config.has(TrackConfKeys.MEAN_RESPONSE_TIME_SAMPLE_INTERVAL.toString()))
			meanResponseTimeSamplingInterval = config.getLong(TrackConfKeys.MEAN_RESPONSE_TIME_SAMPLE_INTERVAL
					.toString());

		// Configure the maxUsers if specified
		if (config.has(TrackConfKeys.MAX_USERS.toString()))
			maxUsersFromConfig = config.getInt(TrackConfKeys.MAX_USERS.toString());

		// Look for a load generation strategy
		loadGenerationStrategyClassName = config.getString(TrackConfKeys.LOAD_GENERATION_STRATEGY_KEY.toString());

		// Check for parameters
		if (config.has(TrackConfKeys.LOAD_GENERATION_STRATEGY_PARAMS_KEY.toString()))
			loadGenerationStrategyParams = config.getJSONObject(TrackConfKeys.LOAD_GENERATION_STRATEGY_PARAMS_KEY
					.toString());
	}
}
