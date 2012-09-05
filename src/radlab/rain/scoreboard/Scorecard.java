package radlab.rain.scoreboard;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.LoadProfile;
import radlab.rain.OperationExecution;
import radlab.rain.util.NullSamplingStrategy;
import radlab.rain.util.PoissonSamplingStrategy;

// Not even going to try to make Scorecards thread-safe, the Scoreboard must do "the right thing"(tm)
public class Scorecard {
	private static Logger logger = LoggerFactory.getLogger(Scorecard.class);

	// Eventually all stats reporting will be done using Scorecards. There will
	// be per-interval Scorecards as well as a final Scorecard for the entire run.
	// The Scoreboard will maintain/manage a hashtable of Scorecards.

	// All scorecards are named with the interval they are generated in
	String name = "";

	// What track does this scorecard belong to
	String trackName = "";

	// What goes on the scorecard?
	long totalOpsSuccessful = 0;
	long totalOpsFailed = 0;
	long totalActionsSuccessful = 0;
	long totalOpsAsync = 0;
	long totalOpsSync = 0;
	long totalOpsInitiated = 0;
	long totalOpsLate = 0;
	long totalOpResponseTime = 0;

	long intervalDuration = 0;
	double numberOfUsers = 0.0;
	double activeCount = 1.0;

	// A mapping of each operation with its summary
	TreeMap<String, OperationSummary> operationMap = new TreeMap<String, OperationSummary>();

	public Scorecard(String name, long intervalDurationInSecs, String trackName) {
		this.name = name;
		this.intervalDuration = intervalDurationInSecs;
		this.trackName = trackName;
	}

	public void reset() {
		// Clear the operation map
		this.operationMap.clear();

		// Reset aggregate counters
		this.totalActionsSuccessful = 0;
		this.totalOpsAsync = 0;
		this.totalOpsFailed = 0;
		this.totalOpsInitiated = 0;
		this.totalOpsSuccessful = 0;
		this.totalOpsSync = 0;
		this.totalOpsLate = 0;
		this.totalOpResponseTime = 0;
		this.intervalDuration = 0;
		this.activeCount = 0.0;
		this.numberOfUsers = 0.0;
	}

	void processLateOperation(OperationExecution result) {
		totalOpsInitiated++;
		totalOpsLate++;
	}

	void processResult(OperationExecution result, double meanResponseTimeSamplingInterval) {
		// Update global counters counters
		String operationName = result._operationName;
		totalOpsInitiated++;
		if (result.isAsynchronous())
			totalOpsAsync++;
		else
			totalOpsSync++;

		// Do the accounting for the final score card
		OperationSummary operationSummary = operationMap.get(operationName);
		// Create operation summary if needed
		if (operationSummary == null) {
			operationSummary = new OperationSummary(new PoissonSamplingStrategy(meanResponseTimeSamplingInterval));
			operationMap.put(operationName, operationSummary);
		}

		if (result.isFailed()) {
			totalOpsFailed++;
			operationSummary.opsFailed++;
		} else { // Result successful
			totalOpsSuccessful++;
			operationSummary.opsSuccessful++;

			totalActionsSuccessful += result.getActionsPerformed();
			operationSummary.actionsSuccessful += result.getActionsPerformed();

			// Count operations
			if (result.isAsynchronous())
				operationSummary.asyncInvocations++;
			else
				operationSummary.syncInvocations++;

			// Intervals passed in seconds, convert to milliseconds
			long profileLengthMsecs = result._generatedDuring._interval * 1000;
			long profileEndMsecs = result._profileStartTime + profileLengthMsecs;

			// Result returned after profile interval ended
			if (result.getTimeFinished() > profileEndMsecs) {
				totalOpsLate++;
			} else { // Did the result occur before the profile interval ended
			}

			if (result.isInteractive()) {
				long responseTime = result.getExecutionTime();
				operationSummary.acceptSample(responseTime);

				// Response time
				operationSummary.totalResponseTime += responseTime;
				totalOpResponseTime += responseTime;

				// Update max and min response time
				operationSummary.maxResponseTime = Math.max(operationSummary.maxResponseTime, responseTime);
				operationSummary.minResponseTime = Math.min(operationSummary.minResponseTime, responseTime);
			}
		}
	}

	void processProfileResult(OperationExecution result, double meanResponseTimeSamplingInterval) {
		LoadProfile activeProfile = result._generatedDuring;
		activeCount = activeProfile._activeCount;
		processResult(result, meanResponseTimeSamplingInterval);
	}

	JSONObject getStatistics(double runDuration) throws JSONException {
		// Total operations executed
		long totalOperations = totalOpsSuccessful + totalOpsFailed;

		double offeredLoadOps = 0;// Operations initiated per second
		double effectiveLoadOps = 0; // Operations successful per second
		double effectiveLoadRequests = 0; // Actions successful per second
		double averageOpResponseTimeSecs = 0; // Average response time of an operation in seconds

		// Calculations
		if (runDuration > 0) {
			offeredLoadOps = (double) totalOpsInitiated / runDuration;
			effectiveLoadOps = (double) totalOpsSuccessful / runDuration;
			effectiveLoadRequests = (double) totalActionsSuccessful / runDuration;
		} else {
			logger.warn("run duration <= 0");
		}

		if (totalOpsSuccessful > 0) {
			averageOpResponseTimeSecs = (double) totalOpResponseTime / (double) totalOpsSuccessful;
		} else {
			logger.warn("total ops successfull <= 0");
		}

		// Create result object
		JSONObject result = new JSONObject();
		result.put("track", trackName);
		result.put("interval_name", name);
		result.put("active_count", activeCount);

		result.put("total_ops_successful", totalOpsSuccessful);
		result.put("total_operations_failed", totalOpsFailed);
		result.put("total_actions_successfulo", totalActionsSuccessful);
		result.put("total_ops_async", totalOpsAsync);
		result.put("total_ops_sync", totalOpsSync);
		result.put("total_ops_initiated", totalOpsInitiated);
		result.put("total_ops_late", totalOpsLate);
		result.put("total_op_response_time", totalOpResponseTime);
		result.put("interval_duration", intervalDuration);
		result.put("number_of_users", numberOfUsers);
		result.put("total_operations", totalOperations);
		result.put("offered_load_ops", offeredLoadOps);
		result.put("effective_load_ops", effectiveLoadOps);
		result.put("effective_load_req", effectiveLoadRequests);
		result.put("average_operation_response_time_secs", averageOpResponseTimeSecs);

		// Operational statistics
		result.put("operational", getOperationalStatistics(false));

		return result;
	}

