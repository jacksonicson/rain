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

import org.apache.log4j.Logger;

import radlab.rain.communication.thrift.ThriftService;

/**
 * Singleton configuration class
 */
public class RainConfig {
	private static Logger logger = Logger.getLogger(RainConfig.class);

	// What can we configure?
	public boolean verboseErrors = true;

	// Thrift communication server params
	public boolean useThrift = false;
	public int thriftPort = ThriftService.DEFAULT_PORT;

	// Should we wait for a start message before we start the run, default is no
	public boolean waitForStartSignal = true;

	// Host that is running a Sonar collector
	public String sonarHost = "monitor0";
	public String iaasHost = "192.168.96.6";

	// Mean response time sampling interval for the poisson process
	public int meanResponseTimeSamplingInterval = 30;

	// Threshold for the operation response time
	public double rtime_T = 3000;

	// Instance locks
	private static Object singletonLock = new Object();
	private static RainConfig config = null;

	// List of shutdown hooks
	private List<IShutdown> shutdownHooks = new ArrayList<IShutdown>();

	public static RainConfig getInstance() {
		synchronized (singletonLock) {
			if (config == null)
				config = new RainConfig();
		}
		return config;
	}

	public synchronized void register(IShutdown shutdown) {
		this.shutdownHooks.add(shutdown);
	}

	public synchronized void triggerShutdown() {
		logger.info("Running shutdown hooks...");
		for (IShutdown shutdown : shutdownHooks) {
			logger.info("Shutdown hook: " + shutdown.getName());
			shutdown.shutdown();
		}
	}

	private RainConfig() {
		// No one is allowed to create a instance
	}

}
