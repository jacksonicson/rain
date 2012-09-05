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
import java.util.Random;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.ErrorSummary;
import radlab.rain.IScoreboard;
import radlab.rain.LoadProfile;
import radlab.rain.OperationExecution;
import radlab.rain.ResponseTimeStat;
import radlab.rain.ScenarioTrack;
import radlab.rain.TraceLabels;
import radlab.rain.util.MetricWriter;
import radlab.rain.util.PoissonSamplingStrategy;

/**
 * The Scoreboard class implements the IScoreboard interface. Each Scoreboard is specific to a single instantiation of a track
 * (i.e. the statistical results of a a scoreboard pertain to the operations executed by only the. scenario track with which this
 * scoreboard is associated).<br />
 * <br />
 * The graphs we want to show/statistics we want to record:
 * <ol>
 * <li>Offered load timeline (in ops or requests per sec in a bucket of time)</li>
 * <li>Offered load during the run (in ops or requests per sec)</li>
 * <li>Effective load during the run (in ops or requests per sec) (avg number of operations/requests that completed successfully
 * during the run duration</li>
 * <li>Data distribution for each operation type - histogram of id's generated/used</li>
 * </ol>
 */
public class Scoreboard implements Runnable, IScoreboard {
	private static Logger log = LoggerFactory.getLogger(Scoreboard.class);

	// Time in seconds to wait for worker thread to exit before interrupt
	public static int WORKER_EXIT_TIMEOUT = 60;

	// Who owns this scoreboard
	private String trackName;
	private String trackTargetHost;
	private ScenarioTrack _owner = null;

	// If true, this scoreboard will refuse any new results.
	// Indicates the thread status (started or stopped)
	private boolean done = false;

	// Random number generator
	private Random _random = new Random();

	// Response time sampling interval (wait time and operation summary)
	private long meanResponseTimeSamplingInterval = 500;

	// Log (trace) sampling probability
	private double _logSamplingProbability = 1.0;

	// Time markers are set in the initialization method
	private long startTime;
	private long endTime;

	// Global counters for dropoffs and dropoff time (time waited for the lock to
	// write a result to a processing queue)
	// They are only used for internal statistics about the scoreboard behavior
	private long totalDropoffs = 0;
	private long totalDropOffWaitTime = 0;
	private long maxDropOffWaitTime = 0;

	// Write metrics using the snapshotThread with the MetricWriter
	private boolean usingMetricSnapshots = false;
	private MetricWriter metricWriter = null;

	// Final scorecard
	// Basically holds all counters relevant for aggregated result statistics
	private Scorecard finalCard = null;

	// Scorecards: For each profile (= interval) there is one scorecard
	// Each element of a workload profile is a profile in rain
	// This scorecards are only generated if per-interval metrics are used
	// Per-interval means, the <code>_generatedDuring</code> flag in OperationExecution is filled
	private TreeMap<String, Scorecard> profileScorecards = new TreeMap<String, Scorecard>();

	// Summary reports for each operation
	private TreeMap<String, ErrorSummary> errorMap = new TreeMap<String, ErrorSummary>();
	private TreeMap<String, WaitTimeSummary> waitTimeMap = new TreeMap<String, WaitTimeSummary>();

	// Dropoff and processing queues
	private LinkedList<OperationExecution> dropOffQ = new LinkedList<OperationExecution>();
	private LinkedList<OperationExecution> processingQ = new LinkedList<OperationExecution>();

	// Lock objects
	private Object swapDropoffQueueLock = new Object();
	private Object waitTimeDropOffLock = new Object();
	private Object errorSummaryDropOffLock = new Object();

	// Threads used to process the queues
	private Thread workerThread = null;
	private SnapshotWriterThread snapshotThread = null;

	/**
	 * Creates a new Scoreboard with the track name specified. The Scoreboard returned must be initialized by calling
	 * <code>initialize</code>.
	 * 
	 * @param trackName
	 *            The track name to associate with this scoreboard.
	 */
	public Scoreboard(String trackName) {
		this.trackName = trackName;
	}

	public void initialize(long startTime, long endTime) {
		this.startTime = startTime;
		this.endTime = endTime;

		long runDuration = this.endTime - this.startTime;
		finalCard = new Scorecard("final", runDuration, this.trackName);

		reset();
	}

	@Override
	public void reset() {
		// Reset final card
		finalCard.reset();

		// Reset Scoreboard
		this.processingQ.clear();
		this.totalDropoffs = 0;
		this.totalDropOffWaitTime = 0;
		this.maxDropOffWaitTime = 0;
		synchronized (this.swapDropoffQueueLock) {
			this.dropOffQ.clear();
		}
		synchronized (this.waitTimeDropOffLock) {
			this.waitTimeMap.clear();
		}
	}

