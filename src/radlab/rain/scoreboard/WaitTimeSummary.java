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

import radlab.rain.util.ISamplingStrategy;

public class WaitTimeSummary {
	public long count = 0;
	public long totalWaitTime = 0;
	public long minWaitTime = Long.MAX_VALUE;
	public long maxWaitTime = Long.MIN_VALUE;

	// Sample the response times so that we can give a "reasonable"
	// estimate of the 90th and 99th percentiles.
	private ISamplingStrategy waitTimeSampler;

	public WaitTimeSummary(ISamplingStrategy strategy) {
		this.waitTimeSampler = strategy;
	}

	void dropOff(long waitTime) {
		// Update wait time summary for this operation
		count++;
		totalWaitTime += waitTime;
		if (waitTime < minWaitTime)
			minWaitTime = waitTime;
		if (waitTime > maxWaitTime)
			maxWaitTime = waitTime;

		// Drop sample
		waitTimeSampler.accept(waitTime);
	}

	public long getNthPercentileResponseTime(int pct) {
		return this.waitTimeSampler.getNthPercentile(pct);
	}

	public void resetSamples() {
		this.waitTimeSampler.reset();
	}

	public int getSamplesSeen() {
		return this.waitTimeSampler.getSamplesSeen();
	}

	public int getSamplesCollected() {
		return this.waitTimeSampler.getSamplesCollected();
	}

	public double getAverageWaitTime() {
		if (this.count == 0)
			return 0.0;
		else
			return (double) this.totalWaitTime / (double) this.count;
	}

	public double getSampleMean() {
		return this.waitTimeSampler.getSampleMean();
	}

	public double getSampleStandardDeviation() {
		return this.waitTimeSampler.getSampleStandardDeviation();
	}

	public double getTvalue(double averageWaitTime) {
		return this.waitTimeSampler.getTvalue(averageWaitTime);
	}

	public JSONObject getStatistics() throws JSONException {
		JSONObject wait = new JSONObject();

		wait.put("average_wait_time", getAverageWaitTime());
		wait.put("min_wait_time", minWaitTime);
		wait.put("max_wait_time", maxWaitTime);
		wait.put("90th_percentile_response_time", getNthPercentileResponseTime(90));
		wait.put("99th_percentile_response_time", getNthPercentileResponseTime(99));
		wait.put("samples_collected", getSamplesCollected());
		wait.put("samples_seen", getSamplesSeen());
		wait.put("sample_mean", getSampleMean());
		wait.put("sample_standard_deviation", getSampleStandardDeviation());
		wait.put("tvalue_average_wait_time", getTvalue(getAverageWaitTime()));

		return wait;
	}
}
