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

package radlab.rain.workload.http;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.TrackConfiguration;
import radlab.rain.TracksConfigurationCreator;
import radlab.rain.configuration.TrackConfKeys;

public class HttpTestTrackConfCreator extends TracksConfigurationCreator {

	enum ConfKeys {
		BASE_HOST_IP_KEY("baseHostIp"), NUM_HOST_TARGETS_KEY("numHostTargets"), RESOURCE_PATH("resourcePath");

		private final String value;

		private ConfKeys(String value) {
			this.value = value;
		}

		public String toString() {
			return value;
		}
	}

	public TrackConfiguration createConfiguration(JSONObject params) throws JSONException {
		TrackConfiguration config = new TrackConfiguration(); 
		
		JSONObject trackConfig = new JSONObject();

		String baseHostIP = params.getString(ConfKeys.BASE_HOST_IP_KEY.toString());
		int numHostTargets = params.getInt(ConfKeys.NUM_HOST_TARGETS_KEY.toString());

		// Create a number of targets
		for (int i = 0; i < numHostTargets; i++) {
			JSONObject trackDetails = new JSONObject();

			// Fill in details
			trackDetails.put(TrackConfKeys.GENERATOR_KEY.toString(), "radlab.rain.workload.httptest.HttpTestGenerator");
			trackDetails.put(TrackConfKeys.TRACK_CLASS_KEY.toString(), "radlab.rain.DefaultScenarioTrack");
			trackDetails.put(ConfKeys.RESOURCE_PATH.toString(), "resources/");

			JSONObject behaviorDetails = new JSONObject();

			// Create an array for each row
			JSONArray row1 = new JSONArray(new int[] { 0, 100, 0, 0, 0, 0, 0 });
			JSONArray row2 = new JSONArray(new int[] { 0, 0, 100, 0, 0, 0, 0 });
			JSONArray row3 = new JSONArray(new int[] { 0, 0, 5, 60, 10, 15, 10 });
			JSONArray row4 = new JSONArray(new int[] { 0, 0, 10, 5, 35, 40, 10 });
			JSONArray row5 = new JSONArray(new int[] { 0, 0, 25, 45, 5, 20, 5 });
			JSONArray row6 = new JSONArray(new int[] { 0, 0, 40, 30, 5, 20, 5 });
			JSONArray row7 = new JSONArray(new int[] { 0, 0, 25, 20, 10, 40, 5 });

			// Now create a JSONArray which stores each row
			JSONArray mix1 = new JSONArray();
			mix1.put(row1);
			mix1.put(row2);
			mix1.put(row3);
			mix1.put(row4);
			mix1.put(row5);
			mix1.put(row6);
			mix1.put(row7);

			// Associate a mix matrix with a tag/name
			behaviorDetails.put("default", mix1);

			// Store the behavior details in the track config
			trackDetails.put(TrackConfKeys.BEHAVIOR_KEY.toString(), behaviorDetails);

			// Specifiy the load creator class
			trackDetails.put(TrackConfKeys.LOAD_SCHEDULE_CREATOR_KEY.toString(),
					"radlab.rain.workload.httptest.HttpTestScheduleCreator");

			JSONObject targetDetails = new JSONObject();

			// Get base IP, split on . get last octet convert to int then add i
			String[] ipAddressParts = baseHostIP.split("\\.");
			if (ipAddressParts.length != 4)
				throw new JSONException("Expected numerical IPv4 address format: N.N.N.N");

			int lastOctet = Integer.parseInt(ipAddressParts[3]);
			StringBuffer targetIPAddress = new StringBuffer();
			targetIPAddress.append(ipAddressParts[0]);
			targetIPAddress.append(".");
			targetIPAddress.append(ipAddressParts[1]);
			targetIPAddress.append(".");
			targetIPAddress.append(ipAddressParts[2]);
			targetIPAddress.append(".");
			targetIPAddress.append((lastOctet + i));

			System.out.println("Target IP: " + targetIPAddress.toString());

			targetDetails.put(TrackConfKeys.TARGET_HOSTNAME_KEY.toString(), targetIPAddress.toString());
			targetDetails.put(TrackConfKeys.TARGET_PORT_KEY.toString(), 8080);

			trackDetails.put(TrackConfKeys.TARGET_KEY.toString(), targetDetails);
			trackDetails.put(TrackConfKeys.LOG_SAMPLING_PROBABILITY_KEY.toString(), 0.0); // No log sampling
			trackDetails.put(TrackConfKeys.OPEN_LOOP_PROBABILITY_KEY.toString(), 0.0);
			trackDetails.put(TrackConfKeys.MEAN_CYCLE_TIME_KEY.toString(), 0);
			trackDetails.put(TrackConfKeys.MEAN_THINK_TIME_KEY.toString(), 0);

			// Set response time sampling interval - should be tuned based on the expected
			// order of the expected number of operations/requests that will be issued/served
			// e.g. lower values if we're doing a short run with few operations and
			// larger values if we're doing a long run with many operations so we reduce
			// memory overhead of storing samples
			trackDetails.put(TrackConfKeys.MEAN_RESPONSE_TIME_SAMPLE_INTERVAL.toString(), 50);

			String trackName = "track" + i;
			trackConfig.put(trackName, trackDetails);
		}

		return trackConfig;
	}
}


