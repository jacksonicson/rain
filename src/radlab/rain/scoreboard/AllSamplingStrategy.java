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

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;


public class AllSamplingStrategy implements IMetricSampler {
	private final int SIZE = 1000;
	private int index;
	private LinkedList<double[]> samples = new LinkedList<double[]>();

	private boolean invalidBuffer = true;
	private double[] buffer;

	public AllSamplingStrategy() {
		reset();
	}

	private void newBucket() {
		double[] bucket = new double[SIZE];
		samples.add(0, bucket);
		index = 0;
	}

	// accept always keeps each sample seen
	@Override
	public boolean accept(long value) {
		double[] bucket = samples.peek();
		bucket[index++] = value;
		invalidBuffer = true;
		if (index >= SIZE)
			newBucket();
		return true;
	}

	private void updateBuffer() {
		if (!invalidBuffer)
			return;

		buffer = new double[SIZE * samples.size()];
		for (int i = 0; i < samples.size(); i++) {
			double[] bucket = samples.get(i);
			System.arraycopy(bucket, 0, buffer, i * SIZE, SIZE);
		}
	}

	@Override
	public long getNthPercentile(int pct) {
		updateBuffer();
		Percentile percentile = new Percentile(pct);
		return (long) percentile.evaluate(buffer, 0, getSamplesCollected());
	}

	@Override
	public LinkedList<Long> getRawSamples() {
		LinkedList<Long> data = new LinkedList<Long>();
		for (int i = 0; i < samples.size(); i++) {
			double[] bucket = samples.get(i);

			int max = SIZE;
			if (i == (samples.size() - 1))
				max = index;

			for (int j = 0; j < max; j++) {
				data.add((long) bucket[j]);
			}
		}
		return data;
	}

	@Override
	public double getSampleMean() {
		updateBuffer();
		Mean mean = new Mean();
		return mean.evaluate(buffer, 0, getSamplesCollected());
	}

	@Override
	public double getSampleStandardDeviation() {
		updateBuffer();
		StandardDeviation sd = new StandardDeviation();
		return sd.evaluate(buffer, 0, getSamplesCollected());
	}

	@Override
	public int getSamplesCollected() {
		return (samples.size() - 1) * SIZE + index;
	}

	@Override
	public int getSamplesSeen() {
		return getSamplesCollected();
	}

	@Override
	public double getTvalue(double populationMean) {
		return 0.0;
	}

	@Override
	public void reset() {
		invalidBuffer = true;
		samples.clear();
		index = 0;
		newBucket();
	}

	@Override
	public void merge(IMetricSampler responseTimeSampler) {
		for(long sample : responseTimeSampler.getRawSamples())
			accept(sample); 
	}
}
