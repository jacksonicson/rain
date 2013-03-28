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
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.configuration.ScenarioConfKeys;
import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.scoreboard.Scorecard;
import radlab.rain.util.MetricWriterFactory;

public class Scenario {
	private static Logger logger = LoggerFactory.getLogger(Scenario.class);

	private Timing timing;

	private TargetFactory trackFactory;

	private MetricWriterFactory.Type metricWriterType;
	private JSONObject metricWriterConf;

	private List<ITarget> tracks;

	public Scenario(JSONObject config) throws Exception {
		configure(config);
	}

	Timing execute() throws Exception {
		// Build tracks based on static configuration
		tracks = trackFactory.createTracks();

		// Configure tracks
		for (ITarget track : tracks) {
			track.setTiming(timing);
			track.setMetricWriter(metricWriterType, metricWriterConf);
			track.init();
		}

		// Start all tracks
		for (ITarget track : tracks)
			track.start();

		// Join all running tracks
		for (ITarget track : tracks)
			track.end();

		return timing;
	}

	private void configure(JSONObject jsonConfig) throws JSONException, BenchmarkFailedException {
		// Read timing
		JSONObject timing = jsonConfig.getJSONObject(ScenarioConfKeys.TIMING_KEY.toString());
		long rampUp = timing.getLong(ScenarioConfKeys.RAMP_UP_KEY.toString());
		long duration = timing.getLong(ScenarioConfKeys.DURATION_KEY.toString());
		long rampDown = timing.getLong(ScenarioConfKeys.RAMP_DOWN_KEY.toString());
		this.timing = new Timing(rampUp, duration, rampDown);

		// New track factory
		String trackConfClass = jsonConfig.getString(ScenarioConfKeys.TARGET_FACTORY_CLASS.toString());
		trackFactory = createTrackFactory(trackConfClass);

		// Configure track factory
		JSONObject params = jsonConfig.getJSONObject(ScenarioConfKeys.TARGET_FACTORY_CONF.toString());
		trackFactory.configure(params);

		// Metric writer configuration
		metricWriterType = MetricWriterFactory.Type.getType(jsonConfig.getString(ScenarioConfKeys.METRIC_WRITER_TYPE
				.toString()));
		metricWriterConf = jsonConfig.getJSONObject(ScenarioConfKeys.METRIC_WRITER_CONF.toString());
	}

	@SuppressWarnings("unchecked")
	private TargetFactory createTrackFactory(String name) throws BenchmarkFailedException {
		try {
			Class<TargetFactory> creatorClass = (Class<TargetFactory>) Class.forName(name);
			Constructor<TargetFactory> creatorCtor = creatorClass.getConstructor(new Class[] {});
			TargetFactory creator = (TargetFactory) creatorCtor.newInstance((Object[]) null);
			return creator;
		} catch (Exception e) {
			throw new BenchmarkFailedException("Unable to instantiate track factory", e);
		}
	}

	public void aggregateScorecards(Timing timing) throws JSONException {
		TreeMap<String, Scorecard> aggStats = new TreeMap<String, Scorecard>();
		Scorecard globalCard = new Scorecard("global", "global", timing.steadyStateDuration());

		// Shutdown the scoreboards and tally up the results.
		for (ITarget track : tracks) {
			// Scoreboard of the track
			IScoreboard scoreboard = track.getScoreboard();

			// Write detailed statistics to sonar
			JSONObject stats = scoreboard.getStatistics();
			String strStats = stats.toString();
			logger.info("Track metrics: " + strStats);

			// Get the name of the generator active for this track
			String generatorClassName = "TODO";

			// Get the final scorecard for this track
			Scorecard finalScorecard = track.getScoreboard().getFinalScorecard();
			if (!aggStats.containsKey(generatorClassName)) {
				Scorecard aggCard = new Scorecard("aggregated", generatorClassName, finalScorecard
						.getIntervalDuration());
				aggStats.put(generatorClassName, aggCard);
			}
			// Get the current aggregated scorecard for this generator
			Scorecard aggCard = aggStats.get(generatorClassName);
			// Merge the final card for this track with the current per-driver
			// aggregated scorecard
			aggCard.merge(finalScorecard);
			aggStats.put(generatorClassName, aggCard);
			// Collect scoreboard results
			// Collect object pool results

			// Merge global card
			globalCard.merge(finalScorecard);
		}

		// Aggregate stats
		if (aggStats.size() > 0)
			logger.info("# aggregated stats: " + aggStats.size());

		for (String generatorName : aggStats.keySet()) {
			Scorecard card = aggStats.get(generatorName);

			// Sonar output
			JSONObject stats = card.getIntervalStatistics();
			String strStats = stats.toString();
			logger.info("Rain metrics: " + strStats);
		}

		// Dump global card
		logger.info("Global metrics: " + globalCard.getIntervalStatistics().toString());
	}

	public List<String> getTrackNames() {
		List<String> names = new ArrayList<String>();
		for (ITarget track : tracks) {
			names.add(track.toString());
		}
		return names;
	}

	public Timing getTiming() {
		return this.timing;
	}
}
