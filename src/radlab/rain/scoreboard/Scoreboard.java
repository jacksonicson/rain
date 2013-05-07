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

package radlab.rain.scoreboard;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.Timing;
import radlab.rain.operation.OperationExecution;

public class Scoreboard extends Thread implements Runnable, IScoreboard {
	private static Logger logger = LoggerFactory.getLogger(Scoreboard.class);

	// Target that owns this scoreboard
	private final long targetId;

	// Target timings
	private Timing timing;

	// Set to terminate the thread
	private boolean running = false;

	// Technical statistics
	private long totalDropoffs = 0;
	private long totalDropOffWaitTime = 0;
	private long maxDropOffWaitTime = 0;

	// Technical statistics of wait time for each operation
	private Map<String, WaitTimeSummary> waitTimeMap = new TreeMap<String, WaitTimeSummary>();

	// Metric writer thread
	private MetricWriterThread metricWriter = null;

	// Dropoff queues
	private List<OperationExecution> dropOffQ = new LinkedList<OperationExecution>();
	private List<OperationExecution> processingQ = new LinkedList<OperationExecution>();

	// Dropoff queue locks
	private Object swapDropoffQueueLock = new Object();
	private Object waitTimeDropOffLock = new Object();

	// All relevant counters are stored in this scorecard
	private Scorecard scorecard = null;

	/**
	 * Each scoreboard is attached to one target
	 */
	public Scoreboard(long targetId) {
		this.targetId = targetId;
	}

	@Override
	public void initialize(Timing timing, long maxUsers) {
		this.timing = timing;

		// Run duration
		long runDuration = timing.steadyStateDuration();
		logger.debug("run duration: " + runDuration);

		// Create a final scorecard
		scorecard = new Scorecard(runDuration);
	}

	@Override
	public void dropOffWaitTime(long time, String operationName, long waitTime) {
		// Scoreboard closed?
		if (!running)
			return;

		// In steady state
		if (!timing.inSteadyState(time))
			return;

		// Dropoff
		synchronized (waitTimeDropOffLock) {
			WaitTimeSummary waitTimeSummary = waitTimeMap.get(operationName);

			// Create wait time summary if necessary
			if (waitTimeSummary == null) {
				IMetricSampler sampler = new DummySamplingStrategy();
				waitTimeSummary = new WaitTimeSummary(sampler);
				waitTimeMap.put(operationName, waitTimeSummary);
			}

			// Dropoff wait time for the operation
			waitTimeSummary.dropOff(waitTime);
		}
	}

	@Override
	public void dropOffOperation(OperationExecution result) {
		// Scoreboard closed?
		if (!running)
			return;

		// Assign label to the operation execution
		if (timing.inRampUp(result.timeStarted))
			result.setTraceLabel(TraceLabels.RAMP_UP_LABEL);
		else if (timing.inSteadyState(result.timeFinished))
			result.setTraceLabel(TraceLabels.STEADY_STATE_TRACE_LABEL);
		else if (timing.inSteadyState(result.timeStarted))
			result.setTraceLabel(TraceLabels.LATE_LABEL);
		else if (timing.inRampDown(result.timeStarted))
			result.setTraceLabel(TraceLabels.RAMP_DOWN_LABEL);

		// Put all results into the dropoff queue
		long lockStart = System.currentTimeMillis();
		synchronized (swapDropoffQueueLock) {
			// Calculate time required to acquire this lock
			long dropOffWaitTime = (System.currentTimeMillis() - lockStart);

			// Update internal dropoff statistics
			totalDropOffWaitTime += dropOffWaitTime;
			totalDropoffs++;
			maxDropOffWaitTime = Math.max(maxDropOffWaitTime, dropOffWaitTime);

			// Dropoff this operation execution
			dropOffQ.add(result);
		}
	}

	public void start() {
		// Thread is running
		running = true;

		// Start snapshot thread
		metricWriter = new MetricWriterThread();
		metricWriter.setName("Scoreboard-Snapshot-Writer");
		metricWriter.start();

		// Start worker thread
		setName("Scoreboard-Worker");
		super.start();
	}

	public void dispose() {
		// Disable running flag
		running = false;

		// Worker thread
		try {
			// Join worker thread
			logger.debug(this + " waiting for worker thread to exit!");
			join(60 * 1000);

			// If its still alive try to interrupt it
			if (isAlive()) {
				logger.debug(this + " interrupting worker thread.");
				interrupt();
			}
		} catch (InterruptedException ie) {
			logger.info(this + " Interrupted waiting on worker thread exit!");
		}

		// Snapshot thread
		try {
			// Stop snapshot thread
			if (metricWriter != null) {
				// Set stop flag
				metricWriter.interrupt();

				// Wait to join
				logger.debug(this + " waiting metric snapshot writer thread to join");
				metricWriter.join(60 * 1000);

				// If its still alive try to interrupt again
				if (metricWriter.isAlive()) {
					logger.debug(this + " interrupting snapshot thread.");
					metricWriter.interrupt();
				}
			}
		} catch (InterruptedException ie) {
			logger.info(this + " Interrupted waiting on snapshot thread exit!");
		}
	}

