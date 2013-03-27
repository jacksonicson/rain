package radlab.rain;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.configuration.ScenarioConfKeys;
import radlab.rain.util.ConfigUtil;

public class ScenarioConfiguration {

	private static Logger logger = LoggerFactory.getLogger(ScenarioConfiguration.class);

	public static final int DEFAULT_MAX_SHARED_THREADS = 10;
	public static final boolean DEFAULT_AGGREGATE_STATS = false;

	// Ramp up time in seconds.
	private long rampUp;

	// Duration of the run in seconds.
	private long duration;

	// Ramp down time in seconds.
	private long rampDown;

	// Max number of threads to keep in the shared threadpool
	private int maxSharedThreads = DEFAULT_MAX_SHARED_THREADS;

	// Log aggregated stats
	private boolean aggregateStats = DEFAULT_AGGREGATE_STATS;

	// List of all track configurations
	private List<JSONObject> trackConfigurations = new ArrayList<JSONObject>();

	public long getRampUp() {
		return this.rampUp;
	}

	public void setRampUp(long val) {
		this.rampUp = val;
	}

	public long getRampDown() {
		return this.rampDown;
	}

	public void setRampDown(long val) {
		this.rampDown = val;
	}

	public long getDuration() {
		return this.duration;
	}

	public void setDuration(long val) {
		this.duration = val;
	}

	public int getMaxSharedThreads() {
		return this.maxSharedThreads;
	}

	public void setMaxSharedThreads(int val) {
		this.maxSharedThreads = val;
	}

	public boolean getAggregateStats() {
		return this.aggregateStats;
	}

	public void setAggregateStats(boolean val) {
		this.aggregateStats = val;
	}

	public List<JSONObject> getTrackConfigurations() {
		return this.trackConfigurations;
	}

	/**
	 * Reads the run specifications from the provided JSON configuration object. The timings (i.e. ramp up, duration,
	 * and ramp down) are set and the scenario tracks are created.
	 * 
	 * @param jsonConfig
	 *            The JSON object containing load specifications.
	 */
	public void loadProfile(JSONObject jsonConfig) throws Exception {
		JSONObject tracksConfig = null;
		try {
			JSONObject timing = jsonConfig.getJSONObject(ScenarioConfKeys.TIMING_KEY.toString());
			setRampUp(timing.getLong(ScenarioConfKeys.RAMP_UP_KEY.toString()));
			setDuration(timing.getLong(ScenarioConfKeys.DURATION_KEY.toString()));
			setRampDown(timing.getLong(ScenarioConfKeys.RAMP_DOWN_KEY.toString()));

			// Set up Rain configuration params (if they've been provided)
			if (jsonConfig.has(ScenarioConfKeys.VERBOSE_ERRORS_KEY.toString())) {
				boolean val = jsonConfig.getBoolean(ScenarioConfKeys.VERBOSE_ERRORS_KEY.toString());
				RainConfig.getInstance()._verboseErrors = val;
			}

			// Setup sonar recorder
			if (jsonConfig.has(ScenarioConfKeys.SONAR_HOSTNAME.toString())) {
				String host = jsonConfig.getString(ScenarioConfKeys.SONAR_HOSTNAME.toString());
				RainConfig.getInstance()._sonarHost = host;
			}

			// Check if thrift remote management is used
			boolean useThrift = false;
			if (jsonConfig.has(ScenarioConfKeys.USE_THRIFT.toString()))
				useThrift = jsonConfig.getBoolean(ScenarioConfKeys.USE_THRIFT.toString());

			if (useThrift) {
				// Set in the config that we're using pipes
				RainConfig.getInstance()._useThrift = useThrift;

				// Check whether we're supposed to wait for a start signal
				if (jsonConfig.has(ScenarioConfKeys.WAIT_FOR_START_SIGNAL.toString())) {
					RainConfig.getInstance().waitForStartSignal = jsonConfig
							.getBoolean(ScenarioConfKeys.WAIT_FOR_START_SIGNAL.toString());
				}
			}

			// Look for the profiles key OR the name of a class that
			// generates the
			// profiles.
			if (jsonConfig.has(ScenarioConfKeys.PROFILES_CREATOR_CLASS_KEY.toString())) {
				// Programmatic generation class takes precedence
				// Create profile creator class by reflection
				String profileCreatorClass = jsonConfig.getString(ScenarioConfKeys.PROFILES_CREATOR_CLASS_KEY
						.toString());
				ProfileCreator creator = createLoadProfileCreator(profileCreatorClass);
				JSONObject params = null;
				// Look for profile creator params - if we find some then
				// pass them
				if (jsonConfig.has(ScenarioConfKeys.PROFILES_CREATOR_CLASS_PARAMS_KEY.toString()))
					params = jsonConfig.getJSONObject(ScenarioConfKeys.PROFILES_CREATOR_CLASS_PARAMS_KEY.toString());

				tracksConfig = creator.createProfile(params);
			} else // Otherwise there MUST be a profiles key in the config
					// file
			{
				String filename = jsonConfig.getString(ScenarioConfKeys.PROFILES_KEY.toString());
				String fileContents = ConfigUtil.readFileAsString(filename);
				tracksConfig = new JSONObject(fileContents);
			}

			if (jsonConfig.has(ScenarioConfKeys.MAX_SHARED_THREADS.toString())) {
				int sharedThreads = jsonConfig.getInt(ScenarioConfKeys.MAX_SHARED_THREADS.toString());
				if (sharedThreads > 0)
					this.maxSharedThreads = sharedThreads;
			}

			if (jsonConfig.has(ScenarioConfKeys.AGGREGATE_STATS.toString()))
				this.aggregateStats = jsonConfig.getBoolean(ScenarioConfKeys.AGGREGATE_STATS.toString());
		} catch (JSONException e) {
			logger.info("[SCENARIO] ERROR reading JSON configuration object. Reason: " + e.toString());
			System.exit(1);
		} catch (IOException e) {
			logger.info("[SCENARIO] ERROR loading tracks configuration file. Reason: " + e.toString());
			System.exit(1);
		}

		loadTracks(tracksConfig);
	}

	/**
	 * Reads the track configuration from the provided JSON configuration object and creates each scenario track.
	 * 
	 * @param jsonConfig
	 *            The JSON object containing load specifications.
	 */
	@SuppressWarnings("unchecked")
	private void loadTracks(JSONObject jsonConfig) {
		try {
			Iterator<String> i = jsonConfig.keys();
			while (i.hasNext()) {
				String trackName = i.next();
				JSONObject trackConfig = jsonConfig.getJSONObject(trackName);
				trackConfigurations.add(trackConfig);
			}
		} catch (JSONException e) {
			logger.info("[SCENARIO] ERROR parsing tracks in JSON configuration file/object. Reason: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			logger.info("[SCENARIO] ERROR initializing tracks. Reason: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		}
	}

	@SuppressWarnings("unchecked")
	public ProfileCreator createLoadProfileCreator(String name) throws Exception {
		Class<ProfileCreator> creatorClass = (Class<ProfileCreator>) Class.forName(name);
		Constructor<ProfileCreator> creatorCtor = creatorClass.getConstructor(new Class[] {});
		ProfileCreator creator = (ProfileCreator) creatorCtor.newInstance((Object[]) null);
		return creator;
	}
}
