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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.scoreboard.Aggregation;
import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.target.ITarget;
import radlab.rain.target.ITargetFactory;
import radlab.rain.util.MetricWriter;
import radlab.rain.util.MetricWriterFactory;

public class Scenario {
	private static Logger logger = LoggerFactory.getLogger(Scenario.class);

	private Timing timing;

	private ITargetFactory targetFactory;

	private MetricWriterFactory.Type metricWriterType;
	private JSONObject metricWriterConf;

	private List<ITarget> targets;

	public Scenario(JSONObject config) throws Exception {
		configure(config);
	}

	Timing execute() throws Exception {
		// Build tracks based on static configuration
		targets = targetFactory.createTargets();
		logger.info("Number of targets: " + targets.size());

		// Configure tracks
		long id = 0;
		for (ITarget target : targets) {
			logger.debug("Initializing target " + target.getId());

			// Create a metric writer
			MetricWriter metricWriter = MetricWriterFactory.createMetricWriter(metricWriterType, metricWriterConf);

			target.setTiming(timing);
			target.setMetricWriter(metricWriter);
			target.init(id++);
		}

		// Start all tracks
		for (ITarget target : targets) {
			logger.debug("Starting target " + target.getId());
			target.start();
		}

		// Wait until all targets joined
		for (ITarget target : targets) {
			logger.debug("Waiting for target to join " + target.getId());
			target.joinAgents();
		}

		// Stop running tracks
		for (ITarget target : targets) {
			logger.debug("Stopping target " + target.getId());
			target.dispose();
		}

		logger.info("Scenario execution ended");

		return timing;
	}

	private void configure(JSONObject jsonConfig) throws JSONException, BenchmarkFailedException {
		// Read timing
		JSONObject timing = jsonConfig.getJSONObject(ScenarioConfKeys.TIMING_KEY.toString());
		long rampUp = timing.getLong(ScenarioConfKeys.RAMP_UP_KEY.toString()) * 1000;
		long duration = timing.getLong(ScenarioConfKeys.DURATION_KEY.toString()) * 1000;
		long rampDown = timing.getLong(ScenarioConfKeys.RAMP_DOWN_KEY.toString()) * 1000;
		this.timing = new Timing(rampUp, duration, rampDown);

		// New track factory
		String targetFacClass = jsonConfig.getString(ScenarioConfKeys.TARGET_FACTORY_CLASS.toString());
		targetFactory = createTargetFactory(targetFacClass);

		// Configure track factory
		JSONObject params = jsonConfig.getJSONObject(ScenarioConfKeys.TARGET_FACTORY_CONF.toString());
		targetFactory.configure(params);

		// Metric writer configuration
		metricWriterType = MetricWriterFactory.Type.getType(jsonConfig.getString(ScenarioConfKeys.METRIC_WRITER_TYPE
				.toString()));
		metricWriterConf = jsonConfig.getJSONObject(ScenarioConfKeys.METRIC_WRITER_CONF.toString());
	}

	@SuppressWarnings("unchecked")
	private ITargetFactory createTargetFactory(String name) throws BenchmarkFailedException {
		try {
			Class<ITargetFactory> creatorClass = (Class<ITargetFactory>) Class.forName(name);
			Constructor<ITargetFactory> creatorCtor = creatorClass.getConstructor(new Class[] {});
			ITargetFactory creator = (ITargetFactory) creatorCtor.newInstance((Object[]) null);
			return creator;
		} catch (Exception e) {
			throw new BenchmarkFailedException("Unable to instantiate track factory from class " + name, e);
		}
	}

	public void statAggregation(Timing timing) throws JSONException {
		List<IScoreboard> scoreboards = new ArrayList<IScoreboard>();
		for (ITarget target : targets) {
			scoreboards.add(target.getScoreboard());
		}
		Aggregation aggregation = new Aggregation();
		aggregation.aggregateScoreboards(timing, scoreboards);
	}

	public List<String> getTargetNames() {
		List<String> names = new ArrayList<String>();
		for (ITarget track : targets) {
			names.add(track.toString());
		}
		return names;
	}

	public Timing getTiming() {
		return this.timing;
	}
}
