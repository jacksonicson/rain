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
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.util.MetricWriterFactory;

public class Scenario {
	private static Logger logger = LoggerFactory.getLogger(Scenario.class);

	private Timing timing;

	private MetricWriterFactory.Type metricWriterType;
	private JSONObject metricWriterConf;

	private JSONObject targetScheduleConf;
	private JSONObject targetFactoryConf;

	public Scenario(JSONObject config) throws Exception {
		configure(config);
	}

	Timing execute() throws Exception {
		TargetSchedule schedule = new TargetSchedule(targetScheduleConf, targetFactoryConf);
		TargetManager manager = new TargetManager(timing, schedule);
		manager.setMetricWriterConf(metricWriterConf);
		manager.setMetricWriterType(metricWriterType);

		// Start target manager
		manager.start();

		// Wait for manager to join
		manager.join();

		return timing;
	}

	private void configure(JSONObject jsonConfig) throws JSONException, BenchmarkFailedException {
		// Read timing
		JSONObject timing = jsonConfig.getJSONObject(ScenarioConfKeys.TIMING_KEY.toString());
		long rampUp = timing.getLong(ScenarioConfKeys.RAMP_UP_KEY.toString()) * 1000;
		long duration = timing.getLong(ScenarioConfKeys.DURATION_KEY.toString()) * 1000;
		long rampDown = timing.getLong(ScenarioConfKeys.RAMP_DOWN_KEY.toString()) * 1000;
		this.timing = new Timing(rampUp, duration, rampDown);

		// Target factory configuration
		this.targetFactoryConf = jsonConfig.getJSONObject("targetFactories");

		// Target schedule configuration
		this.targetScheduleConf = jsonConfig.getJSONObject("targetSchedule");

		// Metric writer configuration
		metricWriterType = MetricWriterFactory.Type.getType(jsonConfig.getString(ScenarioConfKeys.METRIC_WRITER_TYPE
				.toString()));
		metricWriterConf = jsonConfig.getJSONObject(ScenarioConfKeys.METRIC_WRITER_CONF.toString());
	}

	public void statAggregation(Timing timing) throws JSONException {
		// Aggregation aggregation = new Aggregation();
		// aggregation.aggregateScoreboards(timing, targets);
	}

	public List<String> getTargetNames() {
		List<String> names = new ArrayList<String>();
		// for (ITarget track : targets) {
		// names.add(track.toString());
		// }
		return names;
	}

	public Timing getTiming() {
		return this.timing;
	}
}