	@Override
	public void dropOffWaitTime(long time, String opName, long waitTime) {
		if (isDone())
			return;
		if (!this.isSteadyState(time))
			return;

		synchronized (this.waitTimeDropOffLock) {
			WaitTimeSummary waitTimeSummary = this.waitTimeMap.get(opName);

			// Create wait time summary if it does not exist
			if (waitTimeSummary == null) {
				waitTimeSummary = new WaitTimeSummary(new PoissonSamplingStrategy(this.meanResponseTimeSamplingInterval));
				this.waitTimeMap.put(opName, waitTimeSummary);
			}

			waitTimeSummary.dropOff(waitTime);
		}
	}

	public void dropOffOperation(OperationExecution result) {
		if (isDone())
			return;

		// Set result label
		if (this.isRampUp(result.getTimeStarted()))
			result.setTraceLabel(TraceLabels.RAMP_UP_LABEL);
		else if (this.isSteadyState(result.getTimeFinished()))
			result.setTraceLabel(TraceLabels.STEADY_STATE_TRACE_LABEL);
		else if (this.isSteadyState(result.getTimeStarted()))
			result.setTraceLabel(TraceLabels.LATE_LABEL);
		else if (this.isRampDown(result.getTimeStarted()))
			result.setTraceLabel(TraceLabels.RAMP_DOWN_LABEL);

		// Put all results into the dropoff queue
		long lockStart = System.currentTimeMillis();
		synchronized (this.swapDropoffQueueLock) {
			long dropOffWaitTime = (System.currentTimeMillis() - lockStart);

			// Update internal dropoff statistics
			this.totalDropOffWaitTime += dropOffWaitTime;
			this.totalDropoffs++;
			if (dropOffWaitTime > this.maxDropOffWaitTime)
				this.maxDropOffWaitTime = dropOffWaitTime;

			// Put this result into the dropoff queue
			this.dropOffQ.add(result);
		}

		// TODO: If operation failed - log error reason

		// Flip a coin to determine whether we log or not? (reduces amount of log information)
		double randomVal = this._random.nextDouble();
		if (this._logSamplingProbability == 1.0 || randomVal <= this._logSamplingProbability) {
			// TODO: If needed file logging can be done here
		} else // not logging
		{
			result.getOperation().disposeOfTrace();
		}

		// ATTENTION: Return operation object to pool
		if (this._owner.getObjectPool().isActive())
			this._owner.getObjectPool().returnObject(result.getOperation());
	}

	private final boolean isDone() {
		return this.done;
	}

	private final boolean isSteadyState(long time) {
		return (time >= this.startTime && time <= this.endTime);
	}

	private final boolean isRampUp(long time) {
		return (time < this.startTime);
	}

	private final boolean isRampDown(long time) {
		return (time > this.endTime);
	}

	public void start() {
		if (!this.isRunning()) {
			this.done = false;

			// Start worker thread
			this.workerThread = new Thread(this);
			this.workerThread.setName("Scoreboard-Worker");
			this.workerThread.start();

			// Start snapshot thread
			if (this.usingMetricSnapshots) {
				if (this.metricWriter == null) {
					log.warn(this + " Metric snapshots disabled - No metric writer instance provided");
				} else {
					this.snapshotThread = new SnapshotWriterThread(this.trackName);
					this.snapshotThread.setMetricWriter(this.metricWriter);
					this.snapshotThread.setName("Scoreboard-Snapshot-Writer");
					this.snapshotThread.start();
				}
			}
		}
	}

	public void stop() {
		if (this.isRunning()) {
			this.done = true;

			// Worker thread
			try {
				// Join worker thread
				log.debug(this + " waiting " + WORKER_EXIT_TIMEOUT + " seconds for worker thread to exit!");
				workerThread.join(WORKER_EXIT_TIMEOUT * 1000);

				// If its still alive try to interrupt it
				if (workerThread.isAlive()) {
					log.debug(this + " interrupting worker thread.");
					workerThread.interrupt();
				}
			} catch (InterruptedException ie) {
				log.info(this + " Interrupted waiting on worker thread exit!");
			}

			// Snapshot thread
			try {
				// Stop snapshot thread
				if (snapshotThread != null) {
					// Set stop flag
					snapshotThread.set_done(true);

					// Wait to join
					log.debug(this + " waiting " + WORKER_EXIT_TIMEOUT + " seconds for snapshot thread to exit!");
					snapshotThread.join(WORKER_EXIT_TIMEOUT * 1000);

					// If its still alive try to interrupt again
					if (snapshotThread.isAlive()) {
						log.debug(this + " interrupting snapshot thread.");
						snapshotThread.interrupt();
					}
				}
			} catch (InterruptedException ie) {
				log.info(this + " Interrupted waiting on snapshot thread exit!");
			}
		}
	}

