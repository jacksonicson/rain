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

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.util.IMetricSampler;

class WaitTimeSummary {
	private long count = 0;
	private long totalWaitTime = 0;
	private long minWaitTime = Long.MAX_VALUE;
	private long maxWaitTime = Long.MIN_VALUE;

	// Sample the response times so that we can give a "reasonable"
	// estimate of the 90th and 99th percentiles.
	private IMetricSampler waitTimeSampler;

	WaitTimeSummary(IMetricSampler strategy) {
		this.waitTimeSampler = strategy;
	}

	void dropOff(long waitTime) {
		count++;
		totalWaitTime += waitTime;
		minWaitTime = Math.min(minWaitTime, waitTime);
		maxWaitTime = Math.max(maxWaitTime, waitTime);
		waitTimeSampler.accept(waitTime);
	}

	void resetSamples() {
		waitTimeSampler.reset();
	}

	private long getNthPercentileResponseTime(int pct) {
		return waitTimeSampler.getNthPercentile(pct);
	}

	public JSONObject getStatistics() throws JSONException {
		// Calculations
		long minWaitTime = this.minWaitTime;
		if (minWaitTime == Long.MAX_VALUE)
			minWaitTime = 0;

		long maxWaitTime = this.maxWaitTime;
		if (maxWaitTime == Long.MIN_VALUE)
			maxWaitTime = 0;

		double avgWaitTime = 0;
		if (count > 0)
			avgWaitTime = (double) this.totalWaitTime / (double) this.count;

		double tvalue = waitTimeSampler.getTvalue(avgWaitTime);
		double sampleStdDev = waitTimeSampler.getSampleStandardDeviation();

		// Results
		JSONObject wait = new JSONObject();
		wait.put("average_wait_time", avgWaitTime);
		wait.put("total_wait_time", totalWaitTime);
		wait.put("min_wait_time", minWaitTime);
		wait.put("max_wait_time", maxWaitTime);
		wait.put("90th_percentile_wait_time", getNthPercentileResponseTime(90));
		wait.put("99th_percentile_wait_time", getNthPercentileResponseTime(99));
		wait.put("samples_collected", waitTimeSampler.getSamplesCollected());
		wait.put("samples_seen", waitTimeSampler.getSamplesSeen());
		wait.put("sample_mean", waitTimeSampler.getSampleMean());
		wait.put("sample_standard_deviation", sampleStdDev);
		wait.put("tvalue_average_wait_time", tvalue);

		return wait;
	}
}
