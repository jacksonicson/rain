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
	private Timing timing;

	// Scoreboard
	protected IScoreboard scoreboard;
	protected TrackConfiguration config;
	protected ScenarioConfiguration scenarioConfiguration;

	// Load schedule used by the generator and strategy
	protected LoadDefinition currentLoadUnit;
	protected LoadScheduleCreator loadScheduleCreator;
	protected LoadSchedule loadSchedule;

	// Generates queries
	protected Generator generator;

	// Triggers generator
	protected List<LoadGeneratingUnit> generators = new ArrayList<LoadGeneratingUnit>();

	public Track(ScenarioConfiguration scenarioConfiguration) throws Exception {
		this.scenarioConfiguration = scenarioConfiguration;
	}

	public abstract void start();

	public void end() {
		scoreboard.stop();
	}

	public abstract boolean validateLoadProfile(LoadDefinition profile);

	void initialize(Timing timing, JSONObject jsonConfig) throws Exception {
		// Timings
		this.timing = timing;

		// Load configuration
		config = new TrackConfiguration();
		config.initialize(jsonConfig);

		// Create scoreboard
		scoreboard = createScoreboard();

		// Create load schedule creator
		loadScheduleCreator = createLoadScheduleCreator();

		// Create load schedule
		loadSchedule = loadScheduleCreator.createSchedule(config.loadSchedulerParams);

		// Create load generator
		generator = createGenerator();
	}

	public IScoreboard createScoreboard() throws JSONException, Exception {
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
	public LoadScheduleCreator createLoadScheduleCreator() throws Exception {
		Class<LoadScheduleCreator> creatorClass = (Class<LoadScheduleCreator>) Class
				.forName(config.loadScheduleCreatorClass);
		Constructor<LoadScheduleCreator> creatorCtor = creatorClass.getConstructor(new Class[] {});
		return (LoadScheduleCreator) creatorCtor.newInstance((Object[]) null);
	}

	@SuppressWarnings("unchecked")
	public Generator createGenerator() throws Exception {
		Class<Generator> generatorClass = (Class<Generator>) Class.forName(config.generatorClassName);
		Constructor<Generator> generatorCtor = generatorClass.getConstructor(new Class[] { Track.class });
		Generator generator = (Generator) generatorCtor.newInstance(new Object[] { this });
		generator.configure(config.generatorParams);
		return generator;
	}

	@SuppressWarnings("unchecked")
	public LoadGeneratingUnit createLoadGenerationStrategy(Generator generator) throws Exception {
		Class<LoadGeneratingUnit> loadGenStrategyClass = (Class<LoadGeneratingUnit>) Class
				.forName(config.loadGenerationStrategyClassName);
		Constructor<LoadGeneratingUnit> loadGenStrategyCtor = loadGenStrategyClass.getConstructor(new Class[] {
				Generator.class, long.class, JSONObject.class });
		LoadGeneratingUnit loadGenStrategy = (LoadGeneratingUnit) loadGenStrategyCtor.newInstance(new Object[] {
				generator, config.loadGenerationStrategyParams });
		return loadGenStrategy;
	}

	public void createLoadGenerators(long start, ExecutorService pool, IScoreboard scoreboard) throws Exception {
		long maxGenerators = loadSchedule.getMaxGenerators();
		for (int i = 0; i < maxGenerators; i++) {
			// Setup generator
			Generator generator = createGenerator();
			generator.setScoreboard(scoreboard);
			generator.setMeanCycleTime((long) (config.meanCycleTime * 1000));
			generator.setMeanThinkTime((long) (config.meanThinkTime * 1000));
			generator.initialize();

			// Allow the load generation strategy to be configurable
			LoadGeneratingUnit strategy = createLoadGenerationStrategy(generator);
			strategy.setExecutorService(pool);
			strategy.setTimeStarted(start);

			// Add thread to thread list and start the thread
			generators.add(strategy);
		}
	}

	public void startGenerators() {
		for (LoadGeneratingUnit strategy : generators) {
			strategy.start();
		}
	}

	void join() {
		for (LoadGeneratingUnit generator : generators) {
			try {
				generator.join();
				logger.info("Thread joined: " + generator.getName());
			} catch (InterruptedException ie) {
				logger.error("Main thread interrupted... exiting!");
			} finally {
				generator.dispose();
			}
		}
	}

	IScoreboard getScoreboard() {
		return scoreboard;
	}

	TrackConfiguration getConfiguration() {
		return config;
	}
}
