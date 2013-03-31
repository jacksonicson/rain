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

import radlab.rain.load.LoadDefinition;
import radlab.rain.operation.OperationExecution;
import radlab.rain.util.NullSamplingStrategy;
import radlab.rain.util.PoissonSamplingStrategy;

/**
 * Eventually all stats reporting will be done using Scorecards. There will be per-interval Scorecards as well as a
 * final Scorecard for the entire run. The Scoreboard will maintain/manage a hashtable of Scorecards.
 * 
 */
class Scorecard {
	private static Logger logger = LoggerFactory.getLogger(Scorecard.class);

	// Name of the scorecard
	private final String name;

	// What track does this scorecard belong to
	private final long targetId;

	// What goes on the scorecard?
	private long totalOpsSuccessful = 0;
	private long totalOpsFailed = 0;
	private long totalActionsSuccessful = 0;
	private long totalOpsAsync = 0;
	private long totalOpsSync = 0;
	private long totalOpsInitiated = 0;
	private long totalOpsLate = 0;
	private long totalOpResponseTime = 0;
	private long intervalDuration = 0;
	private long numberOfUsers = 0;
	private long minResponseTime = Long.MAX_VALUE;
	private long maxResponseTime = 0;

	// A mapping of each operation with its summary
	private TreeMap<String, OperationSummary> operationMap = new TreeMap<String, OperationSummary>();

	public Scorecard(String name, long trackId, long intervalDuration) {
		this(name, trackId, intervalDuration, 0);
	}

	public Scorecard(String name, long trackId, long intervalDuration, long numberOfUsers) {
		this.name = name;
		this.targetId = trackId;
		this.intervalDuration = intervalDuration;
		this.numberOfUsers = numberOfUsers;
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
		this.maxResponseTime = 0;
		this.minResponseTime = Long.MAX_VALUE;
	}

	void processLateOperation(OperationExecution result) {
		totalOpsInitiated++;
		totalOpsLate++;
	}

	void processResult(OperationExecution result, double meanResponseTimeSamplingInterval) {
		// Update global counters counters
		String operationName = result.operationName;
		totalOpsInitiated++;

		// Do the accounting for the final score card
		OperationSummary operationSummary = operationMap.get(operationName);
		// Create operation summary if needed
		if (operationSummary == null) {
			operationSummary = new OperationSummary(new PoissonSamplingStrategy(name + "." + targetId + "."
					+ operationName, meanResponseTimeSamplingInterval));
			operationMap.put(operationName, operationSummary);
		}

		// Process result for the operation
		operationSummary.processResult(result, meanResponseTimeSamplingInterval);

		// Process result for this scorecard
		if (result.failed) {
			totalOpsFailed++;
		} else { // Result successful
			totalOpsSuccessful++;

			totalActionsSuccessful += result.actionsPerformed;

			// Count operations
			if (result.async)
				totalOpsAsync++;
			else
				totalOpsSync++;

			long responseTime = result.getExecutionTime();

			// Response time
			totalOpResponseTime += responseTime;

			// Update max and min response time
			maxResponseTime = Math.max(maxResponseTime, responseTime);
			minResponseTime = Math.min(minResponseTime, responseTime);
		}
	}

	void processProfileResult(OperationExecution result, double meanResponseTimeSamplingInterval) {
		processResult(result, meanResponseTimeSamplingInterval);
	}

	JSONObject getStatistics(double runDuration) throws JSONException {
		// Total operations executed
		long totalOperations = totalOpsSuccessful + totalOpsFailed;

		double offeredLoadOps = 0;// Operations initiated per second
		double effectiveLoadOps = 0; // Operations successful per second
		double effectiveLoadRequests = 0; // Actions successful per second
		double averageOpResponseTime = 0; // Average response time of an operation in seconds

		// Calculations (per second)
		if (runDuration > 0) {
			offeredLoadOps = (double) totalOpsInitiated / toSeconds(runDuration);
			effectiveLoadOps = (double) totalOpsSuccessful / toSeconds(runDuration);
			effectiveLoadRequests = (double) totalActionsSuccessful / toSeconds(runDuration);
		} else {
			logger.warn("run duration <= 0");
		}

		if (totalOpsSuccessful > 0) {
			averageOpResponseTime = (double) totalOpResponseTime / (double) totalOpsSuccessful;
		} else {
			logger.warn("total ops successfull <= 0");
		}

		// Create result object
		JSONObject result = new JSONObject();
		result.put("track", targetId);
		result.put("interval_name", name);
		result.put("run_duration", runDuration);
		result.put("interval_duration", intervalDuration);
		result.put("total_ops_successful", totalOpsSuccessful);
		result.put("total_operations_failed", totalOpsFailed);
		result.put("total_actions_successful", totalActionsSuccessful);
		result.put("total_ops_async", totalOpsAsync);
		result.put("total_ops_sync", totalOpsSync);
		result.put("total_ops_initiated", totalOpsInitiated);
		result.put("total_ops_late", totalOpsLate);
		result.put("total_op_response_time", totalOpResponseTime);
		result.put("number_of_users", numberOfUsers);
		result.put("total_operations", totalOperations);
		result.put("offered_load_ops", offeredLoadOps);
		result.put("effective_load_ops", effectiveLoadOps);
		result.put("effective_load_req", effectiveLoadRequests);
		result.put("max_response_time", maxResponseTime);
		result.put("min_response_time", minResponseTime);
		result.put("average_response_time", averageOpResponseTime);

		// Operational statistics
		result.put("operational", getOperationalStatistics(false));

		return result;
	}

