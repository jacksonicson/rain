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

import java.io.FileWriter;
import java.util.Random;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PartlyOpenLoopLoadGeneration class is a thread that supports partly open loop load generation.
 */
public class PartlyOpenLoopLoadGeneration extends LoadGenerationStrategy {

	private static Logger logger = LoggerFactory.getLogger(PartlyOpenLoopLoadGeneration.class);

	/** Minimum increments of intervals of inactivity in seconds. */
	public static int INACTIVE_DURATION = 1000;

	/** The probability of using open loop vs. closed loop. */
	protected double _openLoopProbability;

	/** The random number generator used to decide which loop to use. */
	protected Random _random = new Random();

	/** Statistic: number of synchronous operations run. */
	protected long _synchOperations = 0;

	/** Statistic: number of asynchronous operations run. */
	protected long _asynchOperations = 0;

	/**
	 * Creates a load generation thread that supports partly open loop.
	 * 
	 * @param generator
	 *            The generator used to generate this thread's load.
	 * @param id
	 *            The ID of this thread; used to sleep it on demand.
	 */
	public PartlyOpenLoopLoadGeneration(Generator generator, long id) {
		super(generator, id);

		// If a thread dies for some reason (e.g. the JVM runs out of heap
		// space, which causes an Error not an Exception), use our uncaught
		// exception handler to catch it and print some useful debugging info.
		Thread.setDefaultUncaughtExceptionHandler(new UnexpectedDeathHandler());
	}

	/**
	 * Creates a load generation thread that supports partly open loop.
	 * 
	 * @param generator
	 *            The generator used to generate this thread's load.
	 * @param id
	 *            The ID of this thread; used to sleep it on demand.
	 * @param params
	 *            Additional configuration parameters.
	 */
	public PartlyOpenLoopLoadGeneration(Generator generator, long id, JSONObject params) {
		super(generator, id, params);

		// If a thread dies for some reason (e.g. the JVM runs out of heap
		// space, which causes an Error not an Exception), use our uncaught
		// exception handler to catch it and print some useful debugging info.
		Thread.setDefaultUncaughtExceptionHandler(new UnexpectedDeathHandler());
	}

	/** Resets the number of synchronous/asynchronous operations run. */
	public void resetStatistics() {
		this._synchOperations = 0;
		this._asynchOperations = 0;
	}

	/** Disposes of objects used by this thread. */
	public void dispose() {
		this._generator.dispose();
	}

