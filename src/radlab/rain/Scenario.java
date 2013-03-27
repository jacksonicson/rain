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
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.configuration.TrackConfKeys;

public class Scenario {
	private static Logger logger = LoggerFactory.getLogger(Scenario.class);

	private TreeMap<String, Track> tracks = new TreeMap<String, Track>();

	private ScenarioConfiguration conf = new ScenarioConfiguration();

	private Timing timing;

	public Scenario(JSONObject jsonConfig) throws Exception {
		conf.loadProfile(jsonConfig);
	}

	private void buildTracks() throws Exception {
		for (JSONObject trackConfig : conf.getTrackConfigurations()) {
			String trackClassName = trackConfig.getString(TrackConfKeys.TRACK_CLASS_KEY.toString());

			Class<Track> trackClass = (Class<Track>) Class.forName(trackClassName);
			Constructor<Track> trackCtor = trackClass.getConstructor(new Class[] { String.class, Scenario.class });
			Track track = (Track) trackCtor.newInstance();
			track.initialize(trackConfig);

			this.tracks.put("", track);
		}
	}

	void execute() throws Exception {
		// Calculate timing
		timing = new Timing(conf.getRampUp(), conf.getDuration(), conf.getRampDown());

		// Threads
		int sharedThreads = conf.getMaxSharedThreads();
		ExecutorService pool = Executors.newFixedThreadPool(sharedThreads);

		// Build tracks based on static configuration
		buildTracks();

		// Join all running tracks
		for (Track track : tracks.values()) {
			track.join();
		}

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
	}
}