	private final double toSeconds(double milliseconds) {
		return milliseconds / 1000d;
	}

	public JSONObject getIntervalStatistics() throws JSONException {
		JSONObject result = getStatistics(intervalDuration);
		return result;
	}

	private JSONObject getOperationalStatistics(boolean purgePercentileData) throws JSONException {
		JSONObject result = new JSONObject();
		JSONArray operations = new JSONArray();
		result.put("operations", operations);

		synchronized (operationMap) {

			long totalOperations = getTotalSteadyOperations();
			double totalAvgResponseTime = 0.0;
			double totalResponseTime = 0.0;
			long totalSuccesses = 0;

			for (Iterator<String> keys = operationMap.keySet().iterator(); keys.hasNext();) {
				String operationName = keys.next();
				OperationSummary operationSummary = operationMap.get(operationName);

				// Update global counters
				totalAvgResponseTime += operationSummary.getAverageResponseTime();
				totalResponseTime += operationSummary.getTotalResponseTime();
				totalSuccesses += operationSummary.getOpsSuccessful();

				// Calculations
				double proportion = (double) (operationSummary.getOpsSuccessful() + operationSummary.getOpsFailed())
						/ (double) totalOperations;

				// Print out the operation summary.
				JSONObject operation = operationSummary.getStatistics();
				operations.put(operation);
				operation.put("operation_name", operationName);
				operation.put("proportion", proportion);

				if (purgePercentileData)
					operationSummary.resetSamples();
			}

			// Add totals
			result.put("total_avg_response_time", totalAvgResponseTime);
			result.put("total_response_time", totalResponseTime);
			result.put("total_successes", totalSuccesses);
			result.put("total_operations", totalOperations);
		}

		return result;
	}

	public void merge(Scorecard from) {
		// Merge another scorecard with "me"
		this.totalOpsSuccessful += from.totalOpsSuccessful;
		this.totalOpsFailed += from.totalOpsFailed;
		this.totalActionsSuccessful += from.totalActionsSuccessful;
		this.totalOpsAsync += from.totalOpsAsync;
		this.totalOpsSync += from.totalOpsSync;
		this.totalOpsInitiated += from.totalOpsInitiated;
		this.totalOpsLate += from.totalOpsLate;
		this.totalOpResponseTime += from.totalOpResponseTime;
		this.numberOfUsers += from.numberOfUsers;
		this.maxResponseTime = Math.max(maxResponseTime, from.maxResponseTime);
		this.minResponseTime = Math.min(minResponseTime, from.minResponseTime);

		// Merge operation maps
		for (String operationName : from.operationMap.keySet()) {
			OperationSummary mySummary = null;
			OperationSummary fromSummary = from.operationMap.get(operationName);

			// Do we have an operationSummary for this operation yet?
			// If we don't have one, initialize an OperationSummary with a Null/dummy sampler that will
			// simply accept all of the samples from the rhs' sampler
			if (this.operationMap.containsKey(operationName))
				mySummary = this.operationMap.get(operationName);
			else
				mySummary = new OperationSummary(new NullSamplingStrategy());

			mySummary.merge(fromSummary);
			this.operationMap.put(operationName, mySummary);
		}
	}

	private final long getTotalSteadyOperations() {
		return totalOpsSuccessful + totalOpsFailed;
	}

	public long getIntervalDuration() {
		return intervalDuration;
	}

	public String getName() {
		return name;
	}

	public long getTargetId() {
		return targetId;
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

	public long getNumberOfUsers() {
		return numberOfUsers;
	}

	public Map<String, OperationSummary> getOperationMap() {
		return Collections.unmodifiableMap(operationMap);
	}

	public String toString() {
		return "target-" + targetId + " ";
	}
}
