package radlab.rain.util;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import radlab.rain.RainConfig;
import radlab.rain.scoreboard.ResponseTimeStat;
import de.tum.in.sonar.collector.Identifier;
import de.tum.in.sonar.collector.MetricReading;

public class SonarMetricWriter extends MetricWriter {

	private static final Logger logger = Logger.getLogger(SonarMetricWriter.class);

	private SonarRecorder sonarRecorder;

	private long lastSnapshotLog = 0;
	private long lastTotalResponseTime = 0;
	private long lastNumObservations = 0;

	private long[] thrBuffer = new long[10];

	public SonarMetricWriter(JSONObject config) throws Exception {
		super(config);

		System.out.println("Start SonarMetricWriter");

		// Read configuration
		String sonarServer = RainConfig.getInstance()._sonarHost;
		logger.debug("Sonar server: " + sonarServer);
		sonarRecorder = new SonarRecorder(sonarServer);
	}

	@Override
	public String getDetails() {
		return "SonarMetricWriter";
	}

	@Override
	public boolean write(ResponseTimeStat stat) throws Exception {

		if (lastSnapshotLog == 0) {
			lastSnapshotLog = System.currentTimeMillis();
			lastTotalResponseTime = stat._totalResponseTime;
			lastNumObservations = stat._numObservations;
		}

		int responseTime = (int) (stat._responseTime / 1000);
		if (responseTime < (thrBuffer.length - 1)) {
			thrBuffer[responseTime]++;
		} else {
			thrBuffer[thrBuffer.length - 1]++;
		}

		long delta = (System.currentTimeMillis() - lastSnapshotLog);
		if (delta > 3000) {

			Identifier id = new Identifier();
			id.setTimestamp(stat._timestamp / 1000);
			MetricReading value = new MetricReading();

			double totalAveragegResponseTime = stat._totalResponseTime / stat._numObservations;
			double deltaObservations = stat._numObservations - lastNumObservations;
			double deltaResponseTime = stat._totalResponseTime - lastTotalResponseTime;
			double deltaAverageResponseTime = deltaResponseTime / deltaObservations;

			// Total average response time
			id.setSensor("rain.avgrtime." + stat._trackName);
			value.setValue(totalAveragegResponseTime);
			sonarRecorder.record(id, value);

			// Delta average response time
			id.setSensor("rain.rtime." + stat._trackName);
			value.setValue(deltaAverageResponseTime);
			sonarRecorder.record(id, value);

			// Total observations
			id.setSensor("rain.tobservations." + stat._trackName);
			value.setValue(stat._numObservations);
			sonarRecorder.record(id, value);

			// Delta observations
			id.setSensor("rain.dobservations." + stat._trackName);
			value.setValue(deltaObservations);
			sonarRecorder.record(id, value);

			// Total response time
			id.setSensor("rain.trtime." + stat._trackName);
			value.setValue(stat._totalResponseTime);
			sonarRecorder.record(id, value);

			// Delta response time
			id.setSensor("rain.drtime." + stat._trackName);
			value.setValue(deltaResponseTime);
			sonarRecorder.record(id, value);

			// Log thrBuffer
			for (int i = 0; i < thrBuffer.length; i++) {
				id.setSensor("rain.thr-" + i + "." + stat._trackName);
				value.setValue(thrBuffer[i]);
				sonarRecorder.record(id, value);
			}

			// Update deltas
			lastSnapshotLog = System.currentTimeMillis();
			lastTotalResponseTime = stat._totalResponseTime;
			lastNumObservations = stat._numObservations;

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
