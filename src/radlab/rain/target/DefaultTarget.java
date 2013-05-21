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

package radlab.rain.target;

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

import radlab.rain.BenchmarkFailedException;
import radlab.rain.Timing;
import radlab.rain.agent.IAgent;
import radlab.rain.agent.IAgentFactory;
import radlab.rain.load.LoadDefinition;
import radlab.rain.load.LoadManager;
import radlab.rain.load.LoadSchedule;
import radlab.rain.load.LoadScheduleFactory;
import radlab.rain.operation.Generator;
import radlab.rain.operation.IGeneratorFactory;
import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.scoreboard.Scoreboard;

public abstract class DefaultTarget extends Thread implements ITarget {
	// Logger
	private static Logger logger = LoggerFactory.getLogger(DefaultTarget.class);

	// Target id
	protected long id;

	// Timings
	protected Timing timing;

	// Load manager
	protected LoadManager loadManager;

	// Scoreboard
	protected IScoreboard scoreboard;

	// Load schedule
	protected LoadSchedule loadSchedule;

	// Current load definition
	protected LoadDefinition currentLoadDefinition;

	// Markov chain matrices
	protected Map<String, MixMatrix> mixMatrices = new HashMap<String, MixMatrix>();

	/*
	 * Factories
	 */
	// Generator factory
	protected IGeneratorFactory generatorFactory;

	// Load schedule used by the generator and strategy
	protected LoadScheduleFactory loadScheduleFactory;

	// Agent factory
	protected IAgentFactory agentFactory;

	// Execution times
	protected double openLoopProbability = 0d;
	protected double meanCycleTime = 0;
	protected double meanThinkTime = 0;

	// Sampling
	protected double metricSnapshotInterval = 60.0;
	protected long meanResponseTimeSamplingInterval = 500;

	// List of all load generating units
	protected List<IAgent> agents = new ArrayList<IAgent>();

	// Executer thread pool
	protected ExecutorService executor;

	private ClassLoader cl; 
	
	/*
	 * Abstract methods
	 */
	protected abstract void setup();

	protected abstract void teardown();

	public DefaultTarget(ClassLoader cl) {
		this.cl = cl; 
	}
	
	protected void init() throws BenchmarkFailedException {
		// Recalculate timing based on current timestamp
		timing = new Timing(timing);

		// Create load schedule creator and load schedule
		try {
			loadSchedule = loadScheduleFactory.createSchedule();
		} catch (JSONException e) {
			throw new BenchmarkFailedException("Error while configuring target load schedule", e);
		}

		// Create scoreboard
		scoreboard = createScoreboard();

		// Create a new load manager
		loadManager = new LoadManager(timing, loadSchedule, mixMatrices.keySet());

		// Create a new thread pool
		executor = Executors.newCachedThreadPool();
	}

	private void createAgents(ExecutorService executor) throws Exception {
		// Determine maximum number of required lg units that are required by the schedule
		long maxAgents = loadSchedule.getMaxAgents();

		// Create all agents
		for (int i = 0; i < maxAgents; i++) {
			// Setup generator for each agent
			Generator generator = generatorFactory.createGenerator();
			generator.setMeanCycleTime((long) (meanCycleTime * 1000));
			generator.setMeanThinkTime((long) (meanThinkTime * 1000));
			generator.initialize();

			// Allow the load generation strategy to be configurable
			IAgent agent = agentFactory.createAgent(this.id, i);
			agent.setLoadManager(loadManager);
			agent.setGenerator(generator);
			agent.setTiming(timing);
			agent.setScoreboard(scoreboard);

			// Add thread to thread list and start the thread
			agents.add(agent);
		}
	}

	private void createAgents() {
		try {
			createAgents(executor);
			logger.debug("Agents created: " + agents.size());
		} catch (Exception e) {
			logger.error("Could not create agents", e);
		}
	}

	private void startAgents() {
		logger.info("Starting agents");
		for (IAgent agent : agents)
			agent.start();
	}

	private void joinAgents() {
		for (IAgent agent : agents) {
			try {
				agent.join();
			} catch (InterruptedException e) {
				logger.error("Could not join agent", e);
			}
		}
	}

	public void run() {
		
		setContextClassLoader(cl); 
		
		// Setup target
		logger.info("Running target setup...");
		setup();

		// Initialize
		try {
			init();
		} catch (BenchmarkFailedException e) {
			logger.error("Benchmark failed because target initialization failed", e);
			System.exit(1);
		}

		// Starting load manager
		logger.debug("Starting load manager");
		loadManager.start();

		// Start the scoreboard
		logger.debug("Starting scoreboard");
		scoreboard.start();

		// Create agents
		createAgents();

		// Start agents
		startAgents();

		// Wait for all agents to finish
		joinAgents();

		// Run shutdown code
		logger.info("Running target teardown ...");
		teardown();
	}

	private void disposeAgents() {
		// Shutdown all agent threads
		// This should not be necessary if target was joined
		for (IAgent agent : agents) {
			agent.dispose();
		}
	}

	private void disposeLoadManager() {
		try {
			logger.debug("Shutting down load manager");
			loadManager.interrupt();
			loadManager.join();
			return;
		} catch (InterruptedException e) {
			// ignore
		}
	}

	public void dispose() {
		// Dispose agents
		disposeAgents();

		// Shutdown load manager thread
		disposeLoadManager();

		// Stop the scoreboard
		scoreboard.dispose();
	}

	public void loadConfiguration(JSONObject config) throws JSONException {
		// Open-Loop Probability
		if (config.has("pOpenLoop"))
			openLoopProbability = config.getDouble("pOpenLoop");

		// Mean Cycle Time
		if (config.has("meanCycleTime"))
			meanCycleTime = config.getDouble("meanCycleTime");

		// Mean Think Time
		if (config.has("meanThinkTime"))
			meanThinkTime = config.getDouble("meanThinkTime");

		// Load Mix Matrices/Behavior Directives
		if (config.has("behavior")) {
			JSONObject behavior = config.getJSONObject("behavior");
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
		}
	}

	private IScoreboard createScoreboard() {
		logger.debug("Creating scoreboard for target " + id);

		// Create scoreboard
		IScoreboard scoreboard = new Scoreboard(id);

		// Set the log sampling probability for the scoreboard
		scoreboard.initialize(timing, loadSchedule.getMaxAgents());

		return scoreboard;
	}

	public void setTiming(Timing timing) {
		this.timing = timing;
	}

	public IScoreboard getScoreboard() {
		return scoreboard;
	}

	public void setLoadScheduleFactory(LoadScheduleFactory loadScheduleFactory) {
		this.loadScheduleFactory = loadScheduleFactory;
	}

	public void setAgentFactory(IAgentFactory agentFactory) {
		this.agentFactory = agentFactory;
	}

	public void setGeneratorFactory(IGeneratorFactory generatorFactory) {
		this.generatorFactory = generatorFactory;
	}

	public long getId() {
		return this.id;
	}

	@Override
	public String getAggregationIdentifier() {
		return generatorFactory.createGenerator().getClass().getName();
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}
}
