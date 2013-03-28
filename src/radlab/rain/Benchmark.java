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

import org.apache.log4j.LogManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.communication.thrift.ThriftService;
import radlab.rain.util.ConfigUtil;
import radlab.rain.util.SonarRecorder;

/**
 * The Benchmark class provides a framework to initialize and run a benchmark specified by a provided scenario.
 */
public class Benchmark {
	private static Logger logger = LoggerFactory.getLogger(Benchmark.class);

	public void start(Scenario scenario) throws Exception {
		Thread.currentThread().setName("benchmark");

		// Execute scenario
		Timing timing = scenario.execute();

		// Wait for scenario to end
		scenario.join();

		// Aggregate scorecards
		scenario.aggregateScorecards(timing);

		// Shutdown Sonar monitoring
		SonarRecorder.getInstance().shutdown();

		// Shutdown thrift server
		if (RainConfig.getInstance().useThrift) {
			logger.debug("Shutting down the thrift communication!");
			ThriftService.getInstance().stop();
		}
	}

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

	/**
	 * Runs the benchmark. The only required argument is the configuration file path (e.g.
	 * config/rain.config.sample.json).
	 */
	public static void main(String[] args) throws Exception {
		try {

			if (args.length < 1) {
				logger.info("Unspecified name/path to configuration file!");
				System.exit(1);
			}

			// Log startup
			logger.info("Rain started");

			// Load configuration
			JSONObject jsonConfig = loadConfiguration(args[0]);

			// Build scenario based on the configuration
			Scenario scenario = new Scenario(jsonConfig);

			// Set the global Scenario instance for the Driver
			Benchmark benchmark = new Benchmark();

			// Start thrift server for remote control
			if (RainConfig.getInstance().useThrift) {
				ThriftService thrift = ThriftService.getInstance();
				logger.info("Starting thrift communication! Using port: " + thrift.getPort());
				thrift.start();
			}

			// Waiting for start signal
			if (RainConfig.getInstance().waitForStartSignal)
				logger.info("Waiting for start signal...");
			while (RainConfig.getInstance().waitForStartSignal) {
				logger.trace("Sleeping for 1sec...");
				Thread.sleep(1000);
				logger.trace("Checking for wakeup");
			}

			// Start signal passed. Start scenario now
			logger.info("Starting scenario (threads)");
			benchmark.start(scenario);

		} catch (Exception e) {
			logger.error("error in benchmark", e);
		} finally {
			logger.info("Rain stopped");
			LogManager.shutdown();
		}
	}
}
