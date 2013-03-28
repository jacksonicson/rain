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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.scoreboard.Scoreboard;
import radlab.rain.util.MetricWriter;
import radlab.rain.util.MetricWriterFactory;

/**
 * The ScenarioTrack abstract class represents a single workload among potentially many that are simultaneously run
 * under a single Scenario.<br />
 * <br />
 * The ScenarioTrack is responsible for reading in the configuration of a workload and generating the load profiles.
 */
public abstract class Target implements ITrack {
	private static Logger logger = LoggerFactory.getLogger(Target.class);

	// Timings
	protected Timing timing;

	// Metric writer configuration
	private MetricWriterFactory.Type metricWriterType;
	private JSONObject metricWriterConf;

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
	public long meanResponseTimeSamplingInterval = 500;

	// Scoreboard
	protected IScoreboard scoreboard;

	// Generator factory
	protected GeneratorFactory generatorFactory;

	// List of all load generating units
	protected List<Agent> loadGeneratingUnits = new ArrayList<Agent>();

	// Load schedule used by the generator and strategy
	protected LoadScheduleCreator loadScheduleCreator;
	protected LoadDefinition currentLoadUnit;
	protected LoadSchedule loadSchedule;

	// Executer pool
	protected ExecutorService executor;

	// Abstract methods
	protected abstract boolean validateLoadDefinition(LoadDefinition definition);

	protected abstract Agent createLoadGeneratingUnit(long id, Generator generator) throws Exception;

	public void setTiming(Timing timing) {
		this.timing = timing;
	}

	public void setLoadScheduleCreator(LoadScheduleCreator loadScheduleCreator) {
		this.loadScheduleCreator = loadScheduleCreator;
	}

	public void setGeneratorFactory(GeneratorFactory generatorFactory) {
		this.generatorFactory = generatorFactory;
	}

	public void init() throws Exception {
		// Create scoreboard
		scoreboard = createScoreboard();

		// Create load schedule creator and load schedule
		loadSchedule = loadScheduleCreator.createSchedule(null);

		// Create a new thread pool
		executor = Executors.newCachedThreadPool();
	}

	public void start() throws Exception {
		// Create load generating units
		createLoadGeneratingUnits(executor);

		// Start the scoreboard
		scoreboard.start();

		// Start load generating unit threads
		for (Agent generator : loadGeneratingUnits)
			generator.start();
	}

	public void end() {
		// Wait for all load generating units to exit
		for (Agent generator : loadGeneratingUnits) {
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

		// Create a metric writer
		MetricWriter metricWriter = MetricWriterFactory.createMetricWriter(metricWriterType, metricWriterConf);

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
			Agent lgUnit = createLoadGeneratingUnit(i, generator);
			lgUnit.setExecutorService(executor);
			lgUnit.setTimeStarted(System.currentTimeMillis());

			// Add thread to thread list and start the thread
			loadGeneratingUnits.add(lgUnit);
		}
	}

	public IScoreboard getScoreboard() {
		return scoreboard;
	}

	public void setMetricWriterType(MetricWriterFactory.Type metricWriterType) {
		this.metricWriterType = metricWriterType;
	}

	public void setMetricWriterConf(JSONObject metricWriterConf) {
		this.metricWriterConf = metricWriterConf;
	}
}
