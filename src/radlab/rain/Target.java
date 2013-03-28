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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.configuration.TrackConfKeys;
import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.scoreboard.Scoreboard;
import radlab.rain.util.MetricWriter;

/**
 * The ScenarioTrack abstract class represents a single workload among potentially many that are simultaneously run
 * under a single Scenario.<br />
 * <br />
 * The ScenarioTrack is responsible for reading in the configuration of a workload and generating the load profiles.
 */
public abstract class Target implements ITarget {
	private static Logger logger = LoggerFactory.getLogger(Target.class);

	// Timings
	protected Timing timing;

	// Metric writer configuration
	private MetricWriter metricWriter;

	// Load manager
	protected LoadManager loadManager;

	// Scoreboard
	protected IScoreboard scoreboard;

	// Generator factory
	protected GeneratorFactory generatorFactory;

	// Load schedule used by the generator and strategy
	protected LoadScheduleFactory loadScheduleFactory;

	// Load schedule
	protected LoadSchedule loadSchedule;

	// Current load definition
	protected LoadDefinition currentLoadDefinition;

	// Markov chain matrices
	protected Map<String, MixMatrix> mixMatrices = new HashMap<String, MixMatrix>();

	// Execution times
	protected double openLoopProbability;
	protected double meanCycleTime;
	protected double meanThinkTime;

	// Sampling
	protected double logSamplingProbability = 1.0;
	protected double metricSnapshotInterval = 60.0;
	protected boolean useMetricSnapshots = false;
	protected long meanResponseTimeSamplingInterval = 500;

	// List of all load generating units
	protected List<Agent> agents = new ArrayList<Agent>();

	// Executer pool
	protected ExecutorService executor;

	private boolean validateLoadDefinition(LoadDefinition profile) {
		// Check number of users
		if (profile.numberOfUsers <= 0) {
			logger.info("Invalid load profile. Number of users <= 0. Profile details: " + profile.toString());
			return false;
		}

		// Check references to the mix matrix
		if (profile.mixName.length() > 0 && !mixMatrices.containsKey(profile.mixName)) {
			logger.info("Invalid load profile. mixname not in track's mixmap. Profile details: " + profile.toString());
			return false;
		}

		return true;
	}

	protected Agent createAgent(long id, Generator generator) {
		AgentPOL agent = new AgentPOL(id, loadManager, generator, timing);
		return agent;
	}

	public void init() throws Exception {
		// Create scoreboard
		scoreboard = createScoreboard();

		// Create load schedule creator and load schedule
		loadSchedule = loadScheduleFactory.createSchedule();

		// Create a new load manager
		loadManager = new LoadManager(timing, loadSchedule);

		// Create a new thread pool
		executor = Executors.newCachedThreadPool();
	}

	public void start() throws Exception {
		// Starting load manager
		loadManager.start();

		// Create load generating units
		createLoadGeneratingUnits(executor);

		// Start the scoreboard
		scoreboard.start();

		// Start load generating unit threads
		for (Agent generator : agents)
			generator.start();
	}

	public void configure(JSONObject config) throws JSONException {
		// Open-Loop Probability
		openLoopProbability = config.getDouble(TrackConfKeys.OPEN_LOOP_PROBABILITY_KEY.toString());

		// Log Sampling Probability
		logSamplingProbability = config.getDouble(TrackConfKeys.LOG_SAMPLING_PROBABILITY_KEY.toString());

		// Mean Cycle Time
		meanCycleTime = config.getDouble(TrackConfKeys.MEAN_CYCLE_TIME_KEY.toString());

		// Mean Think Time
		meanThinkTime = config.getDouble(TrackConfKeys.MEAN_THINK_TIME_KEY.toString());

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

		// Configure the response time sampler
		if (config.has(TrackConfKeys.MEAN_RESPONSE_TIME_SAMPLE_INTERVAL.toString()))
			meanResponseTimeSamplingInterval = config.getLong(TrackConfKeys.MEAN_RESPONSE_TIME_SAMPLE_INTERVAL
					.toString());
	}

	public void end() {
		// Shutdown load manager
		try {
			loadManager.setDone(true);
			loadManager.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Wait for all load generating units to exit
		for (Agent generator : agents) {
			try {
				generator.join();
				logger.info("Thread joined: " + generator.getName());
			} catch (InterruptedException ie) {
				logger.error("Main thread interrupted... exiting!");
			} finally {
				generator.dispose();
			}
		}

		// Stop the scoreboard
		scoreboard.stop();
	}

	private IScoreboard createScoreboard() throws JSONException, Exception {
		logger.debug("Creating track scoreboard...");

		// Create scoreboard
		IScoreboard scoreboard = new Scoreboard("track");

		// Set the log sampling probability for the scoreboard
		scoreboard.initialize(timing);
		scoreboard.setScenarioTrack(this);
		scoreboard.setLogSamplingProbability(logSamplingProbability);
		scoreboard.setUsingMetricSnapshots(useMetricSnapshots);
		scoreboard.setMeanResponseTimeSamplingInterval(meanResponseTimeSamplingInterval);
		scoreboard.setMetricSnapshotInterval((long) (metricSnapshotInterval * 1000));
		scoreboard.setMetricWriter(metricWriter);
		scoreboard.start();

		return scoreboard;
	}

	private void createLoadGeneratingUnits(ExecutorService executor) throws Exception {
		// Determine maximum number of required lg units that are required by the schedule
		long maxGenerators = loadSchedule.getMaxGenerators();

		// Create all lg units
		for (int i = 0; i < maxGenerators; i++) {
			// Setup generator
			Generator generator = generatorFactory.createGenerator();
			generator.setScoreboard(scoreboard);
			generator.setMeanCycleTime((long) (meanCycleTime * 1000));
			generator.setMeanThinkTime((long) (meanThinkTime * 1000));
			generator.initialize();

			// Allow the load generation strategy to be configurable
			Agent lgUnit = createAgent(i, generator);
			lgUnit.setExecutorService(executor);
			lgUnit.setTimeStarted(System.currentTimeMillis());

			// Add thread to thread list and start the thread
			agents.add(lgUnit);
		}
	}

	public void setTiming(Timing timing) {
		this.timing = timing;
	}

	public IScoreboard getScoreboard() {
		return scoreboard;
	}

	public void setMetricWriter(MetricWriter metricWriter) {
		this.metricWriter = metricWriter;
	}

	public void setLoadScheduleFactory(LoadScheduleFactory loadScheduleFactory) {
		this.loadScheduleFactory = loadScheduleFactory;
	}

	public void setGeneratorFactory(GeneratorFactory generatorFactory) {
		this.generatorFactory = generatorFactory;
	}
}
