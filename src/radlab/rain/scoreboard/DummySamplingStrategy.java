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

import java.util.Collections;
import java.util.List;

public class DummySamplingStrategy implements IMetricSampler {
	public DummySamplingStrategy() {
	}

	@Override
	public boolean accept(long value) {
		return false;
	}

	@Override
	public double getMeanSamplingInterval() {
		return 0;
	}

	@Override
	public long getNthPercentile(int pct) {
		return 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Long> getRawSamples() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public double getSampleMean() {
		return 0;
	}

	@Override
	public double getSampleStandardDeviation() {
		return 0;
	}

	@Override
	public int getSamplesCollected() {
		return 0;
	}

	@Override
	public int getSamplesSeen() {
		return 0;
	}

	@Override
	public double getTvalue(double populationMean) {
		return 0;
	}

	@Override
	public void reset() {
	}

	@Override
	public void setMeanSamplingInterval(double val) {
	}

	@Override
	public void merge(IMetricSampler responseTimeSampler) {
	}
}
