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

import java.util.concurrent.ExecutorService;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The LoadGenerationStrategy abstract class is a basic thread that keeps track of its state (waiting to begin, active,
 * or inactive) and associates itself with a generator that creates operations.
 */
public abstract class LoadGenerationStrategy extends Thread {
	private static Logger logger = LoggerFactory.getLogger(LoadGenerationStrategy.class);

	public enum ThreadStates {
		// Waiting until the start time to begin
		WaitingToBegin,
		// Is active
		Active,
		// Thread is sleeping
		Inactive
	}

	// Constants
	public static final int NO_OPERATION_INDEX = -1;
	public static final int TIME_NOT_SET = -1;

	// The generator used to create operations for this thread
	protected Generator generator;

	// Timings
	protected long timeStarted = TIME_NOT_SET;
	protected long startSteadyState = TIME_NOT_SET;
	protected long endSteadyStatete = TIME_NOT_SET;
	protected long timeToQuitit = TIME_NOT_SET;

	// The current state of this thread
	protected ThreadStates threadState = ThreadStates.WaitingToBegin;

	// Determine whether we async requests should be limited/throttled down to a max of x/sec
	protected long sendNextRequest = NO_OPERATION_INDEX;
	protected LoadUnit lastLoadUnit = null;

	// The shared pool of worker threads
	protected ExecutorService sharedWorkPool;

	public LoadGenerationStrategy(Generator generator) {
		this.generator = generator;
	}

	public LoadGenerationStrategy(Generator generator, JSONObject params) {
		this.generator = generator;
	}

	public abstract void run();

	public abstract void dispose();

	public void resetRateLimitCounters() {
		sendNextRequest = NO_OPERATION_INDEX;
	}

	private void runAsyncOperation(Operation operation) {
		LoadUnit loadUnit = operation.getGeneratedDuringProfile();

		// Load unit unspecified - execute operation immediately
		if (loadUnit == null) {
			sharedWorkPool.submit(operation);
			return;
		}

		// No rate limiting - execute operation immediately
		long aggRatePerSec = loadUnit.openLoopMaxOpsPerSec;
		if (aggRatePerSec == 0) {
			sharedWorkPool.submit(operation);
			return;
		}

		// Rate limited send
		long activeUsers = loadUnit.numberOfUsers;
		long now = System.currentTimeMillis();

		// Is it time to send the request?
		if (now >= sendNextRequest) {
			double myRate = (aggRatePerSec) / (double) activeUsers;
			if (myRate <= 0)
				myRate = 1000.0;
			double waitIntervalMsecs = (1000.0 / myRate);
			sendNextRequest = System.currentTimeMillis() + (long) waitIntervalMsecs;

			// Submit operation
			sharedWorkPool.submit(operation);
		} else {
			long sleepTime = sendNextRequest - now;
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				logger.error(getName() + " interrupted from sleep", e);
			}

			// Submit operation
			sharedWorkPool.submit(operation);
		}
	}

	/**
	 * Called to execute an operation. Depending on its type its executed in a synchronous or asynchronous fashion.
	 * 
	 * @param operation
	 *            The operation to execute
	 */
	public void doOperation(Operation operation) {
		// Set the time the operation was queued (not how long it takes).
		operation.setTimeQueued(System.currentTimeMillis());

		if (!operation.getAsync()) { // Synchronous mode
			operation.run();
		} else { // Asynchronous mode
			runAsyncOperation(operation);
		}
	}
}
