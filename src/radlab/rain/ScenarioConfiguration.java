package radlab.rain;

import java.io.IOException;
import java.lang.reflect.Constructor;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.configuration.ScenarioConfKeys;

public class ScenarioConfiguration {

	private static Logger logger = LoggerFactory.getLogger(ScenarioConfiguration.class);

	public static final boolean DEFAULT_AGGREGATE_STATS = false;

	// Ramp up time in seconds.
	private long rampUp;

	// Duration of the run in seconds.
	private long duration;

	// Ramp down time in seconds.
	private long rampDown;

	// Factory to generate track configurations
	TrackFactory tracksConfFactory;

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

	public TrackFactory getTrackFactory() {
		return tracksConfFactory;
	}

	/**
	 * Reads the run specifications from the provided JSON configuration object. The timings (i.e. ramp up, duration,
	 * and ramp down) are set and the scenario tracks are created.
	 * 
	 * @param jsonConfig
	 *            The JSON object containing load specifications.
	 */
	public void loadProfile(JSONObject jsonConfig) throws Exception {
		try {
			JSONObject timing = jsonConfig.getJSONObject(ScenarioConfKeys.TIMING_KEY.toString());
			setRampUp(timing.getLong(ScenarioConfKeys.RAMP_UP_KEY.toString()));
			setDuration(timing.getLong(ScenarioConfKeys.DURATION_KEY.toString()));
			setRampDown(timing.getLong(ScenarioConfKeys.RAMP_DOWN_KEY.toString()));

			// Set up Rain configuration params (if they've been provided)
			if (jsonConfig.has(ScenarioConfKeys.VERBOSE_ERRORS_KEY.toString())) {
				boolean val = jsonConfig.getBoolean(ScenarioConfKeys.VERBOSE_ERRORS_KEY.toString());
				RainConfig.getInstance().verboseErrors = val;
			}

			// Setup sonar recorder
			if (jsonConfig.has(ScenarioConfKeys.SONAR_HOSTNAME.toString())) {
				String host = jsonConfig.getString(ScenarioConfKeys.SONAR_HOSTNAME.toString());
				RainConfig.getInstance().sonarHost = host;
			}

			// Check if thrift remote management is used
			boolean useThrift = false;
			if (jsonConfig.has(ScenarioConfKeys.USE_THRIFT.toString()))
				useThrift = jsonConfig.getBoolean(ScenarioConfKeys.USE_THRIFT.toString());

			if (useThrift) {
				// Set in the config that we're using pipes
				RainConfig.getInstance().useThrift = useThrift;

				// Check whether we're supposed to wait for a start signal
				if (jsonConfig.has(ScenarioConfKeys.WAIT_FOR_START_SIGNAL.toString())) {
					RainConfig.getInstance().waitForStartSignal = jsonConfig
							.getBoolean(ScenarioConfKeys.WAIT_FOR_START_SIGNAL.toString());
				}
			}

			// Look for the profiles key OR the name of a class that
			// generates the
			// profiles.
			if (jsonConfig.has(ScenarioConfKeys.TRACK_CONF_CREATOR_CLASS_KEY.toString())) {
				// Programmatic generation class takes precedence
				// Create profile creator class by reflection
				String trackConfClass = jsonConfig.getString(ScenarioConfKeys.TRACK_CONF_CREATOR_CLASS_KEY.toString());
				tracksConfFactory = createLoadProfileCreator(trackConfClass);
				JSONObject params = jsonConfig.getJSONObject(ScenarioConfKeys.TRACK_CONF_CREATOR_PARAMS_KEY.toString());
				tracksConfFactory.configure(params);
			}

		} catch (JSONException e) {
			logger.info("[SCENARIO] ERROR reading JSON configuration object. Reason: " + e.toString());
			System.exit(1);
		} catch (IOException e) {
			logger.info("[SCENARIO] ERROR loading tracks configuration file. Reason: " + e.toString());
			System.exit(1);
		}
	}

	@SuppressWarnings("unchecked")
	public TrackFactory createLoadProfileCreator(String name) throws Exception {
		Class<TrackFactory> creatorClass = (Class<TrackFactory>) Class.forName(name);
		Constructor<TrackFactory> creatorCtor = creatorClass.getConstructor(new Class[] {});
		TrackFactory creator = (TrackFactory) creatorCtor.newInstance((Object[]) null);
		return creator;
	}
}
