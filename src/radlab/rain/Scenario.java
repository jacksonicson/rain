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

import radlab.rain.scoreboard.Aggregation;
import radlab.rain.target.ITarget;
import radlab.rain.util.MetricWriterFactory;

public class Scenario {
	private static Logger logger = LoggerFactory.getLogger(Scenario.class);

	private JSONObject config;

	private TargetManager targetManager;

	public Scenario(JSONObject config) throws Exception {
		this.config = config;
	}

	void launch() throws Exception {
		logger.info("Launching scenario..."); 
		
		JSONObject targetFactoryConf = config.getJSONObject("targetFactories");
		JSONObject targetScheduleConf = config.getJSONObject("targetSchedule");
		MetricWriterFactory.Type metricWriterType = MetricWriterFactory.Type.getType(config
				.getString("metricWriterType"));
		JSONObject metricWriterConf = config.getJSONObject("metricWriterConf");

		// Create target schedule
		TargetSchedule schedule = new TargetSchedule(targetScheduleConf, targetFactoryConf);

		// Create target manager
		targetManager = new TargetManager(schedule);
		targetManager.setMetricWriterConf(metricWriterConf);
		targetManager.setMetricWriterType(metricWriterType);

		// Start target manager
		targetManager.start();

		// Wait for manager to join
		targetManager.join();
	}

	public void statAggregation() throws JSONException {
		Aggregation aggregation = new Aggregation();
		aggregation.aggregateScoreboards(targetManager.getAllTargets());
	}

	public List<String> getTargetNames() {
		List<String> names = new ArrayList<String>();
		for (ITarget track : targetManager.getAllTargets()) {
			names.add(track.toString());
		}
		return names;
	}

	public Timing getTiming() {
		return null;
	}
}
