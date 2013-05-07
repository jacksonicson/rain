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

import radlab.rain.RainConfig;
import radlab.rain.util.NegativeExponential;
import radlab.rain.util.SonarRecorder;
import de.tum.in.sonar.collector.Identifier;
import de.tum.in.sonar.collector.MetricReading;

public class PoissonSamplingStrategy implements IMetricSampler {
	// All sampled values are stored in Sonar
	private SonarRecorder sonarRecorder;
	private long targetId;
	private String operation;

	// Settings
	private final double meanSamplingInterval = RainConfig.getInstance().meanResponseTimeSamplingInterval;

	// Buffer with all values
	private AllSamplingStrategy sampling = new AllSamplingStrategy();

	// Sampling variables
	private int nextSampleToAccept = 1;
	private int samplesSeen = 0;
	private NegativeExponential random = null;

	public PoissonSamplingStrategy(long targetId, String operation) {
		this.targetId = targetId;
		this.operation = operation;

		// Initialize random number generator
		this.random = new NegativeExponential(this.meanSamplingInterval);

		// Rest this sampler
		reset();

		// Get a Sonar recorder instance
		sonarRecorder = SonarRecorder.getInstance();
	}

	public void reset() {
		this.sampling.reset();

		this.samplesSeen = 0;
		this.nextSampleToAccept = 1;
	}

	public int getSamplesCollected() {
		return sampling.getSamplesCollected();
	}

	@Override
	public int getSamplesSeen() {
		return samplesSeen;
	}

	@Override
	public long getNthPercentile(int pct) {
		return sampling.getNthPercentile(pct);
	}

	public double getSampleMean() {
		return sampling.getSampleMean();
	}

	public double getSampleStandardDeviation() {
		return sampling.getSampleStandardDeviation();
	}

	public double getTvalue(double populationMean) {
		return sampling.getTvalue(populationMean);
	}

	public boolean accept(long value) {
		samplesSeen++;

		if (samplesSeen == nextSampleToAccept) {
			sampling.accept(value);

			// Calculate next sampling index
			double randExp = this.random.nextDouble();
			nextSampleToAccept = this.samplesSeen + (int) Math.ceil(randExp);

			// Write the value to Sonar
			sonarRecord(value);

			return true;
		}

		return false;
	}

	private void sonarRecord(long value) {
		if (sonarRecorder == null)
			return;

		if (targetId < 0)
			return;

		Identifier id = new Identifier();
		id.setSensor("rain.rtime.sampler." + targetId + "." + operation);
		id.setTimestamp(System.currentTimeMillis() / 1000);

		MetricReading mvalue = new MetricReading();
		mvalue.setValue(value);

		sonarRecorder.record(id, mvalue);
	}

	@Override
	public void merge(IMetricSampler responseTimeSampler) {
		sampling.merge(responseTimeSampler);
	}

	public LinkedList<Long> getRawSamples() {
		return sampling.getRawSamples();
	}
}
