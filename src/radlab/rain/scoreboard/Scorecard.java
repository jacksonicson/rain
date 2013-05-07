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
		DEFAULT, MERGED
	}

	// Scorecard type
	private Type type = Type.DEFAULT;

	// Aggregation identifier
	private final String aggregationIdentifier;

	// Duration of the interval for this scorecard
	private final long intervalDuration;

	// Total operation counters (includes failed operations)
	private long totalOpsInitiated = 0;
	private long totalOpsLate = 0;

	// Summary for all operations
	private OperationSummary summary = new OperationSummary(new DummySamplingStrategy());

	// A mapping of each operation with its summary
	private TreeMap<String, OperationSummary> operationSummaryMap = new TreeMap<String, OperationSummary>();

	Scorecard(long timeActive) {
		this.intervalDuration = timeActive;
		this.aggregationIdentifier = null;
	}

	Scorecard(long timeActive, String aggregationIdentifier) {
		this.intervalDuration = timeActive;
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
			operationSummary = new OperationSummary(new PoissonSamplingStrategy(result.operationName));
			operationSummaryMap.put(result.operationName, operationSummary);
		}

		// Process result for the operation
		operationSummary.processResult(result);
		summary.processResult(result);

		// Total operation counter
		totalOpsInitiated++;
	}

	public JSONObject getSummarizedStatistics() throws JSONException {
		JSONObject result = getSummarizedStatistics(intervalDuration);
		return result;
	}

	JSONObject getSummarizedStatistics(double runDuration) throws JSONException {
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

		// Embed summary statistics
		result.put("summary", summary.getStatistics(runDuration, type == Type.MERGED));

		// Embed operational statistics
		result.put("operational", getOperationStatistics(runDuration));

		return result;
	}

	private JSONObject getOperationStatistics(double runDuration) throws JSONException {
		JSONObject result = new JSONObject();
		JSONArray operations = new JSONArray();
		result.put("operations", operations);

		synchronized (operationSummaryMap) {

			for (Iterator<String> keys = operationSummaryMap.keySet().iterator(); keys.hasNext();) {
				String operationName = keys.next();
				OperationSummary operationSummary = operationSummaryMap.get(operationName);
				result.put(operationName, operationSummary.getStatistics(runDuration, type == Type.MERGED));
			}
		}

		return result;
	}

	public void merge(Scorecard from) {
		// This scorecard becomes a merged
		this.type = Type.MERGED;

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
				mySummary = new OperationSummary(new AllSamplingStrategy());

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
