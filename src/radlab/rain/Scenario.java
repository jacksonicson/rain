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
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.configuration.TrackConfKeys;
import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.scoreboard.Scorecard;

public class Scenario {
	private static Logger logger = LoggerFactory.getLogger(Scenario.class);

	private List<Track> tracks = new LinkedList<Track>();

	private ScenarioConfiguration conf;

	public Scenario(ScenarioConfiguration conf) throws Exception {
		this.conf = conf;
	}

	Timing execute() throws Exception {
		// Calculate timing
		Timing timing = new Timing(conf.getRampUp(), conf.getDuration(), conf.getRampDown());

		// Threads
		int sharedThreads = conf.getMaxSharedThreads();
		ExecutorService pool = Executors.newFixedThreadPool(sharedThreads);

		// Build tracks based on static configuration
		buildTracks(timing);

		// Start all tracks
		for (Track track : tracks)
			track.start();

		// Join all running tracks
		for (Track track : tracks)
			track.end();

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

		return timing;
	}

	@SuppressWarnings("unchecked")
	private void buildTracks(Timing timing) throws Exception {
		// For all configured tracks
		for (JSONObject trackConfig : conf.getTrackConfigurations()) {
			String trackClassName = trackConfig.getString(TrackConfKeys.TRACK_CLASS_KEY.toString());
			Class<Track> trackClass = (Class<Track>) Class.forName(trackClassName);

			// Create new track instance
			Constructor<Track> trackCtor = trackClass.getConstructor(new Class[] { Timing.class });
			Track track = (Track) trackCtor.newInstance(timing);
			track.initialize(trackConfig);

			// Add track instance to the results
			tracks.add(track);
		}
	}

	public void aggregateScorecards(Timing timing) throws JSONException {
		TreeMap<String, Scorecard> aggStats = new TreeMap<String, Scorecard>();
		Scorecard globalCard = new Scorecard("global", "global", timing.steadyStateDuration());

		// Shutdown the scoreboards and tally up the results.
		for (Track track : tracks) {
			track.end();

			// Scoreboard of the track
			IScoreboard scoreboard = track.getScoreboard();

			// Write detailed statistics to sonar
			JSONObject stats = scoreboard.getStatistics();
			String strStats = stats.toString();
			logger.info("Track metrics: " + strStats);

			// Get the name of the generator active for this track
			String generatorClassName = track.getConfiguration().generatorClassName;

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
		for (Track track : tracks) {
			names.add(track.toString());
		}
		return names;
	}

	public ScenarioConfiguration getConfig() {
		return conf;
	}
}
