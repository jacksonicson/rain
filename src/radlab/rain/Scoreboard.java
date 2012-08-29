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
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.State;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public static String NO_TRACE_LABEL = "[NONE]";
	public static String STEADY_STATE_TRACE_LABEL = "[STEADY-STATE]";
	public static String LATE_LABEL = "[LATE]";
	public static String RAMP_UP_LABEL = "[RAMP-UP]";
	public static String RAMP_DOWN_LABEL = "[RAMP-DOWN]";

	// Time in seconds to wait for worker thread to exit before interrupt
	public static int WORKER_EXIT_TIMEOUT = 60;

	// Random number generator
	private Random _random = new Random();

	// Response time sampling interval
	private long _meanResponseTimeSamplingInterval = 500;
	private static String NEWLINE = System.getProperty("line.separator");

	// Time markers
	private long _startTime = 0;
	private long _endTime = 0;
	private long _totalDropOffWaitTime = 0;
	private long _maxDropOffWaitTime = 0;
	private long _totalDropoffs = 0;
	private boolean _usingMetricSnapshots = false;
	private MetricWriter _metricWriter = null;

	// Scorecards - per-interval scorecards plus the final scorecard
	private TreeMap<String, Scorecard> _intervalScorecards = new TreeMap<String, Scorecard>();
	private Scorecard finalCard = null;

	// Interim stats support
	ObjectPoolGeneric _statsObjPool = null;
	LinkedList<ResponseTimeStat> _responseTimeQ = new LinkedList<ResponseTimeStat>();
	Object _responseTimeQLock = new Object();

	// Log (trace) sampling probability
	private double _logSamplingProbability = 1.0;

	// Track information
	private String _trackName;
	private String _trackTargetHost;
	private ScenarioTrack _owner = null;

	// If true, this scoreboard will refuse any new results.
	private boolean _done = false;

	// Queue that contains all results that have been dropped off.
	private LinkedList<OperationExecution> _dropOffQ = new LinkedList<OperationExecution>();

	// Queue that contains all results that need to be processed
	private LinkedList<OperationExecution> _processingQ = new LinkedList<OperationExecution>();

	// Lock for access to _dropOffQ
	private Object _dropOffQLock = new Object();

	// Lock for access to waitTime table
	private Object _waitTimeDropOffLock = new Object();

	// Lock for error summary table
	private Object _errorSummaryDropOffLock = new Object();

	// TreeMap that contains all the error summaries of failed operations
	private TreeMap<String, ErrorSummary> _errorMap = new TreeMap<String, ErrorSummary>();

	// A mapping of each operation with its wait/cycle time
	private TreeMap<String, WaitTimeSummary> _waitTimeMap = new TreeMap<String, WaitTimeSummary>();

	// Threads for the queue processing
	private Thread _workerThread = null;
	private SnapshotWriterThread _snapshotThread = null;

	/*
	 * How do we handle operations generated by asynchronous worker threads? - Lump all of them together. - Associate them with
	 * the thread that delegated them.
	 */
	private Hashtable<String, FileWriter> _logHandleMap = new Hashtable<String, FileWriter>();
	private Hashtable<String, FileWriter> _errorLogHandleMap = new Hashtable<String, FileWriter>();

	// Formatter is used for the output
	private NumberFormat _formatter = new DecimalFormat("#0.0000");

	/**
	 * Creates a new Scoreboard with the track name specified. The Scoreboard returned must be initialized by calling
	 * <code>initialize</code>.
	 * 
	 * @param trackName
	 *            The track name to associate with this scoreboard.
	 */
	public Scoreboard(String trackName) {
		this._trackName = trackName;

		// Initialize objectpools
		this._statsObjPool = new ObjectPoolGeneric(80000);
		this._statsObjPool.setTrackName(trackName);
	}

	public void initialize(long startTime, long endTime) {
		this._startTime = startTime;
		this._endTime = endTime;

		double runDuration = (double) (this._endTime - this._startTime) / 1000.0;
		this.finalCard = new Scorecard("final", runDuration, this._trackName);

		this.reset();
	}

	@Override
	public void reset() {
		// Clear the operation map
		this.finalCard._operationMap.clear();
		synchronized (this._dropOffQLock) {
			this._dropOffQ.clear();
		}
		this._processingQ.clear();
		synchronized (this._waitTimeDropOffLock) {
			this._waitTimeMap.clear();
		}
		this.finalCard._totalActionsSuccessful = 0;
		this._totalDropoffs = 0;
		this._totalDropOffWaitTime = 0;
		this.finalCard._totalOpsAsync = 0;
		this.finalCard._totalOpsFailed = 0;
		this.finalCard._totalOpsInitiated = 0;
		this.finalCard._totalOpsSuccessful = 0;
		this.finalCard._totalOpsSync = 0;
		this._maxDropOffWaitTime = 0;
		this.finalCard._totalOpsLate = 0;
		this.finalCard._totalOpResponseTime = 0;
	}

	private final boolean isDone() {
		return this._done;
	}

	public void dropOffWaitTime(long time, String opName, long waitTime) {
		if (isDone())
			return;
		if (!this.isSteadyState(time))
			return;

		synchronized (this._waitTimeDropOffLock) {
			WaitTimeSummary waitTimeSummary = this._waitTimeMap.get(opName);

			// Create wait time summary if it does not exist
			if (waitTimeSummary == null) {
				waitTimeSummary = new WaitTimeSummary(new PoissonSamplingStrategy(this._meanResponseTimeSamplingInterval));
				this._waitTimeMap.put(opName, waitTimeSummary);
			}

			// Update wait time summary for this operation
			waitTimeSummary.count++;
			waitTimeSummary.totalWaitTime += waitTime;
			if (waitTime < waitTimeSummary.minWaitTime)
				waitTimeSummary.minWaitTime = waitTime;
			if (waitTime > waitTimeSummary.maxWaitTime)
				waitTimeSummary.maxWaitTime = waitTime;

			// Drop sample
			waitTimeSummary.acceptSample(waitTime);
		}
	}

	public void dropOffOperation(OperationExecution result) {
		if (isDone())
			return;

		// Put all results into the dropoff queue
		long lockStart = System.currentTimeMillis();
		synchronized (this._dropOffQLock) {
			long dropOffWaitTime = (System.currentTimeMillis() - lockStart);

			// Update statistics
			this._totalDropOffWaitTime += dropOffWaitTime;
			this._totalDropoffs++;
			if (dropOffWaitTime > this._maxDropOffWaitTime)
				this._maxDropOffWaitTime = dropOffWaitTime;

			// Put this result into the dropoff queue
			this._dropOffQ.add(result);
		}

		// Set result label
		if (this.isRampUp(result.getTimeStarted()))
			result.setTraceLabel(Scoreboard.RAMP_UP_LABEL);
		else if (this.isSteadyState(result.getTimeFinished()))
			result.setTraceLabel(Scoreboard.STEADY_STATE_TRACE_LABEL);
		else if (this.isSteadyState(result.getTimeStarted()))
			result.setTraceLabel(Scoreboard.LATE_LABEL);
		else if (this.isRampDown(result.getTimeStarted()))
			result.setTraceLabel(Scoreboard.RAMP_DOWN_LABEL);
		}

		// Flip a coin to determine whether we log or not?
		double randomVal = this._random.nextDouble();

		if (this._logSamplingProbability == 1.0 || randomVal <= this._logSamplingProbability) {
			FileWriter logger = null;
			synchronized (this._logHandleMap) {
				logger = this._logHandleMap.get(generatedBy);
			}

			if (logger != null) {
				synchronized (logger) {
					try {
						StringBuffer trace = result.getOperation().dumpTrace();
						if (trace != null && trace.length() > 0) {
							// Don't flush on every write, it kills the load performance
							logger.write(trace.toString() + Scoreboard.NEWLINE);
							// Dumping objects we persisted to disk
							result.getOperation().disposeOfTrace();
						}
					} catch (IOException ioe) {
						result.getOperation().disposeOfTrace();
						log.error("Error writing trace record: Thread name: " + Thread.currentThread().getName()
								+ " on behalf of: " + result.getOperation().getGeneratedBy() + " Reason: " + ioe.toString());
					}
				}
			}
		} else // not logging
		{
			// Discard the trace
			result.getOperation().disposeOfTrace();
		}
		// Return operation object to pool
		if (this._owner.getObjectPool().isActive())
			this._owner.getObjectPool().returnObject(result.getOperation());
	}

	public boolean isSteadyState(long time) {
		return (time >= this._startTime && time <= this._endTime);
	}

	public boolean isRampUp(long time) {
		return (time < this._startTime);
	}

	public boolean isRampDown(long time) {
		return (time > this._endTime);
	}

	public void printStatistics(PrintStream out) {
		double runDuration = (double) (this._endTime - this._startTime) / 1000.0;

		long totalOperations = this.finalCard._totalOpsSuccessful + this.finalCard._totalOpsFailed;

		double offeredLoadOps = 0.0;
		if (totalOperations > 0) {
			offeredLoadOps = (double) this.finalCard._totalOpsInitiated / runDuration;
		}

		double effectiveLoadOps = 0.0;
		if (this.finalCard._totalOpsSuccessful > 0) {
			effectiveLoadOps = (double) this.finalCard._totalOpsSuccessful / runDuration;
		}

		double effectiveLoadRequests = 0.0;
		if (this.finalCard._totalActionsSuccessful > 0) {
			effectiveLoadRequests = (double) this.finalCard._totalActionsSuccessful / runDuration;
		}

		/*
		 * Show... - average ops per second generated (load offered) - total ops/duration - average ops per second completed
		 * (effective load)- total successful ops/duration - average requests per second - async % vs. sync %
		 */

		double totalUsers = 0.0;
		double totalIntervalActivations = 0.0;
		out.println(this + " Interval results-------------------: ");
		// Print out per-interval stats?
		for (Scorecard card : this._intervalScorecards.values()) {
			// Let's look at the load schedule to find the profile that
			// matches the current score card
			for (LoadProfile profile : this._owner._loadSchedule) {
				if (!card._name.equals(profile._name))
					continue;

				// If the profile started after the end of a run then
				// decrease the activation count accordingly
				if (profile.getTimeStarted() > this._endTime) {
					// Decrease activation count
					double intervalSpillOver = (this._endTime - profile.getTimeStarted())
							/ ((profile._interval * 1000) + (profile._transitionTime * 1000));
					log.info(this + " Need to decrease activation count for: " + profile._name + " spillover: "
							+ intervalSpillOver);
					card._activeCount -= intervalSpillOver;
					continue;
				}
				// Look at the diff between the last activation
				// and the end of steady state
				long intervalEndTime = profile.getTimeStarted() + (profile._interval * 1000) + (profile._transitionTime * 1000);

				// Did the end of the run interrupt this interval
				double diff = intervalEndTime - this._endTime;
				if (diff > 0) {
					double delta = (diff / (double) (profile._interval * 1000));
					log.info(this + " " + card._name + " shortchanged (msecs): " + this._formatter.format(diff));
					log.info(this + " " + card._name + " shortchanged (delta): " + this._formatter.format(delta));
					// Interval truncated so revise activation count downwards
					card._activeCount -= delta;
				}
			}

			totalUsers += card._numberOfUsers * card._activeCount;
			totalIntervalActivations += card._activeCount;
			card.printStatistics(out);
		}

		double averageOpResponseTimeSecs = 0.0;

		if (this.finalCard._totalOpsSuccessful > 0)
			averageOpResponseTimeSecs = ((double) this.finalCard._totalOpResponseTime / (double) this.finalCard._totalOpsSuccessful) / 1000.0;

		ScenarioTrack track = this.getScenarioTrack();
		// Rough averaging of the additional time spent in the system due to think times/cycle times.
		// Look at the proportion of time we would have waited based on think times and the proportion of times we would have
		// waited based on cycle times
		double thinkTimeDeltaSecs = ((1 - track._openLoopProbability) * track.getMeanThinkTime())
				+ (track._openLoopProbability * track.getMeanCycleTime());

		double averageNumberOfUsers = 0.0;
		if (totalIntervalActivations != 0)
			averageNumberOfUsers = totalUsers / totalIntervalActivations;
		finalCard._numberOfUsers = averageNumberOfUsers;
		out.println(this + " Final results----------------------: ");
		out.println(this + " Target host                        : " + this._trackTargetHost);
		out.println(this + " Total drop offs                    : " + this._totalDropoffs);
		out.println(this + " Average drop off Q time (ms)       : "
				+ this._formatter.format((double) this._totalDropOffWaitTime / (double) this._totalDropoffs));
		out.println(this + " Max drop off Q time (ms)           : " + this._maxDropOffWaitTime);
		out.println(this + " Total interval activations         : " + this._formatter.format(totalIntervalActivations));
		out.println(this + " Average number of users            : " + this._formatter.format(averageNumberOfUsers));
		out.println(this + " Offered load (ops/sec)             : " + this._formatter.format(offeredLoadOps));
		out.println(this + " Effective load (ops/sec)           : " + this._formatter.format(effectiveLoadOps));
		// Still a rough estimate, need to compute the bounds on this estimate
		if (averageOpResponseTimeSecs > 0.0) {
			// double opsPerUser = averageNumberOfUsers / this.finalCard._totalOpsSuccessful;

			double littlesEstimate = averageNumberOfUsers / (averageOpResponseTimeSecs + thinkTimeDeltaSecs);
			double littlesDelta = Math.abs((effectiveLoadOps - littlesEstimate) / littlesEstimate) * 100;
			out.println(this + " Little's Law Estimate (ops/sec)    : " + this._formatter.format(littlesEstimate));
			out.println(this + " Variation from Little's Law (%)    : " + this._formatter.format(littlesDelta));
		} else
			out.println(this + " Little's Law Estimate (ops/sec)    : 0");

		out.println(this + " Effective load (requests/sec)      : " + this._formatter.format(effectiveLoadRequests));
		out.println(this + " Operations initiated               : " + this.finalCard._totalOpsInitiated);
		out.println(this + " Operations successfully completed  : " + this.finalCard._totalOpsSuccessful);
		// Avg response time per operation
		out.println(this + " Average operation response time (s): " + this._formatter.format(averageOpResponseTimeSecs));
		out.println(this + " Operations late                    : " + this.finalCard._totalOpsLate);
		out.println(this + " Operations failed                  : " + this.finalCard._totalOpsFailed);
		out.println(this + " Async Ops                          : " + this.finalCard._totalOpsAsync + " "
				+ this._formatter.format((((double) this.finalCard._totalOpsAsync / (double) totalOperations) * 100)) + "%");
		out.println(this + " Sync Ops                           : " + this.finalCard._totalOpsSync + " "
				+ this._formatter.format((((double) this.finalCard._totalOpsSync / (double) totalOperations) * 100)) + "%");

		out.println(this + " Mean response time sample interval : " + this._meanResponseTimeSamplingInterval
				+ " (using Poisson sampling)");

		this.printOperationStatistics(out, false);
		out.println("");
		this.printErrorSummaryStatistics(out, false);
		out.println("");
		this.printWaitTimeStatistics(out, false);
	}

	private void printErrorSummaryStatistics(PrintStream out, boolean purgeStats) {
		synchronized (this._errorSummaryDropOffLock) {
			long totalFailures = 0;
			out.println(this + " Error Summary Results              : " + this._errorMap.size() + " types of error(s)");
			Iterator<String> errorNameIt = this._errorMap.keySet().iterator();
			while (errorNameIt.hasNext()) {
				ErrorSummary summary = this._errorMap.get(errorNameIt.next());
				out.println(this + " " + summary.toString());
				totalFailures += summary._errorCount;
			}
			out.println(this + " Total failures                     : " + totalFailures);
		}
	}

	private void printWaitTimeStatistics(PrintStream out, boolean purgePercentileData) {
		synchronized (this.finalCard._operationMap) {
			try {
				// Make this thing "prettier", using fixed width columns
				String outputFormatSpec = "|%20s|%12s|%12s|%12s|%10s|%10s|%50s|";

				out.println(this
						+ String.format(outputFormatSpec, "operation", "avg wait", "min wait", "max wait", "90th (s)",
								"99th (s)", "pctile"));
				out.println(this + String.format(outputFormatSpec, "", "time (s)", "time (s)", "time (s)", "", "", "samples"));
				// out.println( this +
				// "| operation | proportion | successes | failures | avg response | min response | max response | 90th (s) | 99th (s) | pctile  |"
				// );
				// out.println( this +
				// "|           |            |           |          | time (s)     | time (s)     | time (s)     |          |          | samples |"
				// );

				// Show operation proportions, response time: avg, max, min, stdev (op1 = x%, op2 = y%...)
				// Enumeration<String> keys = this.finalCard._operationMap.keys();
				Iterator<String> keys = this.finalCard._operationMap.keySet().iterator();
				while (keys.hasNext()) {
					String opName = keys.next();
					WaitTimeSummary summary = this._waitTimeMap.get(opName);

					// If there were no values, then the min and max wait times would not have been set
					// so make them to 0
					if (summary.minWaitTime == Long.MAX_VALUE)
						summary.minWaitTime = 0;

					if (summary.maxWaitTime == Long.MIN_VALUE)
						summary.maxWaitTime = 0;

					// Print out the operation summary.
					out.println(this
							+ String.format(
									outputFormatSpec,
									opName,
									// this._formatter.format( ( ( (double) ( summary.succeeded + summary.failed ) / (double)
									// totalOperations ) * 100 ) ) + "% ",
									// summary.succeeded,
									// summary.failed,
									this._formatter.format(summary.getAverageWaitTime() / 1000.0),
									this._formatter.format(summary.minWaitTime / 1000.0),
									this._formatter.format(summary.maxWaitTime / 1000.0),
									this._formatter.format(summary.getNthPercentileResponseTime(90) / 1000.0),
									this._formatter.format(summary.getNthPercentileResponseTime(99) / 1000.0),
									summary.getSamplesCollected() + "/" + summary.getSamplesSeen() + " (mu: "
											+ this._formatter.format(summary.getSampleMean() / 1000.0) + ", sd: "
											+ this._formatter.format(summary.getSampleStandardDeviation() / 1000.0) + " t: "
											+ this._formatter.format(summary.getTvalue(summary.getAverageWaitTime())) + ")"));

					if (purgePercentileData)
						summary.resetSamples();
				}
			} catch (Exception e) {
				log.info(this + " Error printing think/cycle time summary. Reason: " + e.toString());
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unused")
	private void printOperationStatistics(PrintStream out, boolean purgePercentileData) {
		long totalOperations = this.finalCard._totalOpsSuccessful + this.finalCard._totalOpsFailed;
		double totalAvgResponseTime = 0.0;
		double totalResponseTime = 0.0;
		long totalSuccesses = 0;

		synchronized (this.finalCard._operationMap) {
			try {
				// Make this thing "prettier", using fixed width columns
				String outputFormatSpec = "|%20s|%10s|%10s|%10s|%12s|%12s|%12s|%10s|%10s|%50s|";

				out.println(this
						+ String.format(outputFormatSpec, "operation", "proportion", "successes", "failures", "avg response",
								"min response", "max response", "90th (s)", "99th (s)", "pctile"));
				out.println(this
						+ String.format(outputFormatSpec, "", "", "", "", "time (s)", "time (s)", "time(s)", "", "", "samples"));
				// out.println( this +
				// "| operation | proportion | successes | failures | avg response | min response | max response | 90th (s) | 99th (s) | pctile  |"
				// );
				// out.println( this +
				// "|           |            |           |          | time (s)     | time (s)     | time (s)     |          |          | samples |"
				// );

				// Show operation proportions, response time: avg, max, min, stdev (op1 = x%, op2 = y%...)
				// Enumeration<String> keys = this.finalCard._operationMap.keys();
				Iterator<String> keys = this.finalCard._operationMap.keySet().iterator();
				while (keys.hasNext()) {
					String opName = keys.next();
					OperationSummary summary = this.finalCard._operationMap.get(opName);

					totalAvgResponseTime += summary.getAverageResponseTime();
					totalResponseTime += summary.totalResponseTime;
					totalSuccesses += summary.succeeded;
					// If there were no successes, then the min and max response times would not have been set
					// so make them to 0
					if (summary.minResponseTime == Long.MAX_VALUE)
						summary.minResponseTime = 0;

					if (summary.maxResponseTime == Long.MIN_VALUE)
						summary.maxResponseTime = 0;

					// Print out the operation summary.
					out.println(this
							+ String.format(
									outputFormatSpec,
									opName,
									this._formatter
											.format((((double) (summary.succeeded + summary.failed) / (double) totalOperations) * 100))
											+ "% ",
									summary.succeeded,
									summary.failed,
									this._formatter.format(summary.getAverageResponseTime() / 1000.0),
									this._formatter.format(summary.minResponseTime / 1000.0),
									this._formatter.format(summary.maxResponseTime / 1000.0),
									this._formatter.format(summary.getNthPercentileResponseTime(90) / 1000.0),
									this._formatter.format(summary.getNthPercentileResponseTime(99) / 1000.0),
									summary.getSamplesCollected() + "/" + summary.getSamplesSeen() + " (mu: "
											+ this._formatter.format(summary.getSampleMean() / 1000.0) + ", sd: "
											+ this._formatter.format(summary.getSampleStandardDeviation() / 1000.0) + " t: "
											+ this._formatter.format(summary.getTvalue(summary.getAverageResponseTime())) + ")"));

					if (purgePercentileData)
						summary.resetSamples();
				}

				/*
				 * if( this._operationMap.size() > 0 ) { out.println( "" ); //out.println( this +
				 * " average response time (agg)        : " + this._formatter.format( (
				 * totalAvgResponseTime/this._operationMap.size())/1000.0 ) ); out.println( this +
				 * " average response time (s)          : " + this._formatter.format( ( totalResponseTime/totalSuccesses)/1000.0 )
				 * ); }
				 */
			} catch (Exception e) {
				log.info(this + " Error printing operation summary. Reason: " + e.toString());
				e.printStackTrace();
			}
		}
	}

	public void start() {
		if (!this.isRunning()) {
			this._done = false;
			this._workerThread = new Thread(this);
			this._workerThread.setName("Scoreboard-Worker");
			this._workerThread.start();
			// Start the snapshot thread
			if (this._usingMetricSnapshots) {
				this._snapshotThread = new SnapshotWriterThread(this);
				if (this._metricWriter == null)
					log.info(this + " Metric snapshots disabled - No metric writer instance provided");
				else {
					log.info(this + " Metric snapshots enabled - " + this._metricWriter.getDetails());
					this._snapshotThread.setMetricWriter(this._metricWriter);
				}
				this._snapshotThread.setName("Scoreboard-Snapshot-Writer");
				this._snapshotThread.start();
			}
		}
	}

	public void stop() {
		if (this.isRunning()) {
			this._done = true;
			try {
				// Check whether the thread is sleeping. If it is, then interrupt it.
				// if ( this._workerThread.getState() == State.TIMED_WAITING )
				// {
				// this._workerThread.interrupt();
				// }
				// If not give it time to exit; if it takes too long interrupt it.
				log.debug(this + " waiting " + WORKER_EXIT_TIMEOUT + " seconds for worker thread to exit!");
				this._workerThread.join(WORKER_EXIT_TIMEOUT * 1000);
				if (this._workerThread.isAlive()) {
					log.debug(this + " interrupting worker thread.");
					this._workerThread.interrupt();
				}

				if (this._snapshotThread != null) {
					this._snapshotThread._done = true;

					if (this._snapshotThread.getState() == State.TIMED_WAITING) {
						this._snapshotThread.interrupt();
					}
					// If not give it time to exit; if it takes too long interrupt it.
					log.info(this + " waiting " + WORKER_EXIT_TIMEOUT + " seconds for snapshot thread to exit!");
					this._snapshotThread.join(WORKER_EXIT_TIMEOUT * 1000);
					if (this._snapshotThread.isAlive()) {
						log.info(this + " interrupting snapshot thread.");
						this._snapshotThread.interrupt();
					}
				}

				// Shutdown the object pool if it's active
				if (this._statsObjPool.isActive())
					this._statsObjPool.shutdown();

				// log.info( this + " processingQ contains: " + this._processingQ.size() + " unprocessed records." );
			} catch (InterruptedException ie) {
				log.info(this + " Interrupted waiting on worker thread exit!");
			}
		}
	}

	/**
	 * Checks whether the worker thread exists and is alive.
	 * 
	 * @return True if the worker thread exists and is alive.
	 */
	protected boolean isRunning() {
		return (this._workerThread != null && this._workerThread.isAlive());
	}

	/**
	 * Implements the worker thread that periodically grabs the results from the dropOffQ and copies it over to the processingQ to
	 * be processed.
	 */
	public void run() {
		log.debug(this + " worker thread started.");
		while (!this._done || this._dropOffQ.size() > 0) {
			if (this._dropOffQ.size() > 0) {
				// Queue swap
				synchronized (this._dropOffQLock) {
					LinkedList<OperationExecution> temp = _processingQ;
					_processingQ = _dropOffQ;
					_dropOffQ = temp;
				}

				while (!this._processingQ.isEmpty()) {
					OperationExecution result = this._processingQ.remove();
					String traceLabel = result.getTraceLabel();

					if (traceLabel.equals(Scoreboard.STEADY_STATE_TRACE_LABEL)) {
						this.finalCard._totalOpsInitiated++;
						this.processSteadyStateResult(result);
					} else if (traceLabel.equals(Scoreboard.LATE_LABEL)) {
						this.finalCard._totalOpsInitiated++;
						this.finalCard._totalOpsLate++;
					}
				}
			} else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException tie) {
					log.info(this + " worker thread interrupted.");
					// log.info( this + " drop off queue size: " + this._dropOffQ.size());
					// log.info( this + " processing queue size: " + this._processingQ.size());
				}
			}
		}
		log.debug(this + " drop off queue size: " + this._dropOffQ.size());
		log.debug(this + " processing queue size: " + this._processingQ.size());
		log.debug(this + " worker thread finished!");
	}

	/**
	 * Processes a result (from the processingQ) if it was received during the steady state period.
	 * 
	 * @param result
	 *            The operation execution result to process.
	 */
	private void processSteadyStateResult(OperationExecution result) {
		String opName = result._operationName;
		// By default we don't save per-interval metrics
		LoadProfile activeProfile = result._generatedDuring;
		if (activeProfile != null && (activeProfile._name != null && activeProfile._name.length() > 0)) {
			String intervalName = activeProfile._name;
			Scorecard intervalScorecard = this._intervalScorecards.get(intervalName);
			if (intervalScorecard == null) {
				intervalScorecard = new Scorecard(intervalName, activeProfile._interval, this._trackName);
				intervalScorecard._numberOfUsers = activeProfile._numberOfUsers;
				this._intervalScorecards.put(intervalName, intervalScorecard);
			}
			intervalScorecard._activeCount = activeProfile._activeCount;
			intervalScorecard._totalOpsInitiated += 1;

			// Do accounting for this interval's scorecard
			OperationSummary intervalSummary = intervalScorecard._operationMap.get(opName);
			if (intervalSummary == null) {
				intervalSummary = new OperationSummary(new PoissonSamplingStrategy(this._meanResponseTimeSamplingInterval));
				intervalScorecard._operationMap.put(opName, intervalSummary);
			}

			if (result.isFailed()) {
				intervalSummary.failed++;
				intervalScorecard._totalOpsFailed++;
			} else // Result was successful
			{
				// Intervals passed in seconds, convert to msecs
				long intervalMsecs = result._generatedDuring._interval * 1000;
				long intervalEndTime = result._profileStartTime + intervalMsecs;
				if (result.getTimeFinished() <= intervalEndTime) {
					// Count sync vs. async for the operations that complete
					// within the interval only. Ignore the late operations
					if (result.isAsynchronous())
						intervalScorecard._totalOpsAsync++;
					else
						intervalScorecard._totalOpsSync++;
					// log.info( "Cover (msecs): " + ( intervalEndTime - result.getTimeFinished() ) );

					intervalScorecard._totalOpsSuccessful++;
					intervalScorecard._totalActionsSuccessful += result.getActionsPerformed();
					intervalSummary.succeeded++;
					intervalSummary.totalActions += result.getActionsPerformed();

					if (result.isAsynchronous())
						intervalSummary.totalAsyncInvocations++;
					else
						intervalSummary.totalSyncInvocations++;

					// If interactive, look at the total response time.
					if (result.isInteractive()) {
						long responseTime = result.getExecutionTime();
						intervalSummary.acceptSample(responseTime);

						intervalSummary.totalResponseTime += responseTime;
						intervalScorecard._totalOpResponseTime += responseTime;
						if (responseTime > intervalSummary.maxResponseTime)
							intervalSummary.maxResponseTime = responseTime;
						if (responseTime < intervalSummary.minResponseTime)
							intervalSummary.minResponseTime = responseTime;
					}
				} else {
					// Mark the result as late for this interval
					intervalScorecard._totalOpsLate++;
				}
			}
		}

		// Do the accounting for the final score card
		OperationSummary summary = this.finalCard._operationMap.get(opName);
		if (summary == null) {
			summary = new OperationSummary(new PoissonSamplingStrategy(this._meanResponseTimeSamplingInterval));
			this.finalCard._operationMap.put(opName, summary);
		}

		if (result.isAsynchronous()) {
			this.finalCard._totalOpsAsync++;
		} else {
			this.finalCard._totalOpsSync++;
		}

		if (result.isFailed()) {
			summary.failed++;
			this.finalCard._totalOpsFailed++;
		} else {
			this.finalCard._totalOpsSuccessful++;
			this.finalCard._totalActionsSuccessful += result.getActionsPerformed();

			summary.succeeded++;
			summary.totalActions += result.getActionsPerformed();

			if (result.isAsynchronous()) {
				summary.totalAsyncInvocations++;
			} else {
				summary.totalSyncInvocations++;
			}

			// If interactive, look at the total response time.
			if (result.isInteractive()) {
				long responseTime = result.getExecutionTime();
				// Save the response time
				summary.acceptSample(responseTime);
				// Update the total response time
				summary.totalResponseTime += responseTime;

				this.finalCard._totalOpResponseTime += responseTime;
				if (responseTime > summary.maxResponseTime) {
					summary.maxResponseTime = responseTime;
				}
				if (responseTime < summary.minResponseTime) {
					summary.minResponseTime = responseTime;
				}

				// Only save response times if we're doing metric snapshots so we don't just leak memory via object instances
				if (this._usingMetricSnapshots) {
					// Save the response time for the snapshot thread
					ResponseTimeStat stat = null;
					stat = (ResponseTimeStat) this._statsObjPool.rentObject(ResponseTimeStat.NAME);
					if (stat == null) {
						// log.info( "Got stats container from heap (not pool)" );
						stat = new ResponseTimeStat();
					}
					// else log.info( "Got stats container from pool" );

					stat._timestamp = result.getTimeFinished();
					stat._responseTime = responseTime;
					stat._totalResponseTime = this.finalCard._totalOpResponseTime;
					stat._numObservations = this.finalCard._totalOpsSuccessful;
					stat._operationName = result._operationName;
					stat._operationRequest = result._operationRequest;
					if (result._generatedDuring != null)
						stat._generatedDuring = result._generatedDuring._name;
					// log.info( "Pre-push stat: " + stat );

					// Push this stat onto a Queue for the snapshot thread
					synchronized (this._responseTimeQLock) {
						// put something in the queue
						this._responseTimeQ.add(stat);
					}
				}
			}
		}
	}

	public String toString() {
		return "[SCOREBOARD TRACK: " + this._trackName + "]";
	}

	protected class SnapshotWriterThread extends Thread {
		// Owning scoreboard
		private Scoreboard _owner = null;
		private boolean _done = false;
		private MetricWriter _metricWriter = null;

		private boolean _jdbcDriverLoaded = false;
		// Use JDBC to talk to the db
		private Connection _conn = null;
		// Keep track of the last stats sent so that we
		// can just send the deltas
		private long _lastTotalResponseTime = -1;
		private long _lastTotalOpsSuccessful = -1;
		private long _lastTotalActionsSuccessful = -1;

		// private File _statsLog;

		public boolean getDone() {
			return this._done;
		}

		public void setDone(boolean val) {
			this._done = val;
		}

		public MetricWriter getMetricWriter() {
			return this._metricWriter;
		}

		public void setMetricWriter(MetricWriter val) {
			this._metricWriter = val;
		}

		private LinkedList<ResponseTimeStat> _todoQ = new LinkedList<ResponseTimeStat>();

		public SnapshotWriterThread(Scoreboard owner) {
			this._owner = owner;
		}

		public String toString() {
			return "[SNAPSHOTWRITER TRACK: " + this._owner._trackName + "]";
		}

		@SuppressWarnings("unused")
		private void pushStatsToMetricDB() throws SQLException {
			long metricTime = System.currentTimeMillis();
			if (this._jdbcDriverLoaded) {
				// Try to create or reuse a connection and put data in the db
				if (this._conn == null) {
					String connectionString = System.getProperty("dashboarddb");
					if (connectionString != null && connectionString.trim().length() > 0)
						this._conn = DriverManager.getConnection(connectionString);
				}

				long responseTimeDelta = 0;
				long opsSuccessfulDelta = 0;
				long actionsSuccessfulDelta = 0;

				long currentTotalOpsResponseTime = this._owner.finalCard._totalOpResponseTime;
				long currentTotalOpsSuccessful = this._owner.finalCard._totalOpsSuccessful;
				long currentTotalActionsSuccessful = this._owner.finalCard._totalActionsSuccessful;

				// Should we send the total response time etc. or just the deltas
				if (this._lastTotalResponseTime == -1) {
					responseTimeDelta = currentTotalOpsResponseTime;
					opsSuccessfulDelta = currentTotalOpsSuccessful;
					actionsSuccessfulDelta = currentTotalActionsSuccessful;
				} else {
					responseTimeDelta = currentTotalOpsResponseTime - this._lastTotalResponseTime;
					opsSuccessfulDelta = currentTotalOpsSuccessful - this._lastTotalOpsSuccessful;
					actionsSuccessfulDelta = currentTotalActionsSuccessful - this._lastTotalActionsSuccessful;
				}

				// Update the previous observations with the current
				this._lastTotalResponseTime = currentTotalOpsResponseTime;
				this._lastTotalOpsSuccessful = currentTotalOpsSuccessful;
				this._lastTotalActionsSuccessful = currentTotalActionsSuccessful;

				// Do the printing
				log.info(this + " " + metricTime + " ttl response time delta (msecs): " + responseTimeDelta
						+ " operations successful delta: " + opsSuccessfulDelta + " actions successful delta: "
						+ actionsSuccessfulDelta);

				if (this._conn != null && !this._conn.isClosed()) {

					PreparedStatement stmnt = this._conn
							.prepareStatement("insert into rainStats (timestamp,trackName,totalResponseTime,operationsSuccessful, actionsSuccessful) values (?,?,?,?,?)");
					stmnt.setLong(1, metricTime);
					stmnt.setString(2, this._owner._owner._name);
					// stmnt.setLong( 3, this._owner.finalCard._totalOpResponseTime );
					// stmnt.setLong( 4, this._owner.finalCard._totalOpsSuccessful );
					// stmnt.setLong( 5, this._owner.finalCard._totalActionsSuccessful );
					stmnt.setLong(3, responseTimeDelta);
					stmnt.setLong(4, opsSuccessfulDelta);
					stmnt.setLong(5, actionsSuccessfulDelta);
					stmnt.execute();
				}
			}
		}

		public void run() {
			// Do the queue swap and then write until there's nothing left to write
			while (!this._done || this._owner._responseTimeQ.size() > 0) {
				if (this._owner._responseTimeQ.size() > 0) {
					// Print pre-swap sizes
					// log.info( "Pre-swap todoQ: " + this._todoQ.size() + " responseTimeQ: " + this._owner._responseTimeQ.size()
					// );

					// grab the queue lock and swap queues so we can write what's currently there
					synchronized (this._owner._responseTimeQLock) {
						LinkedList<ResponseTimeStat> temp = this._owner._responseTimeQ;
						this._owner._responseTimeQ = this._todoQ;
						this._todoQ = temp;
					}

					// log.info( "Post-swap todoQ: " + this._todoQ.size() + " responseTimeQ: " + this._owner._responseTimeQ.size()
					// );

					// Now write everything out
					while (!this._todoQ.isEmpty()) {
						ResponseTimeStat stat = this._todoQ.removeFirst();

						try {
							if (this._metricWriter != null)
								this._metricWriter.write(stat);
						} catch (Exception e) {
						} finally {
							// Return the stats object to the pool
							if (stat != null) {
								this._owner._statsObjPool.returnObject(stat);
							}
						}
					}
					// log.info( this + " todoQ empty, re-checking..." );
				} else {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException tie) {
						log.info(this + " snapshot thread interrupted.");
						// Close the log file if we're interrupted
						if (this._metricWriter != null) {
							try {
								this._metricWriter.close();
							} catch (Exception e) {
							}
						}
					}
				}
			}// end-while there's work to do

			// Close nicely if we're not interrupted
			if (this._metricWriter != null) {
				try {
					this._metricWriter.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public long getMeanResponseTimeSamplingInterval() {
		return this._meanResponseTimeSamplingInterval;
	}

	public void setMeanResponseTimeSamplingInterval(long val) {
		if (val > 0)
			this._meanResponseTimeSamplingInterval = val;
	}

	public long getStartTimestamp() {
		return this._startTime;
	}

	public void setStartTimestamp(long val) {
		this._startTime = val;
	}

	public long getEndTimestamp() {
		return this._endTime;
	}

	public void setEndTimestamp(long val) {
		this._endTime = val;
	}

	public String getTrackName() {
		return this._trackName;
	}

	public void setTrackName(String val) {
		this._trackName = val;
	}

	public boolean getDone() {
		return this._done;
	}

	public void setDone(boolean val) {
		this._done = val;
	}

	public void setLogSamplingProbability(double val) {
		this._logSamplingProbability = val;
	}

	public void setMetricSnapshotInterval(long val) {
		// not supported
	}

	public boolean getUsingMetricSnapshots() {
		return this._usingMetricSnapshots;
	}

	public void setUsingMetricSnapshots(boolean val) {
		this._usingMetricSnapshots = val;
	}

	public MetricWriter getMetricWriter() {
		return this._metricWriter;
	}

	public void setMetricWriter(MetricWriter val) {
		this._metricWriter = val;
	}

	public String getTargetHost() {
		return this._trackTargetHost;
	}

	public void setTargetHost(String val) {
		this._trackTargetHost = val;
	}

	public Scorecard getFinalScorecard() {
		return this.finalCard;
	}

	public void registerErrorLogHandle(String owner, FileWriter logHandle) {
		synchronized (this._errorLogHandleMap) {
			this._errorLogHandleMap.put(owner, logHandle);
		}
	}

	public void deRegisterErrorLogHandle(String owner) {
		synchronized (this._errorLogHandleMap) {
			this._errorLogHandleMap.remove(owner);
		}
	}

	public void registerLogHandle(String owner, FileWriter logHandle) {
		synchronized (this._logHandleMap) {
			this._logHandleMap.put(owner, logHandle);
		}
	}

	public void deRegisterLogHandle(String owner) {
		synchronized (this._logHandleMap) {
			this._logHandleMap.remove(owner);
		}
	}

	public ScenarioTrack getScenarioTrack() {
		return this._owner;
	}

	public void setScenarioTrack(ScenarioTrack owner) {
		this._owner = owner;
	}
}
