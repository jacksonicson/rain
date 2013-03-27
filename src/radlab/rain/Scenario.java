/*
 * Copyright (c) 2010, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  * Neither the name of the University of California, Berkeley
 * nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package radlab.rain;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.configuration.ScenarioConfigs;
import radlab.rain.configuration.TrackConfigs;
import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.util.ConfigUtil;

/**
 * The Scenario class contains the specifications for a benchmark scenario, which includes the timings (i.e. ramp up,
 * duration, ramp down) and the different scenario tracks.
 */
public class Scenario {
	private static Logger logger = LoggerFactory.getLogger(Scenario.class);
	public static final int DEFAULT_MAX_SHARED_THREADS = 10;
	public static final boolean DEFAULT_AGGREGATE_STATS = false;

	// Ramp up time in seconds.
	private long _rampUp;

	// Duration of the run in seconds.
	private long _duration;

	// Ramp down time in seconds.
	private long _rampDown;

	// Max number of threads to keep in the shared threadpool
	private int _maxSharedThreads = DEFAULT_MAX_SHARED_THREADS;

	// Log aggregated stats
	private boolean _aggregateStats = DEFAULT_AGGREGATE_STATS;

	// The instantiated tracks specified by the JSON configuration.
	private TreeMap<String, Target> _tracks = new TreeMap<String, Target>();

	public long getRampUp() {
		return this._rampUp;
	}

	public void setRampUp(long val) {
		this._rampUp = val;
	}

	public long getRampDown() {
		return this._rampDown;
	}

	public void setRampDown(long val) {
		this._rampDown = val;
	}

	public long getDuration() {
		return this._duration;
	}

	public void setDuration(long val) {
		this._duration = val;
	}

	public int getMaxSharedThreads() {
		return this._maxSharedThreads;
	}

	public void setMaxSharedThreads(int val) {
		this._maxSharedThreads = val;
	}

	public boolean getAggregateStats() {
		return this._aggregateStats;
	}

	public void setAggregateStats(boolean val) {
		this._aggregateStats = val;
	}

	public TreeMap<String, Target> getTracks() {
		return this._tracks;
	}

	/** Create a new and uninitialized <code>Scenario</code>. */
	public Scenario() {
	}

	/**
	 * Create a new Scenario and load the profile specified in the given JSON configuration object.
	 * 
	 * @param jsonConfig
	 *            The JSON object containing load specifications.
	 */
	public Scenario(JSONObject jsonConfig) throws Exception {
		this.loadProfile(jsonConfig);
	}

	/**
	 * Ask each scenario track to start.
	 */
	public void start() {
		for (Target track : this._tracks.values()) {
			track.start();
		}
	}

	/**
	 * Ask each scenario track to end.
	 */
	public void end() {
		logger.info("Tracks to end: " + this._tracks.values());
		for (Target track : this._tracks.values()) {
			track.end();
		}
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
			JSONObject timing = jsonConfig.getJSONObject(ScenarioConfigs.TIMING_KEY.toString());
			setRampUp(timing.getLong(ScenarioConfigs.RAMP_UP_KEY.toString()));
			setDuration(timing.getLong(ScenarioConfigs.DURATION_KEY.toString()));
			setRampDown(timing.getLong(ScenarioConfigs.RAMP_DOWN_KEY.toString()));

			// Set up Rain configuration params (if they've been provided)
			if (jsonConfig.has(ScenarioConfigs.VERBOSE_ERRORS_KEY.toString())) {
				boolean val = jsonConfig.getBoolean(ScenarioConfigs.VERBOSE_ERRORS_KEY.toString());
				RainConfig.getInstance()._verboseErrors = val;
			}

			// Setup sonar recorder
			if (jsonConfig.has(ScenarioConfigs.SONAR_HOSTNAME.toString())) {
				String host = jsonConfig.getString(ScenarioConfigs.SONAR_HOSTNAME.toString());
				RainConfig.getInstance()._sonarHost = host;
			}

			// Check if thrift remote management is used
			boolean useThrift = false;
			if (jsonConfig.has(ScenarioConfigs.USE_THRIFT.toString()))
				useThrift = jsonConfig.getBoolean(ScenarioConfigs.USE_THRIFT.toString());

			if (useThrift) {
				// Set in the config that we're using pipes
				RainConfig.getInstance()._useThrift = useThrift;

				// Check whether we're supposed to wait for a start signal
				if (jsonConfig.has(ScenarioConfigs.WAIT_FOR_START_SIGNAL.toString())) {
					RainConfig.getInstance().waitForStartSignal = jsonConfig
							.getBoolean(ScenarioConfigs.WAIT_FOR_START_SIGNAL.toString());
				}
			}

			// Look for the profiles key OR the name of a class that
			// generates the
			// profiles.
			if (jsonConfig.has(ScenarioConfigs.PROFILES_CREATOR_CLASS_KEY.toString())) {
				// Programmatic generation class takes precedence
				// Create profile creator class by reflection
				String profileCreatorClass = jsonConfig
						.getString(ScenarioConfigs.PROFILES_CREATOR_CLASS_KEY.toString());
				ProfileCreator creator = this.createLoadProfileCreator(profileCreatorClass);
				JSONObject params = null;
				// Look for profile creator params - if we find some then
				// pass them
				if (jsonConfig.has(ScenarioConfigs.PROFILES_CREATOR_CLASS_PARAMS_KEY.toString()))
					params = jsonConfig.getJSONObject(ScenarioConfigs.PROFILES_CREATOR_CLASS_PARAMS_KEY.toString());

				tracksConfig = creator.createProfile(params);
			} else // Otherwise there MUST be a profiles key in the config
					// file
			{
				String filename = jsonConfig.getString(ScenarioConfigs.PROFILES_KEY.toString());
				String fileContents = ConfigUtil.readFileAsString(filename);
				tracksConfig = new JSONObject(fileContents);
			}

			if (jsonConfig.has(ScenarioConfigs.MAX_SHARED_THREADS.toString())) {
				int sharedThreads = jsonConfig.getInt(ScenarioConfigs.MAX_SHARED_THREADS.toString());
				if (sharedThreads > 0)
					this._maxSharedThreads = sharedThreads;
			}

			if (jsonConfig.has(ScenarioConfigs.AGGREGATE_STATS.toString()))
				this._aggregateStats = jsonConfig.getBoolean(ScenarioConfigs.AGGREGATE_STATS.toString());
		} catch (JSONException e) {
			logger.info("[SCENARIO] ERROR reading JSON configuration object. Reason: " + e.toString());
			System.exit(1);
		} catch (IOException e) {
			logger.info("[SCENARIO] ERROR loading tracks configuration file. Reason: " + e.toString());
			System.exit(1);
		}

