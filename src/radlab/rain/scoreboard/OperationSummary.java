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

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.operation.OperationExecution;
import radlab.rain.util.ISamplingStrategy;

public class OperationSummary {
	// Information recorded about one operation type
	private long opsSuccessful = 0;
	private long opsFailed = 0;
	private long actionsSuccessful = 0;
	private long totalResponseTime = 0;
	private long asyncInvocations = 0;
	private long syncInvocations = 0;
	private long minResponseTime = Long.MAX_VALUE;
	private long maxResponseTime = Long.MIN_VALUE;

	// Sample the response times so that we can give a "reasonable"
	// estimate of the 90th and 99th percentiles.
	private ISamplingStrategy responseTimeSampler;

	public OperationSummary(ISamplingStrategy strategy) {
		responseTimeSampler = strategy;
	}

	void resetSamples() {
		responseTimeSampler.reset();
	}

	void processResult(OperationExecution result, double meanResponseTimeSamplingInterval) {
		if (result.failed) {
			opsFailed++;
		} else { // Result successful
			opsSuccessful++;

			actionsSuccessful += result.actionsPerformed;

			// Count operations
			if (result.async) {
				asyncInvocations++;
			} else {
				syncInvocations++;
			}

			long responseTime = result.getExecutionTime();
			responseTimeSampler.accept(responseTime);

			// Response time
			totalResponseTime += responseTime;

			// Update max and min response time
			maxResponseTime = Math.max(maxResponseTime, responseTime);
			minResponseTime = Math.min(minResponseTime, responseTime);
		}
	}

	JSONObject getStatistics() throws JSONException {
		// Calculations
		long minResponseTime = this.minResponseTime;
		if (minResponseTime == Long.MAX_VALUE)
			minResponseTime = 0;

		long maxResponseTime = this.maxResponseTime;
		if (maxResponseTime == Long.MIN_VALUE)
			maxResponseTime = 0;

		// Results
		JSONObject operation = new JSONObject();
		operation.put("samples_collected", responseTimeSampler.getSamplesCollected());
		operation.put("samples_seen", responseTimeSampler.getSamplesSeen());
		operation.put("ops_successful", opsSuccessful);
		operation.put("ops_failed", opsFailed);
		operation.put("total_response_time", totalResponseTime);
		operation.put("average_response_time", getAverageResponseTime());
		operation.put("min_response_time", minResponseTime);
		operation.put("max_response_time", maxResponseTime);
		operation.put("90_percentile_response_time", responseTimeSampler.getNthPercentile(90));
		operation.put("99_percentile_response_time", responseTimeSampler.getNthPercentile(99));
		operation.put("sample_mean", responseTimeSampler.getSampleMean());
		operation.put("sample_stdev", responseTimeSampler.getSampleStandardDeviation());
		operation.put("tvalue_avg_resp_time", responseTimeSampler.getTvalue(getAverageResponseTime()));

		return operation;
	}

	double getAverageResponseTime() {
		if (opsSuccessful > 0)
			return (double) totalResponseTime / (double) opsSuccessful;
		else
			return 0.0;

	}

	private ISamplingStrategy getResponseTimeSampler() {
		return responseTimeSampler;
	}

	public void merge(OperationSummary from) {
		opsSuccessful += from.opsSuccessful;
		opsFailed += from.opsFailed;
		actionsSuccessful += from.actionsSuccessful;
		totalResponseTime += from.totalResponseTime;
		asyncInvocations += from.asyncInvocations;
		syncInvocations += from.syncInvocations;
		minResponseTime = Math.min(minResponseTime, from.minResponseTime);
		maxResponseTime = Math.max(maxResponseTime, from.maxResponseTime);

		LinkedList<Long> rhsRawSamples = from.getResponseTimeSampler().getRawSamples();
		for (Long obs : rhsRawSamples)
			responseTimeSampler.accept(obs);
	}

	public long getOpsSuccessful() {
		return opsSuccessful;
	}

	public long getOpsFailed() {
		return opsFailed;
	}

	public long getTotalResponseTime() {
		return totalResponseTime;
	}
}
