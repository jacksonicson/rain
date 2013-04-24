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

package radlab.rain.util;

import java.util.Collections;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.Benchmark;
import de.tum.in.sonar.collector.Identifier;
import de.tum.in.sonar.collector.MetricReading;

public class PoissonSamplingStrategy implements IMetricSampler {

	private static Logger logger = LoggerFactory.getLogger(Benchmark.class);

	// Sonar recorder
	private SonarRecorder sonarRecorder;
	private String operation;

	private LinkedList<Long> _samples = new LinkedList<Long>();
	private int _nextSampleToAccept = 1;
	private int _currentSample = 0;
	private double _meanSamplingInterval = 1.0;
	private NegativeExponential _expRandom = null;
	private long _sampleSum = 0;

	public static long getNthPercentile(int pct, LinkedList<Long> samples) {
		if (samples.size() == 0)
			return 0;
		Collections.sort(samples);
		int index = (int) Math.round((double) (pct * (samples.size() + 1)) / 100.0);
		if (index < samples.size())
			return samples.get(index).longValue();
		else
			return samples.get(samples.size() - 1); // Return the second last sample
	}

	public PoissonSamplingStrategy(String operation, double meanSamplingInterval) {
		this._meanSamplingInterval = meanSamplingInterval;
		this._expRandom = new NegativeExponential(this._meanSamplingInterval);
		this.operation = operation;
		this.reset();

		this.sonarRecorder = SonarRecorder.getInstance();
	}

	public double getMeanSamplingInterval() {
		return this._meanSamplingInterval;
	}

	public void setMeanSamplingInterval(double val) {
		this._meanSamplingInterval = val;
	}

	public void reset() {
		this._currentSample = 0;
		this._nextSampleToAccept = 1;
		this._samples.clear();
		this._sampleSum = 0;
	}

	public int getSamplesCollected() {
		return this._samples.size();
	}

	public int getSamplesSeen() {
		return this._currentSample;
	}

	public long getNthPercentile(int pct) {
		return PoissonSamplingStrategy.getNthPercentile(pct, this._samples);
	}

	public double getSampleMean() {
		long samples = this.getSamplesCollected();
		if (samples == 0)
			return 0.0;
		else
			return (double) this._sampleSum / (double) samples;
	}

	public double getSampleStandardDeviation() {
		long samples = this.getSamplesCollected();
		if (samples == 0 || samples == 1)
			return 0.0;

		double sampleMean = this.getSampleMean();

		// Sum the deviations from the mean for all items
		double deviationSqSum = 0.0;
		for (Long value : this._samples) {
			// Print out value so we can debug the sd computation
			// logger.info( value );
			deviationSqSum += Math.pow((double) (value - sampleMean), 2);
		}
		// Divide deviationSqSum by N-1 then return the square root
		return Math.sqrt(deviationSqSum / (double) (samples - 1));
	}

	public double getTvalue(double populationMean) {
		long samples = this.getSamplesCollected();
		if (samples == 0 || samples == 1)
			return 0.0;

		double ret = (this.getSampleMean() - populationMean)
				/ (this.getSampleStandardDeviation() / Math.sqrt(this.getSamplesCollected()));
		if (Double.isNaN(ret))
			ret = 0;
		if (Double.isInfinite(ret))
			ret = 0;
		return ret;
	}

	public boolean accept(long value) {
		this._currentSample++;

		if (this._currentSample == this._nextSampleToAccept) {
			this._sampleSum += value;
			this._samples.add(value);
			// Update the nextSampleToAccept
			double randExp = this._expRandom.nextDouble();
			// logger.info( "Random exp: " + randExp );
			this._nextSampleToAccept = this._currentSample + (int) Math.ceil(randExp);
			// logger.info("Next sample to accept: " + this._nextSampleToAccept);

			if (sonarRecorder != null) {
				Identifier id = new Identifier();
				id.setSensor("rain.rtime.sampler." + this.operation);
				id.setTimestamp(System.currentTimeMillis() / 1000);

				MetricReading mvalue = new MetricReading();
				mvalue.setValue(value);

				sonarRecorder.record(id, mvalue);
			}

			return true;
		}
		return false;
	}

	public LinkedList<Long> getRawSamples() {
		return this._samples;
	};
}