		this.loadTracks(tracksConfig);
	}

	@SuppressWarnings("unchecked")
	public ProfileCreator createLoadProfileCreator(String name) throws Exception {
		ProfileCreator creator = null;
		Class<ProfileCreator> creatorClass = (Class<ProfileCreator>) Class.forName(name);
		Constructor<ProfileCreator> creatorCtor = creatorClass.getConstructor(new Class[] {});
		creator = (ProfileCreator) creatorCtor.newInstance((Object[]) null);
		return creator;
	}

	/**
	 * Reads the track configuration from the provided JSON configuration object and creates each scenario track.
	 * 
	 * @param jsonConfig
	 *            The JSON object containing load specifications.
	 */
	@SuppressWarnings("unchecked")
	protected void loadTracks(JSONObject jsonConfig) {
		try {
			Iterator<String> i = jsonConfig.keys();
			while (i.hasNext()) {
				String trackName = i.next();
				JSONObject trackConfig = jsonConfig.getJSONObject(trackName);

				String trackClassName = trackConfig.getString(TrackConfigs.TRACK_CLASS_KEY.toString());
				Target track = this.createTrack(trackClassName, trackName);
				track.setName(trackName);
				track.initialize(trackConfig);

				this._tracks.put(track._name, track);
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

	/**
	 * Factory method for creating scenario tracks.
	 * 
	 * @param trackClassName
	 *            The class of the scenario track to create.
	 * @param trackName
	 *            The name of the instantiated track.
	 * @return A newly instantiated scenario track.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public Target createTrack(String trackClassName, String trackName) throws Exception {
		Target track = null;
		Class<Target> trackClass = (Class<Target>) Class.forName(trackClassName);
		Constructor<Target> trackCtor = trackClass.getConstructor(new Class[] { String.class, Scenario.class });
		track = (Target) trackCtor.newInstance(new Object[] { trackName, this });
		return track;
	}

	void simulateTracks(long start, long startSteadyState, long endSteadyState) throws Exception {
		int sharedThreads = getMaxSharedThreads();
		ExecutorService pool = Executors.newFixedThreadPool(sharedThreads);

		logger.debug("Creating " + sharedThreads + " shared threads.");

		// Scenarios manage their own threads
		long totalMaxUsers = 0;
		for (Target track : getTracks().values()) {
			// Start the scoreboard. It needs to know the timings because we only
			// want to retain metrics generated during the steady state interval.
			IScoreboard scoreboard = track.createScoreboard(null);
			scoreboard.initialize(startSteadyState, endSteadyState, track.getMaxUsers());
			scoreboard.setMetricSnapshotInterval((long) (track.getMetricSnapshotInterval() * 1000));
			scoreboard.setMetricWriter(track.getMetricWriter());
			scoreboard.start();

			track.createLoadGenerators(start, pool, scoreboard);
		}

		// Join all running tracks
		for (Target track : getTracks().values()) {
			track.join();
		}

		// Shutdown thread pool
		pool.shutdown();
		try {
			logger.debug("waiting up to 10 seconds for shared threadpool to shutdown!");
			pool.awaitTermination(10000, TimeUnit.MILLISECONDS);
			if (!pool.isTerminated())
				pool.shutdownNow();
		} catch (InterruptedException ie) {
			logger.debug("INTERRUPTED while waiting for shared threadpool to shutdown!");
		}
	}
}
