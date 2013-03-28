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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.util.MetricWriter;
import radlab.rain.util.MetricWriterFactory;

/**
 * The ScenarioTrack abstract class represents a single workload among potentially many that are simultaneously run
 * under a single Scenario.<br />
 * <br />
 * The ScenarioTrack is responsible for reading in the configuration of a workload and generating the load profiles.
 */
public abstract class Track {
	private static Logger logger = LoggerFactory.getLogger(Track.class);

	// Timings
	protected Timing timing;

	// Configuration
	protected TrackConfiguration config;

	// Scoreboard
	protected IScoreboard scoreboard;

	// List of all load generating units
	protected List<LoadGeneratingUnit> loadGeneratingUnits = new ArrayList<LoadGeneratingUnit>();

	// Load schedule used by the generator and strategy
	protected LoadDefinition currentLoadUnit;
	protected LoadScheduleCreator loadScheduleCreator;
	protected LoadSchedule loadSchedule;

	// Executer pool
	protected ExecutorService executor;

	// Abstract methods
	protected abstract boolean validateLoadDefinition(LoadDefinition definition);

	protected abstract LoadGeneratingUnit createLoadGenerationStrategy(long id, Generator generator) throws Exception;

	public Track(Timing timing) {
		this.timing = timing;
	}

	public void start() {
		// Start the scoreboard
		scoreboard.start();

		// Start load generating unit threads
		for (LoadGeneratingUnit generator : loadGeneratingUnits)
			generator.start();
	}

	public void end() {
		// Wait for all load generating units to exit
		for (LoadGeneratingUnit generator : loadGeneratingUnits) {
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

	void initialize(JSONObject jsonConfig) throws Exception {
		// Load configuration
		config = new TrackConfiguration();
		config.initialize(jsonConfig);

		// Create scoreboard
		scoreboard = createScoreboard();

		// Create load schedule creator and load schedule
		loadScheduleCreator = createLoadScheduleCreator();
		loadSchedule = loadScheduleCreator.createSchedule(config.loadSchedulerParams);

		// Create a new thread pool
		executor = Executors.newCachedThreadPool();

		// Create load generating units
		createLoadGeneratingUnits(executor);
	}

	private IScoreboard createScoreboard() throws JSONException, Exception {
		// Create a metric writer
		MetricWriter metricWriter = MetricWriterFactory.createMetricWriter(config.metricWriterParams
				.getString(MetricWriter.CFG_TYPE_KEY), config.metricWriterParams);

		// Create scoreboard
		Class<IScoreboard> scoreboardClass = (Class<IScoreboard>) Class.forName("TODO");
		Constructor<IScoreboard> scoreboardCtor = scoreboardClass.getConstructor(String.class);
		IScoreboard scoreboard = (IScoreboard) scoreboardCtor.newInstance("TODO");

		// Set the log sampling probability for the scoreboard
		scoreboard.initialize(timing);
		scoreboard.setScenarioTrack(this);
		scoreboard.setLogSamplingProbability(config.logSamplingProbability);
		scoreboard.setUsingMetricSnapshots(config.useMetricSnapshots);
		scoreboard.setMeanResponseTimeSamplingInterval(config.meanResponseTimeSamplingInterval);
		scoreboard.setMetricSnapshotInterval((long) (config.metricSnapshotInterval * 1000));
		scoreboard.setMetricWriter(metricWriter);
		scoreboard.start();

		return scoreboard;
	}

	@SuppressWarnings("unchecked")
	private LoadScheduleCreator createLoadScheduleCreator() throws Exception {
		Class<LoadScheduleCreator> creatorClass = (Class<LoadScheduleCreator>) Class
				.forName(config.loadScheduleCreatorClass);
		Constructor<LoadScheduleCreator> creatorCtor = creatorClass.getConstructor(new Class[] {});
		return (LoadScheduleCreator) creatorCtor.newInstance((Object[]) null);
	}

	private void createLoadGeneratingUnits(ExecutorService executor) throws Exception {
		// Determine maximum number of required lg units that are required by the schedule
		long maxGenerators = loadSchedule.getMaxGenerators();

		// Create all lg units
		for (int i = 0; i < maxGenerators; i++) {
			// Setup generator
			Generator generator = createGenerator();
			generator.setScoreboard(scoreboard);
			generator.setMeanCycleTime((long) (config.meanCycleTime * 1000));
			generator.setMeanThinkTime((long) (config.meanThinkTime * 1000));
			generator.initialize();

			// Allow the load generation strategy to be configurable
			LoadGeneratingUnit lgUnit = createLoadGenerationStrategy(i, generator);
			lgUnit.setExecutorService(executor);
			lgUnit.setTimeStarted(System.currentTimeMillis());

			// Add thread to thread list and start the thread
			loadGeneratingUnits.add(lgUnit);
		}
	}

	@SuppressWarnings("unchecked")
	private Generator createGenerator() throws Exception {
		Class<Generator> generatorClass = (Class<Generator>) Class.forName(config.generatorClassName);
		Constructor<Generator> generatorCtor = generatorClass.getConstructor();
		Generator generator = (Generator) generatorCtor.newInstance();
		generator.configure(config.generatorParams);
		return generator;
	}

	IScoreboard getScoreboard() {
		return scoreboard;
	}

	TrackConfiguration getConfiguration() {
		return config;
	}
}
