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

import radlab.rain.operation.OperationExecution;

/**
 * Eventually all stats reporting will be done using Scorecards. There will be per-interval Scorecards as well as a
 * final Scorecard for the entire run. The Scoreboard will maintain/manage a hashtable of Scorecards.
 * 
 */
public class Scorecard {
	private static Logger logger = LoggerFactory.getLogger(Scorecard.class);

	enum Type {
		GLOBAL, AGGREGATED, TARGET
	}

	// Scorecard type
	private Type type = Type.TARGET;

	// Aggregation identifier
	private String aggregationIdentifier;

	// Duration of the interval for this scorecard
	private long intervalDuration = 0;

	// Total operation counters (includes failed operations)
	private long totalOpsInitiated = 0;
	private long totalOpsLate = 0;

	// Summary of all operations
	private OperationSummary summary = new OperationSummary(new NullSamplingStrategy());

	// A mapping of each operation with its summary
	private TreeMap<String, OperationSummary> operationSummaryMap = new TreeMap<String, OperationSummary>();

	Scorecard(Type type, long timeActive) {
		this.type = type;
		this.intervalDuration = timeActive;
	}

	Scorecard(Type type, long timeActive, String aggregationIdentifier) {
		this(type, timeActive);
		this.aggregationIdentifier = aggregationIdentifier;
	}

	void processLateOperation(OperationExecution result) {
		totalOpsInitiated++;
		totalOpsLate++;
	}

	void processResult(OperationExecution result) {
		// Do the accounting for the final score card
		OperationSummary operationSummary = operationSummaryMap.get(result.operationName);
		if (operationSummary == null) {
			operationSummary = new OperationSummary(new AllSamplingStrategy());
			operationSummaryMap.put(result.operationName, operationSummary);
		}

		// Process result for the operation
		operationSummary.processResult(result);
		summary.processResult(result);

		// Total operation counter
		totalOpsInitiated++;
	}

	JSONObject getStatistics(double runDuration) throws JSONException {
		double offeredLoadOps = 0;// Operations initiated per second

		// Calculations (per second)
		if (runDuration > 0) {
			offeredLoadOps = (double) totalOpsInitiated / (runDuration / 1000d);
		} else {
			logger.warn("run duration <= 0");
		}

		// Create result object
		JSONObject result = new JSONObject();
		result.put("aggreation_identifier", aggregationIdentifier);
		result.put("run_duration", runDuration);
		result.put("interval_duration", intervalDuration);
		result.put("total_ops_initiated", totalOpsInitiated);
		result.put("total_ops_late", totalOpsLate);
		result.put("offered_load_ops", offeredLoadOps);

		// Summary statistics
		result.put("summary", summary.getStatistics(runDuration));

		// Operational statistics
		result.put("operational", getOperationalStatistics(runDuration));

		return result;
	}

	public JSONObject getIntervalStatistics() throws JSONException {
		JSONObject result = getStatistics(intervalDuration);
		return result;
	}

	private JSONObject getOperationalStatistics(double runDuration) throws JSONException {
		JSONObject result = new JSONObject();
		JSONArray operations = new JSONArray();
		result.put("operations", operations);

		synchronized (operationSummaryMap) {

			for (Iterator<String> keys = operationSummaryMap.keySet().iterator(); keys.hasNext();) {
				String operationName = keys.next();
				OperationSummary operationSummary = operationSummaryMap.get(operationName);
				result.put(operationName, operationSummary.getStatistics(runDuration));
			}
		}

		return result;
	}

	public void merge(Scorecard from) {
		// Merge another scorecard with this
		this.totalOpsInitiated += from.totalOpsInitiated;
		this.totalOpsLate += from.totalOpsLate;

		// Merge summary
		this.summary.merge(from.summary);

		// Merge operation maps
		for (String operationName : from.operationSummaryMap.keySet()) {
			OperationSummary mySummary = null;
			OperationSummary fromSummary = from.operationSummaryMap.get(operationName);

			// Do we have an operationSummary for this operation yet?
			// If we don't have one, initialize an OperationSummary with a Null/dummy sampler that will
			// simply accept all of the samples from the rhs' sampler
			if (this.operationSummaryMap.containsKey(operationName))
				mySummary = this.operationSummaryMap.get(operationName);
			else
				mySummary = new OperationSummary(new NullSamplingStrategy());

			mySummary.merge(fromSummary);
			this.operationSummaryMap.put(operationName, mySummary);
		}
	}

	String getAggregationIdentifier() {
		return aggregationIdentifier;
	}

	public Map<String, OperationSummary> getOperationMap() {
		return Collections.unmodifiableMap(operationSummaryMap);
	}

	public Type getType() {
		return type;
	}

	public long getTimeActive() {
		return intervalDuration;
	}

	public long getTotalOpResponseTime() {
		return summary.getTotalResponseTime();
	}

	public long getTotalOpsSuccessful() {
		return summary.getOpsSuccessful();
	}

}