	/**
	 * Checks whether the worker thread exists and is alive.
	 * 
	 * @return True if the worker thread exists and is alive.
	 */
	protected boolean isRunning() {
		return (this.workerThread != null && this.workerThread.isAlive());
	}

	/**
	 * Implements the worker thread that periodically grabs the results from the dropOffQ and copies it over to the processingQ to
	 * be processed.
	 */
	public void run() {
		log.debug(this + " starting worker thread...");

		// Run as long as the scoreboard is not done
		// Or the dropoff queue still contains entries
		while (!isDone() || !dropOffQ.isEmpty()) {
			if (!dropOffQ.isEmpty()) {

				// Queue swap (dropOffQ with processingQ)
				synchronized (this.swapDropoffQueueLock) {
					LinkedList<OperationExecution> temp = processingQ;
					processingQ = dropOffQ;
					dropOffQ = temp;
				}

				// Process all entries in the working queue
				while (!processingQ.isEmpty()) {
					OperationExecution result = processingQ.remove();
					TraceLabels traceLabel = result.getTraceLabel();

					// Process this operation by its label
					switch (traceLabel) {
					case STEADY_STATE_TRACE_LABEL:
						processSteadyStateResult(result);
						break;
					case LATE_LABEL:
						finalCard.processLateOperation(result);
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
					log.info(this + " worker thread interrupted.");
				}
			}
		}

		// Debugging
		log.debug(this + " drop off queue size: " + this.dropOffQ.size());
		log.debug(this + " processing queue size: " + this.processingQ.size());
		log.debug(this + " worker thread finished!");
	}

	/**
	 * Processes a result (from the processingQ) if it was received during the steady state period.
	 * 
	 * @param result
	 *            The operation execution result to process.
	 */
	private void processSteadyStateResult(OperationExecution result) {
		// Update per-interval (profile) cards
		LoadProfile activeProfile = result._generatedDuring;
		if (activeProfile != null) {
			if ((activeProfile._name != null && activeProfile._name.length() > 0)) {
				// Get scorecard for this interval
				String profileName = activeProfile._name;
				Scorecard profileScorecard = this.profileScorecards.get(profileName);
				// Create a new scorecard if needed
				if (profileScorecard == null) {
					profileScorecard = new Scorecard(profileName, activeProfile._interval, this.trackName);
					profileScorecard.numberOfUsers = activeProfile._numberOfUsers;
					profileScorecards.put(profileName, profileScorecard);
				}

				// Process statistics
				profileScorecard.processProfileResult(result, meanResponseTimeSamplingInterval);
			}
		}

		// Process statistics
		finalCard.processResult(result, meanResponseTimeSamplingInterval);

		// If interactive, look at the total response time.
		if (!result.isFailed() && result.isInteractive()) {
			// Do metric SNAPSHOTS (record all response times)
			// Only save response times if we're doing metric snapshots
			// This reduces memory leakage
			if (this.usingMetricSnapshots)
				issueMetricSnapshot(result);
		}
	}

	private void issueMetricSnapshot(OperationExecution result) {
		long responseTime = result.getExecutionTime();
		ResponseTimeStat responseTimeStat = this.snapshotThread.provisionRTSObject();
		if (responseTimeStat == null)
			responseTimeStat = new ResponseTimeStat();

		// Fill response time stat
		responseTimeStat._timestamp = result.getTimeFinished();
		responseTimeStat._responseTime = responseTime;
		responseTimeStat._totalResponseTime = this.finalCard.totalOpResponseTime;
		responseTimeStat._numObservations = this.finalCard.totalOpsSuccessful;
		responseTimeStat._operationName = result._operationName;
		responseTimeStat._trackName = this.trackName;
		responseTimeStat._operationRequest = result._operationRequest;

		if (result._generatedDuring != null)
			responseTimeStat._generatedDuring = result._generatedDuring._name;

		// Push this stat onto a Queue for the snapshot thread
		this.snapshotThread.accept(responseTimeStat);
	}

	public JSONObject getStatistics() throws JSONException {
		// Run duration in seconds
		double runDuration = (double) (this.endTime - this.startTime);

		double averageDropOffQTime = 0;
		if (totalDropoffs > 0)
			averageDropOffQTime = (double) totalDropOffWaitTime / (double) totalDropoffs;

		// Results
		JSONObject result = new JSONObject();
		result.put("track", trackName);
		result.put("target_host", trackTargetHost);
		result.put("run_duration", runDuration);
		result.put("total_drop_offs", totalDropoffs);
		result.put("average_drop_off_q_time", averageDropOffQTime);
		result.put("max_drop_off_q_time", maxDropOffWaitTime);
		result.put("mean_response_time_sample_interval", meanResponseTimeSamplingInterval);

		// Add final scorecard statistics
		result.put("final_scorecard", finalCard.getStatistics(runDuration));

		// Add other statistics
		result.put("operation_stats", getOperationStatistics(false));
		result.put("wait_stats", getWaitTimeStatistics(false));

		return result;
	}

	private JSONObject getWaitTimeStatistics(boolean purgePercentileData) throws JSONException {
		JSONObject result = new JSONObject();

		synchronized (this.finalCard.operationMap) {
			JSONArray waits = new JSONArray();
			result.put("waits", waits);

			for (Iterator<String> keys = finalCard.operationMap.keySet().iterator(); keys.hasNext();) {
				String operationName = keys.next();
				WaitTimeSummary waitSummary = waitTimeMap.get(operationName);

				// If there were no values, then the min and max wait times would not have been set so make them to 0
				if (waitSummary.minWaitTime == Long.MAX_VALUE)
					waitSummary.minWaitTime = 0;

				if (waitSummary.maxWaitTime == Long.MIN_VALUE)
					waitSummary.maxWaitTime = 0;

				// Print out the operation summary.
				JSONObject wait = waitSummary.getStatistics();
				wait.put("operation_name", operationName);

				if (purgePercentileData)
					waitSummary.resetSamples();
			}
		}

		return result;
	}

	private JSONObject getOperationStatistics(boolean purgePercentileData) throws JSONException {
		JSONObject result = new JSONObject();

		long totalOperations = finalCard.totalOpsSuccessful + this.finalCard.totalOpsFailed;
		double totalAvgResponseTime = 0.0;
		double totalResponseTime = 0.0;
		long totalSuccesses = 0;

		synchronized (this.finalCard.operationMap) {
			JSONArray operations = new JSONArray();
			result.put("operations", operations);

			for (Iterator<String> keys = finalCard.operationMap.keySet().iterator(); keys.hasNext();) {
				String operationName = keys.next();
				OperationSummary operationSummary = finalCard.operationMap.get(operationName);

				// Update global counters
				totalAvgResponseTime += operationSummary.getAverageResponseTime();
				totalResponseTime += operationSummary.totalResponseTime;
				totalSuccesses += operationSummary.opsSuccessful;

				// If there were no successes, then the min and max response times would not have been set
				// so make them to 0
				if (operationSummary.minResponseTime == Long.MAX_VALUE)
					operationSummary.minResponseTime = 0;

				if (operationSummary.maxResponseTime == Long.MIN_VALUE)
					operationSummary.maxResponseTime = 0;

				// Calculations
				double proportion = 0;
				if (totalOperations > 0)
					proportion = (double) (operationSummary.opsSuccessful + operationSummary.opsFailed) / (double) totalOperations;

				// Print out the operation summary.
				JSONObject operation = operationSummary.getJSONStats();
				operations.put(operation);
				operation.put("operation_name", operationName);
				operation.put("proportion", proportion);

				if (purgePercentileData)
					operationSummary.resetSamples();
			}
		}

		result.put("total_operations", totalOperations);
		result.put("total_avg_response_time", totalAvgResponseTime);
		result.put("total_response_time", totalResponseTime);
		result.put("total_successes", totalSuccesses);

		return result;
	}

	public long getMeanResponseTimeSamplingInterval() {
		return this.meanResponseTimeSamplingInterval;
	}

	public void setMeanResponseTimeSamplingInterval(long val) {
		if (val > 0)
			this.meanResponseTimeSamplingInterval = val;
	}

	public long getStartTimestamp() {
		return this.startTime;
	}

	public long getEndTimestamp() {
		return this.endTime;
	}

	public void setEndTimestamp(long val) {
		this.endTime = val;
	}

	public String getTrackName() {
		return this.trackName;
	}

	public void setTrackName(String val) {
		this.trackName = val;
	}

	public void setLogSamplingProbability(double val) {
		this._logSamplingProbability = val;
	}

	public void setMetricSnapshotInterval(long val) {
		// not supported
	}

	public boolean getUsingMetricSnapshots() {
		return this.usingMetricSnapshots;
	}

	public void setUsingMetricSnapshots(boolean val) {
		this.usingMetricSnapshots = val;
	}

	public MetricWriter getMetricWriter() {
		return this.metricWriter;
	}

	public void setMetricWriter(MetricWriter val) {
		this.metricWriter = val;
	}

	public String getTargetHost() {
		return this.trackTargetHost;
	}

	public void setTargetHost(String val) {
		this.trackTargetHost = val;
	}

	public Scorecard getFinalScorecard() {
		return this.finalCard;
	}

	public ScenarioTrack getScenarioTrack() {
		return this._owner;
	}

	public void setScenarioTrack(ScenarioTrack owner) {
		this._owner = owner;
	}

	public String toString() {
		return "[SCOREBOARD TRACK: " + this.trackName + "]";
	}
}