	public JSONObject getIntervalStatistics() throws JSONException {
		JSONObject result = getStatistics(intervalDuration);
		return result;
	}

	private JSONObject getOperationalStatistics(boolean purgePercentileData) throws JSONException {
		JSONObject result = new JSONObject();
		JSONArray operationArr = new JSONArray();
		result.put("operations", operationArr);

		synchronized (operationMap) {

			long totalOperations = totalOpsSuccessful + totalOpsFailed;
			double totalAvgResponseTime = 0.0;
			double totalResponseTime = 0.0;
			long totalSuccesses = 0;

			for (Iterator<String> keys = operationMap.keySet().iterator(); keys.hasNext();) {
				String operationName = keys.next();
				OperationSummary operationSummary = operationMap.get(operationName);

				// Update global counters
				totalAvgResponseTime += operationSummary.getAverageResponseTime();
				totalResponseTime += operationSummary.totalResponseTime;
				totalSuccesses += operationSummary.opsSuccessful;

				if (operationSummary.minResponseTime == Long.MAX_VALUE)
					operationSummary.minResponseTime = 0;

				if (operationSummary.maxResponseTime == Long.MIN_VALUE)
					operationSummary.maxResponseTime = 0;

				// Calculations
				double proportion = (double) (operationSummary.opsSuccessful + operationSummary.opsFailed) / (double) totalOperations;

				// Print out the operation summary.
				JSONObject operation = operationSummary.getJSONStats();
				operationArr.put(operation);
				operation.put("operation_name", operationName);
				operation.put("proportion", proportion);

				if (purgePercentileData)
					operationSummary.resetSamples();
			}

			// Add totals
			result.put("total_avg_response_time", totalAvgResponseTime);
			result.put("total_response_time", totalResponseTime);
			result.put("total_successes", totalSuccesses);
		}

		return result;
	}

	public void merge(Scorecard rhs) {
		// We expect to merge only "final" scorecards

		// For merges the activeCount is always set to 1
		this.activeCount = 1;
		// Merge another scorecard with "me"
		// Let's compute total operations
		this.totalOpsSuccessful += rhs.totalOpsSuccessful;
		this.totalOpsFailed += rhs.totalOpsFailed;
		this.totalActionsSuccessful += rhs.totalActionsSuccessful;
		this.totalOpsAsync += rhs.totalOpsAsync;
		this.totalOpsSync += rhs.totalOpsSync;
		this.totalOpsInitiated += rhs.totalOpsInitiated;
		this.totalOpsLate += rhs.totalOpsLate;
		this.totalOpResponseTime += rhs.totalOpResponseTime;
		this.numberOfUsers += rhs.numberOfUsers;

		// Merge operation maps
		for (String opName : rhs.operationMap.keySet()) {
			OperationSummary lhsOpSummary = null;
			OperationSummary rhsOpSummary = rhs.operationMap.get(opName);
			// Do we have an operationSummary for this operation yet?
			// If we don't have one, initialize an OperationSummary with a Null/dummy sampler that will
			// simply accept all of the samples from the rhs' sampler
			if (this.operationMap.containsKey(opName))
				lhsOpSummary = this.operationMap.get(opName);
			else
				lhsOpSummary = new OperationSummary(new NullSamplingStrategy());
			lhsOpSummary.merge(rhsOpSummary);
			this.operationMap.put(opName, lhsOpSummary);
		}
	}

	public long getIntervalDuration() {
		return intervalDuration;
	}

	public String toString() {
		return "[SCOREBOARD TRACK: " + this.trackName + "]";
	}

	public String getName() {
		return name;
	}

	public String getTrackName() {
		return trackName;
	}

	public long getTotalOpsSuccessful() {
		return totalOpsSuccessful;
	}

	public long getTotalOpsFailed() {
		return totalOpsFailed;
	}

	public long getTotalActionsSuccessful() {
		return totalActionsSuccessful;
	}

	public long getTotalOpsAsync() {
		return totalOpsAsync;
	}

	public long getTotalOpsSync() {
		return totalOpsSync;
	}

	public long getTotalOpsInitiated() {
		return totalOpsInitiated;
	}

	public long getTotalOpsLate() {
		return totalOpsLate;
	}

	public long getTotalOpResponseTime() {
		return totalOpResponseTime;
	}

	public double getNumberOfUsers() {
		return numberOfUsers;
	}

	public double getActiveCount() {
		return activeCount;
	}

	public Map<String, OperationSummary> getOperationMap() {
		return Collections.unmodifiableMap(operationMap);
	}
}
