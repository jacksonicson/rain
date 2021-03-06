package radlab.rain.scoreboard;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import radlab.rain.util.SonarRecorder;
import de.tum.in.sonar.collector.Identifier;
import de.tum.in.sonar.collector.MetricReading;

public class SonarMetricWriter {

	private SonarRecorder sonarRecorder;

	private long lastSnapshotLog = 0;
	private long lastTotalResponseTime = 0;
	private long lastNumObservations = 0;

	private long[] thrBuffer = new long[10];

	public SonarMetricWriter() {
		// Read configuration
		sonarRecorder = SonarRecorder.getInstance();
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
			log(timestamp, "rain.avgrtime." + rtimeStat.targetId, (long) totalAveragegResponseTime);

			// Delta average response time
			log(timestamp, "rain.rtime." + rtimeStat.targetId, (long) deltaAverageResponseTime);

			// Total observations
			log(timestamp, "rain.tobservations." + rtimeStat.targetId, (long) rtimeStat.numObservations);

			// Delta observations
			log(timestamp, "rain.dobservations." + rtimeStat.targetId, (long) deltaObservations);

			// Total response time
			log(timestamp, "rain.trtime." + rtimeStat.targetId, (long) rtimeStat.totalResponseTime);

			// Delta response time
			log(timestamp, "rain.drtime." + rtimeStat.targetId, (long) deltaResponseTime);

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

	public void close() throws Exception {
		sonarRecorder.disconnect();
	}

}