	@Override
	public void run() {
		logger.info(this + " starting scoreboard worker thread...");

		// Run as long as the scoreboard is not done or the dropoff queue still contains entries
		while (running || !dropOffQ.isEmpty()) {
			if (!dropOffQ.isEmpty()) {

				// Queue swap (dropOffQ with processingQ)
				synchronized (swapDropoffQueueLock) {
					List<OperationExecution> temp = processingQ;
					processingQ = dropOffQ;
					dropOffQ = temp;
				}

				// Process all entries in the working queue
				while (!processingQ.isEmpty()) {
					OperationExecution result = processingQ.remove(0);
					TraceLabels traceLabel = result.getTraceLabel();

					// Process this operation by its label
					switch (traceLabel) {
					case STEADY_STATE_TRACE_LABEL:
						processSteadyStateResult(result);
						break;
					case LATE_LABEL:
						processLateStateResult(result);
						break;
					default:
						// Not processed
						break;
					}
				}
			} else {
				// Wait some time, until the dropOffQ fills up
				try {
					Thread.sleep(1000);
				} catch (InterruptedException tie) {
					logger.info(this + " worker thread interrupted.");
				}
			}
		}

		// Debugging
		logger.debug(this + " drop off queue size (should be 0): " + dropOffQ.size());
		logger.debug(this + " processing queue size (should be 0): " + processingQ.size());
		logger.debug(this + " worker thread finished!");
	}

	private void processLateStateResult(OperationExecution result) {
		scorecard.processLateOperation(result);
	}

	private void processSteadyStateResult(OperationExecution result) {
		// Process statistics
		scorecard.processResult(result);

		// If interactive, look at the total response time.
		if (!result.failed)
			issueMetricSnapshot(result);
	}

	private void issueMetricSnapshot(OperationExecution result) {
		// If snapshot thread doesn't exist
		if (metricWriter == null)
			return;

		long responseTime = result.getExecutionTime();

		// Transferable stat object
		ResponseTimeStat responseTimeStat = new ResponseTimeStat(result.timeFinished, responseTime, scorecard
				.getTotalOpResponseTime(), scorecard.getTotalOpsSuccessful(), result.operationName,
				result.operationRequest, targetId);

		// Accept stat object
		metricWriter.accept(responseTimeStat);
	}

	@Override
	public JSONObject getStatistics() throws JSONException {
		double averageDropOffQTime = 0;
		if (totalDropoffs > 0)
			averageDropOffQTime = (double) totalDropOffWaitTime / (double) totalDropoffs;

		// Results
		JSONObject result = new JSONObject();
		result.put("target_id", targetId);
		result.put("run_duration", timing.steadyStateDuration());
		result.put("start_time", timing.startSteadyState);
		result.put("end_time", timing.endSteadyState);
		result.put("total_dropoff_wait_time", totalDropOffWaitTime);
		result.put("total_dropoffs", totalDropoffs);
		result.put("average_drop_off_q_time", averageDropOffQTime);
		result.put("max_drop_off_q_time", maxDropOffWaitTime);

		// Add final scorecard statistics
		result.put("final_scorecard", scorecard.getSummarizedStatistics(timing.steadyStateDuration()));

		// Add other statistics
		result.put("wait_stats", getWaitTimeStatistics());

		return result;
	}

	private JSONObject getWaitTimeStatistics() throws JSONException {
		JSONObject result = new JSONObject();

		synchronized (scorecard) {
			JSONArray waits = new JSONArray();
			result.put("waits", waits);

			for (Iterator<String> keys = scorecard.getOperationMap().keySet().iterator(); keys.hasNext();) {
				String operationName = keys.next();

				// Wait time summary for the operation
				WaitTimeSummary waitSummary = waitTimeMap.get(operationName);

				// Print out the operation summary.
				JSONObject wait = waitSummary.getStatistics();
				wait.put("operation_name", operationName);
				waits.put(wait);
			}
		}

		return result;
	}

	@Override
	public Scorecard getScorecard() {
		return scorecard;
	}

	public String toString() {
		return "scoreboard of target-" + targetId + ": ";
	}
}
