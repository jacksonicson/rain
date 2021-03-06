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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.communication.thrift.ThriftService;
import radlab.rain.util.ConfigUtil;
import radlab.rain.util.SonarRecorder;

/**
 * The Benchmark class provides a framework to initialize and run a benchmark specified by a provided scenario.
 */
public class Benchmark {
	private static Logger logger = Logger.getLogger(Benchmark.class);

	private static JSONObject loadConfiguration(String filename) {
		try {
			StringBuffer configData = new StringBuffer();
			String fileContents = "";
			// Try to load the config file as a resource first
			InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(filename);
			if (in != null) {
				logger.debug("Reading config file from resource stream.");
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line = "";
				// Read in the entire file and append to the string buffer
				while ((line = reader.readLine()) != null)
					configData.append(line);
				fileContents = configData.toString();
			} else {
				logger.debug("Reading config file from file system.");
				fileContents = ConfigUtil.readFileAsString(filename);
			}
			return new JSONObject(fileContents);
		} catch (IOException e) {
			logger.error("ERROR loading configuration file " + filename + ". Reason: " + e.toString());
			System.exit(1);
		} catch (JSONException e) {
			logger.error("ERROR parsing configuration file " + filename + " as JSON. Reason: " + e.toString());
			System.exit(1);
		}

		return null;
	}

	private static void configureGlobals(JSONObject jsonConfig) throws JSONException {
		// Read global configuration settings
		// Set up Rain configuration params
		if (jsonConfig.has("verboseErrors")) {
			boolean val = jsonConfig.getBoolean("verboseErrors");
			RainConfig.getInstance().verboseErrors = val;
		}

		// Setup sonar recorder
		if (jsonConfig.has("sonarHost")) {
			String host = jsonConfig.getString("sonarHost");
			RainConfig.getInstance().sonarHost = host;
		}

		// Response time threshold
		if (jsonConfig.has("rtime_T")) {
			double rtime = jsonConfig.getDouble("rtime_T");
			RainConfig.getInstance().rtime_T = rtime;
		}

		// Mean sampling interval poisson sampling process
		if (jsonConfig.has("poisson_sampling_mean")) {
			int interval = jsonConfig.getInt("poisson_sampling_mean");
			RainConfig.getInstance().meanResponseTimeSamplingInterval = interval;
		}

		// Check if thrift remote management is used
		boolean useThrift = false;
		if (jsonConfig.has("useThrift"))
			useThrift = jsonConfig.getBoolean("useThrift");

		if (useThrift) {
			// Set in the config that we're using pipes
			RainConfig.getInstance().useThrift = useThrift;

			// Check whether we're supposed to wait for a start signal
			if (jsonConfig.has("waitForStartSignal")) {
				RainConfig.getInstance().waitForStartSignal = jsonConfig.getBoolean("waitForStartSignal");
			}
		}

	}

	public static void main(String[] args) throws Exception {
		try {
			// Check argument length for configuration path
			if (args.length < 1) {
				logger.info("Unspecified name/path to configuration file!");
				System.exit(1);
			}

			// Log startup
			logger.info("Rain started");

			// Load configuration
			JSONObject jsonConfig = loadConfiguration(args[0]);
			configureGlobals(jsonConfig);

			// Build scenario based on the configuration
			Scenario scenario = new Scenario(jsonConfig);

			// Start thrift server for remote control
			ThriftService service = null;
			if (RainConfig.getInstance().useThrift) {
				service = new ThriftService(scenario);
				logger.info("Starting thrift communication! Using port: " + service.getPort());
				service.start();
			}

			// Wait for start signal
			if (RainConfig.getInstance().waitForStartSignal) {
				// Wait until service is available
				Thread.sleep(2000);
				logger.info("Waiting for start signal...");
			}

			// Wait for start signal
			while (RainConfig.getInstance().waitForStartSignal) {
				logger.trace("Sleeping for 1sec...");
				Thread.sleep(1000);
				logger.trace("Checking for wakeup");
			}

			// Set the global Scenario instance for the Driver
			logger.info("Starting scenario (threads)");

			// Set current thread name
			Thread.currentThread().setName("Benchmark");

			// Execute scenario ///////////////////////////////
			// ////////////////////////////////////////////////
			scenario.execute();
			// ////////////////////////////////////////////////

			// Aggregate scorecards
			scenario.runStatisticsAggregation();

			// Trigger shutdown hooks
			RainConfig.getInstance().triggerShutdown();
			
			// Join sonar recorder
			SonarRecorder.getInstance().join();

			if (service != null) {
				logger.info("Stopping thrift communication! Using port: " + service.getPort());
				service.stop();
			}

			// Log joined message
			logger.info("JOINED");

		} catch (JSONException e) {
			logger.error("Error in benchmark configuration", e);
		} catch (BenchmarkFailedException e) {
			logger.error("Benchmark failed ", e);
		} finally {
			// Stop log4j
			logger.info("Stopping log4j...");
			org.apache.log4j.LogManager.shutdown();
		}
	}
}