/*
 * 
 * 	public void initialize(JSONObject config) throws JSONException, Exception {
		// Open-Loop Probability
		openLoopProbability = config.getDouble(TrackConfKeys.OPEN_LOOP_PROBABILITY_KEY.toString());

		// Target Information
		JSONObject target = config.getJSONObject(TrackConfKeys.TARGET_KEY.toString());
		targetHostname = target.getString(TrackConfKeys.TARGET_HOSTNAME_KEY.toString());
		targetPort = target.getInt(TrackConfKeys.TARGET_PORT_KEY.toString());

		// Concrete Generator
		generatorClassName = config.getString(TrackConfKeys.GENERATOR_KEY.toString());
		if (config.has(TrackConfKeys.GENERATOR_PARAMS_KEY.toString()))
			generatorParams = config.getJSONObject(TrackConfKeys.GENERATOR_PARAMS_KEY.toString());

		// Log Sampling Probability
		logSamplingProbability = config.getDouble(TrackConfKeys.LOG_SAMPLING_PROBABILITY_KEY.toString());

		// Mean Cycle Time
		meanCycleTime = config.getDouble(TrackConfKeys.MEAN_CYCLE_TIME_KEY.toString());

		// Mean Think Time
		meanThinkTime = config.getDouble(TrackConfKeys.MEAN_THINK_TIME_KEY.toString());

		// Concrete Load Profile and Load Profile Array
		if (config.has(TrackConfKeys.LOAD_PROFILE_CLASS_KEY.toString()))
			loadProfileClassName = config.getString(TrackConfKeys.LOAD_PROFILE_CLASS_KEY.toString());
		else
			loadProfileClassName = DEFAULT_LOAD_PROFILE_CLASS;

		// Create the load schedule creator
		loadScheduleCreatorClass = config.getString(TrackConfKeys.LOAD_SCHEDULE_CREATOR_KEY.toString());

		// Look for load scheduler parameters if any exist
		if (config.has(TrackConfKeys.LOAD_SCHEDULE_CREATOR_PARAMS_KEY.toString()))
			loadSchedulerParams = config.getJSONObject(TrackConfKeys.LOAD_SCHEDULE_CREATOR_PARAMS_KEY.toString());

		// Load Mix Matrices/Behavior Directives
		JSONObject behavior = config.getJSONObject(TrackConfKeys.BEHAVIOR_KEY.toString());
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

		// Snapshot interval
		if (config.has(TrackConfKeys.METRIC_SNAPSHOT_INTERVAL.toString()))
			metricSnapshotInterval = config.getDouble(TrackConfKeys.METRIC_SNAPSHOT_INTERVAL.toString());

		// Snapshot file suffix
		if (config.has(TrackConfKeys.METRIC_SNAPSHOT_FILE_SUFFIX.toString()))
			metricSnapshotFileSuffix = config.getString(TrackConfKeys.METRIC_SNAPSHOT_FILE_SUFFIX.toString());

		if (config.has(TrackConfKeys.METRIC_SNAPSHOT_CONFIG.toString()))
			metricWriterParams = config.getJSONObject(TrackConfKeys.METRIC_SNAPSHOT_CONFIG.toString());

		// Configure the response time sampler
		if (config.has(TrackConfKeys.MEAN_RESPONSE_TIME_SAMPLE_INTERVAL.toString()))
			meanResponseTimeSamplingInterval = config.getLong(TrackConfKeys.MEAN_RESPONSE_TIME_SAMPLE_INTERVAL
					.toString());

		// Configure the maxUsers if specified
		if (config.has(TrackConfKeys.MAX_USERS.toString()))
			maxUsersFromConfig = config.getInt(TrackConfKeys.MAX_USERS.toString());

		// Look for a load generation strategy
		loadGenerationStrategyClassName = config.getString(TrackConfKeys.LOAD_GENERATION_STRATEGY_KEY.toString());

		// Check for parameters
		if (config.has(TrackConfKeys.LOAD_GENERATION_STRATEGY_PARAMS_KEY.toString()))
			loadGenerationStrategyParams = config.getJSONObject(TrackConfKeys.LOAD_GENERATION_STRATEGY_PARAMS_KEY
					.toString());
	}
 * 
 */
*/