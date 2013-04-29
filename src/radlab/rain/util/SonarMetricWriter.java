package radlab.rain.util;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.json.JSONObject;

import radlab.rain.scoreboard.ResponseTimeStat;
import de.tum.in.sonar.collector.Identifier;
import de.tum.in.sonar.collector.MetricReading;

public class SonarMetricWriter extends MetricWriter {

	private SonarRecorder sonarRecorder;

	private long lastSnapshotLog = 0;
	private long lastTotalResponseTime = 0;
	private long lastNumObservations = 0;

	private long[] thrBuffer = new long[10];

	public SonarMetricWriter(JSONObject config) {
		super(config);

		System.out.println("Start SonarMetricWriter");

		// Read configuration
		sonarRecorder = SonarRecorder.getInstance();
	}

	@Override
	public String getDetails() {
		return "SonarMetricWriter";
	}

	private final int BUFFER = 3000;
	private int aggregationLength = 0;
	private long[] rtime = new long[BUFFER];

	private void updateCalculations(ResponseTimeStat rtimeStat) {
		// Response time buckets
		int responseTime = (int) (rtimeStat.responseTime / 1000);
		if (responseTime < (thrBuffer.length - 1)) {
			thrBuffer[responseTime]++;
		} else {
			thrBuffer[thrBuffer.length - 1]++;
		}

		rtime[aggregationLength] = rtimeStat.responseTime;

		// Fill current aggregation object
		aggregationLength++;
	}

	private void resetCalculations() {
		aggregationLength = 0;

		// Clear buffers
		for (int i = 0; i < thrBuffer.length; i++)
			thrBuffer[i] = 0;
	}

	private long[] calcMinMaxRTime() {
		long min = Long.MAX_VALUE;
		long max = 0;
		for (int i = 0; i < aggregationLength; i++) {
			long test = rtime[i];
			min = Math.min(min, test);
			max = Math.max(max, test);
		}

		return new long[] { min, max };
	}

	private double[] calcPercentileRTime(double[] ps) {
		double[] data = new double[BUFFER];
		for (int j = 0; j < aggregationLength; j++) {
			data[j] = rtime[j];
		}

		double[] res = new double[ps.length];
		for (int i = 0; i < ps.length; i++) {
			Percentile percentile = new Percentile(ps[i]);
			res[i] = percentile.evaluate(data, 0, aggregationLength);
		}

		return res;
	}

	private final void log(long timestamp, String name, long value) {
		Identifier id = new Identifier();
		id.setTimestamp(timestamp);
		id.setSensor(name);
		MetricReading reading = new MetricReading();
		reading.setValue(value);
		sonarRecorder.record(id, reading);
	}

	@Override
	public boolean write(ResponseTimeStat rtimeStat) {

		if (lastSnapshotLog == 0) {
			lastSnapshotLog = System.currentTimeMillis();
			lastTotalResponseTime = rtimeStat.totalResponseTime;
			lastNumObservations = rtimeStat.numObservations;
		}

		// Delta since the last snapshot
		long delta = (System.currentTimeMillis() - lastSnapshotLog);

		// Update aggregation
		updateCalculations(rtimeStat);

		// Snapshot metrics
		if (delta > 3000) {
			Identifier id;
			long timestamp = rtimeStat.timestamp / 1000;
			MetricReading value;

			double totalAveragegResponseTime = rtimeStat.totalResponseTime / rtimeStat.numObservations;
			double deltaObservations = rtimeStat.numObservations - lastNumObservations;
			double deltaResponseTime = rtimeStat.totalResponseTime - lastTotalResponseTime;
			double deltaAverageResponseTime = deltaResponseTime / deltaObservations;

			// Total average response time
			id = new Identifier();
			id.setTimestamp(timestamp);
			id.setSensor("rain.avgrtime." + rtimeStat.targetId);
			value = new MetricReading();
			value.setValue(totalAveragegResponseTime);
			sonarRecorder.record(id, value);

			// Delta average response time
			id = new Identifier();
			id.setTimestamp(timestamp);
			id.setSensor("rain.rtime." + rtimeStat.targetId);
			value = new MetricReading();
			value.setValue(deltaAverageResponseTime);
			sonarRecorder.record(id, value);

			// Total observations
			id = new Identifier();
			id.setTimestamp(timestamp);
			id.setSensor("rain.tobservations." + rtimeStat.targetId);
			value = new MetricReading();
			value.setValue(rtimeStat.numObservations);
			sonarRecorder.record(id, value);

			// Delta observations
			id = new Identifier();
			id.setTimestamp(timestamp);
			id.setSensor("rain.dobservations." + rtimeStat.targetId);
			value = new MetricReading();
			value.setValue(deltaObservations);
			sonarRecorder.record(id, value);

			// Total response time
			id = new Identifier();
			id.setTimestamp(timestamp);
			id.setSensor("rain.trtime." + rtimeStat.targetId);
			value = new MetricReading();
			value.setValue(rtimeStat.totalResponseTime);
			sonarRecorder.record(id, value);

			// Delta response time
			id = new Identifier();
			id.setTimestamp(timestamp);
			id.setSensor("rain.drtime." + rtimeStat.targetId);
			value = new MetricReading();
			value.setValue(deltaResponseTime);
			sonarRecorder.record(id, value);

			// Log min max and percentile response times
			long[] minmax = calcMinMaxRTime();
			double[] pths = calcPercentileRTime(new double[] { 0.5, 0.9, 0.99 });
			log(timestamp, "rain.rtime.min." + rtimeStat.targetId, minmax[0]);
			log(timestamp, "rain.rtime.max." + rtimeStat.targetId, minmax[1]);
			log(timestamp, "rain.rtime.50th." + rtimeStat.targetId, (long) pths[0]);
			log(timestamp, "rain.rtime.90th." + rtimeStat.targetId, (long) pths[1]);
			log(timestamp, "rain.rtime.99th." + rtimeStat.targetId, (long) pths[2]);

			// Log thrBuffer
			for (int i = 0; i < thrBuffer.length; i++) {
				id = new Identifier();
				id.setTimestamp(timestamp);
				id.setSensor("rain.thr-" + i + "." + rtimeStat.targetId);
				value = new MetricReading();
				value.setValue(thrBuffer[i]);
				sonarRecorder.record(id, value);
			}

			// Update deltas
			lastSnapshotLog = System.currentTimeMillis();
			lastTotalResponseTime = rtimeStat.totalResponseTime;
			lastNumObservations = rtimeStat.numObservations;

			resetCalculations();
		}

		return false;
	}

	@Override
	public void close() throws Exception {
		sonarRecorder.disconnect();
	}

}
