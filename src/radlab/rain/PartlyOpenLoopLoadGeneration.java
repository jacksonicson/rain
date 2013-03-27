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

import java.util.Random;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PartlyOpenLoopLoadGeneration class is a thread that supports partly open loop load generation.
 */
public class PartlyOpenLoopLoadGeneration extends LoadGenerationStrategy {
	private static Logger logger = LoggerFactory.getLogger(PartlyOpenLoopLoadGeneration.class);

	// Minimum increments of intervals of inactivity in seconds
	public static int INACTIVE_DURATION = 1000;

	// The probability of using open loop vs. closed loop
	protected double openLoopProbability;

	// The random number generator used to decide which loop to use
	protected Random random = new Random();

	// Statistic: number of synchronous operations run
	protected long synchOperations = 0;

	// Statistic: number of asynchronous operations run
	protected long asynchOperations = 0;

	public PartlyOpenLoopLoadGeneration(long id, LoadManager loadManager, Generator generator) {
		super(id, loadManager, generator);
		Thread.setDefaultUncaughtExceptionHandler(new UnexpectedDeathHandler());
	}

	public PartlyOpenLoopLoadGeneration(long id, LoadManager loadManager, Generator generator, JSONObject params) {
		super(id, loadManager, generator, params);
		Thread.setDefaultUncaughtExceptionHandler(new UnexpectedDeathHandler());
	}

	// Resets the number of synchronous/asynchronous operations run
	public void resetStatistics() {
		this.synchOperations = 0;
		this.asynchOperations = 0;
	}

	// Disposes of objects used by this thread
	public void dispose() {
		this.generator.dispose();
	}

	/**
	 * Decides whether this thread is active or not. The number of threads is equal to the maximum number of generators.
	 * The actual number of threads varies over time. This is achieved by blocking some threads. If a thread is blocked
	 * is decided by this function.
	 */
	protected boolean isActive() {
		LoadUnit loadProfile = loadManager.getCurrentLoadProfile();
		return (id < loadProfile.getNumberOfUsers());
	}

	/**
	 * Load generator loop. Runs the generator for each cycle and executes the returned operation.
	 */
	public void run() {
		String threadName = getName();

		// Reset statistics
		resetStatistics();

		// Track configuration
		loadTrackConfiguration();

		try {
			// Sleep until its time to start
			sleepUntil(timeStarted);

			// Last executed operation (required to run markov chains)
			int lastOperationIndex = NO_OPERATION_INDEX;

			// Check if benchmark is still running
			while (System.currentTimeMillis() <= timeToQuit) {
				// If generator is not active
				if (!isActive()) {
					threadState = ThreadStates.Inactive;
					Thread.sleep(INACTIVE_DURATION);
				} else {
					threadState = ThreadStates.Active;

					// Generate next operation
					Operation nextOperation = generator.nextRequest(lastOperationIndex);
					if (nextOperation != null) {
						// Update last operation index.
						lastOperationIndex = nextOperation.getOperationIndex();

						// Store the thread name/ID so we can organize the traces.
						nextOperation.setGeneratedBy(threadName);

						// Decide whether to do things open or closed (throw a coin)
						double randomDouble = random.nextDouble();
						if (randomDouble <= openLoopProbability)
							doAsyncOperation(nextOperation);
						else
							doSyncOperation(nextOperation);
					}
				}
			}
		} catch (InterruptedException ie) {
			logger.error("[" + threadName + "] load generation thread interrupted exiting!");
		} catch (Exception e) {
			logger.error("[" + threadName + "] load generation thread died by exception! Reason: " + e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Runs the provided operation asynchronously and sleeps this thread on the cycle time.
	 */
	protected void doAsyncOperation(Operation operation) throws InterruptedException {
		// Update operation counters
		asynchOperations++;

		// Calculate timings
		long cycleTime = generator.getCycleTime();
		long now = System.currentTimeMillis();
		long wakeUpTime = now + cycleTime;

		// Set async flag
		operation.setAsync(true);

		// Trigger operation
		doOperation(operation);

		// Sleep for cycle time
		if (wakeUpTime > timeToQuit) {
			if (now < startSteadyState) {
				cycleTime = startSteadyState - now;
				sleepUntil(startSteadyState);
			} else {
				cycleTime = timeToQuit - now;
				sleepUntil(timeToQuit);
			}
		} else {
			sleepUntil(wakeUpTime);
		}

		// Save the cycle time - if we're in the steady state
		generator.getScoreboard().dropOffWaitTime(now, operation._operationName, cycleTime);
	}

	/**
	 * Runs the provided operation synchronously and sleeps this thread on the think time.
	 */
	protected void doSyncOperation(Operation operation) throws InterruptedException {
		// Update operation counters
		synchOperations++;

		// Configure operation
		operation.setAsync(false);

		// Trigger operation
		doOperation(operation);

		// Calculate timings
		long thinkTime = generator.getThinkTime();
		long now = System.currentTimeMillis();
		long wakeUpTime = now + thinkTime;

		// Sleep for think time
		if (wakeUpTime > timeToQuit) {
			if (now < startSteadyState) {
				thinkTime = startSteadyState - now;
				sleepUntil(startSteadyState);
			} else {
				thinkTime = timeToQuit - now;
				sleepUntil(timeToQuit);
			}
		} else
			sleepUntil(wakeUpTime);

		// Save the think time
		generator.getScoreboard().dropOffWaitTime(now, operation._operationName, thinkTime);
	}

	/**
	 * Loads the configuration from the provided scenario track. This sets the open loop probability as well as time
	 * markers for when this thread starts, when steady state should begin (i.e. when metrics start recording), and when
	 * steady state ends.
	 * 
	 * @param track
	 *            The track from which to load the configuration.
	 */
	protected void loadTrackConfiguration() {
		this.openLoopProbability = track.getOpenLoopProbability();

		// This value gets set by Benchmark
		if (this.timeStarted == TIME_NOT_SET)
			this.timeStarted = System.currentTimeMillis();

		// Configuration is specified in seconds; convert to milliseconds.
		long rampUp = track.getRampUp() * 1000;
		long duration = track.getDuration() * 1000;
		long rampDown = track.getRampDown() * 1000;

		this.startSteadyState = this.timeStarted + rampUp;
		this.endSteadyState = this.startSteadyState + duration;
		this.timeToQuit = this.endSteadyState + rampDown;
	}

	protected void sleepUntil(long time) throws InterruptedException {
		long preRunSleep = time - System.currentTimeMillis();
		if (preRunSleep > 0)
			Thread.sleep(preRunSleep);
	}
}
