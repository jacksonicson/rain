package radlab.rain.util;

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

	@Override
	public boolean write(ResponseTimeStat rtimeStat) {

		if (lastSnapshotLog == 0) {
			lastSnapshotLog = System.currentTimeMillis();
			lastTotalResponseTime = rtimeStat.totalResponseTime;
			lastNumObservations = rtimeStat.numObservations;
		}

		int responseTime = (int) (rtimeStat.responseTime / 1000);
		if (responseTime < (thrBuffer.length - 1)) {
			thrBuffer[responseTime]++;
		} else {
			thrBuffer[thrBuffer.length - 1]++;
		}

		long delta = (System.currentTimeMillis() - lastSnapshotLog);
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

			// Clear buffers
			for (int i = 0; i < thrBuffer.length; i++)
				thrBuffer[i] = 0;
		}

		return false;
	}

	@Override
	public void close() throws Exception {
		sonarRecorder.disconnect();
	}

}