	/** Runs this partly open loop load generation thread. */
	public void run() {
		String threadName = this.getName();
		this.resetStatistics();

		// Calculates all benchmark times
		this.loadTrackConfiguration(this._generator.getTrack());

		try {
			// Sleep until its time to start
			this.sleepUntil(this._timeStarted);

			// loop is active until after the ramp down phase
			int lastOperationIndex = NO_OPERATION_INDEX;
			while (System.currentTimeMillis() <= this._timeToQuit) {
				// If the user is inactive
				if (!this.isActive()) {
					this._lgState = LGState.Inactive;
					Thread.sleep(INACTIVE_DURATION);
				} else { // user is active
					this._lgState = LGState.Active;
					Operation nextOperation = this._generator.nextRequest(lastOperationIndex);
					// This will let generators do no-ops by returning null.
					// We might end up making sure that we count/account for the no-ops
					if (nextOperation != null) {
						// Update last operation index.
						lastOperationIndex = nextOperation.getOperationIndex();

						// Store the thread name/ID so we can organize the traces.
						nextOperation.setGeneratedBy(threadName);
						nextOperation.setGeneratorThreadID(this._id);

						// Decide whether to do things open or closed
						double randomDouble = this._random.nextDouble();
						if (randomDouble <= this._openLoopProbability) {
							this.doAsyncOperation(nextOperation);
						} else {
							this.doSyncOperation(nextOperation);
						}
					}
				}
			}
		} catch (InterruptedException ie) {
			logger.info("[" + threadName + "] load generation thread interrupted exiting!");
		} catch (Exception e) {
			logger.info("[" + threadName + "] load generation thread died by exception! Reason: " + e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Runs the provided operation asynchronously and sleeps this thread on the cycle time.
	 * 
	 * @param operation
	 *            The operation to run asynchronously.
	 * 
	 * @throws InterruptedException
	 */
	protected void doAsyncOperation(Operation operation) throws InterruptedException {
		this._asynchOperations++;

		long cycleTime = this._generator.getCycleTime();
		long now = System.currentTimeMillis();
		long wakeUpTime = now + cycleTime;

		operation.setAsync(true);
		this.doOperation(operation);

		if (wakeUpTime > this._timeToQuit) {
			if (now < this._startSteadyState) {
				// logger.info( "[" + this.getName() + "] In rampUp attempt to sleep past end of run! Adjusting." );
				cycleTime = this._startSteadyState - now;
				this.sleepUntil(this._startSteadyState);
			} else {
				// logger.info( "[" + this.getName() + "] Attempt to sleep past end of run! Adjusting." );
				// Revise the cycle time
				cycleTime = this._timeToQuit - now;
				this.sleepUntil(this._timeToQuit);
			}
		} else
			this.sleepUntil(wakeUpTime);

		// Save the cycle time - if we're in the steady state
		this._generator.getScoreboard().dropOffWaitTime(now, operation._operationName, cycleTime);
	}

	/**
	 * Runs the provided operation synchronously and sleeps this thread on the think time.
	 * 
	 * @param operation
	 *            The operation to run synchronously.
	 * 
	 * @throws InterruptedException
	 */
	protected void doSyncOperation(Operation operation) throws InterruptedException {
		this._synchOperations++;

		operation.setAsync(false);
		this.doOperation(operation);

		long thinkTime = this._generator.getThinkTime();
		// logger.info( "[" + this.getName() + "] Think time: " + thinkTime );

		long now = System.currentTimeMillis();
		if ((now + thinkTime) > this._timeToQuit) {
			// If we're in the ramp up period then sleep until the start of
			// steady state
			if (now < this._startSteadyState) {
				// logger.info( "[" + this.getName() + "] In rampUp attempt to sleep past end of run! Adjusting." );
				thinkTime = this._startSteadyState - now;
				this.sleepUntil(this._startSteadyState);
			} else // we're in the steadystate or rampdown
			{
				// logger.info( "[" + this.getName() + "] Attempt to sleep past end of run! Adjusting." );
				// Revise the think time
				thinkTime = this._timeToQuit - now;
				this.sleepUntil(this._timeToQuit);
			}
		} else
			this.sleepUntil(now + thinkTime);

		// Save the think time
		this._generator.getScoreboard().dropOffWaitTime(now, operation._operationName, thinkTime);
	}

	/**
	 * Loads the configuration from the provided scenario track. This sets the open loop probability as well as time markers for
	 * when this thread starts, when steady state should begin (i.e. when metrics start recording), and when steady state ends.
	 * 
	 * @param track
	 *            The track from which to load the configuration.
	 */
	protected void loadTrackConfiguration(Track track) {
		this._openLoopProbability = this._generator.getTrack().getOpenLoopProbability();

		// This value gets set by Benchmark
		if (this._timeStarted == TIME_NOT_SET)
			this._timeStarted = System.currentTimeMillis();

		// Configuration is specified in seconds; convert to milliseconds.
		long rampUp = track.getRampUp() * 1000;
		long duration = track.getDuration() * 1000;
		long rampDown = track.getRampDown() * 1000;

		this._startSteadyState = this._timeStarted + rampUp;
		this._endSteadyState = this._startSteadyState + duration;
		this._timeToQuit = this._endSteadyState + rampDown;
	}

	/**
	 * Sleep this thread until the provided time if this thread is being run in interactive mode. No point in sleeping if we are
	 * simply generating a trace.
	 * 
	 * @param time
	 *            The time to wake up.
	 * 
	 * @throws InterruptedException
	 */
	protected void sleepUntil(long time) throws InterruptedException {
		if (this._interactive) {
			long preRunSleep = time - System.currentTimeMillis();
			if (preRunSleep > 0) {
				Thread.sleep(preRunSleep);
			}
		}
	}

	/**
	 * Checks whether this thread should be active or not based on the number of active users specified by the current load
	 * profile and this thread's ID number.
	 * 
	 * @return True if this thread should be active; otherwise false.
	 */
	protected boolean isActive() {
		LoadUnit loadProfile = this._generator.getTrack().getCurrentLoadProfile();
		return (this._id < loadProfile.getNumberOfUsers());
	}
}
