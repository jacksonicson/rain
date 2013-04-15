package radlab.rain;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.target.ITarget;
import radlab.rain.util.MetricWriter;
import radlab.rain.util.MetricWriterFactory;

public class TargetManager extends Thread {
	private static Logger logger = LoggerFactory.getLogger(TargetManager.class);

	// Reference to the target schedule
	private TargetSchedule schedule;

	// Metric writer configuration
	private MetricWriterFactory.Type metricWriterType;
	private JSONObject metricWriterConf;

	// Timestamp when the whole benchmark (target manager) was started
	private long startBenchmarkTime;

	// List contains all targets that are created
	private List<ITarget> targetsToJoin = new LinkedList<ITarget>();

	TargetManager(TargetSchedule schedule) {
		this.schedule = schedule;
	}

	List<ITarget> getAllTargets() {
		return targetsToJoin;
	}

	void setMetricWriterType(MetricWriterFactory.Type metricWriterType) {
		this.metricWriterType = metricWriterType;
	}

	void setMetricWriterConf(JSONObject conf) {
		this.metricWriterConf = conf;
	}

	private void createTarget(TargetConfiguration conf) throws Exception {
		try {

			List<ITarget> targets = conf.getFactory().createTargets(conf.getHostname());

			// Configure all generated targets
			for (ITarget target : targets) {
				// Create a metric writer
				MetricWriter metricWriter = MetricWriterFactory.createMetricWriter(metricWriterType, metricWriterConf);
				target.setMetricWriter(metricWriter);

				// Set custom timing
				Timing timing = new Timing(conf.getRampUp(), conf.getDuration(), conf.getRampDown());
				target.setTiming(timing);
			}

			// Start targets and add them to the join list
			for (ITarget target : targets) {
				targetsToJoin.add(target);

				target.start();
			}

		} catch (JSONException e) {
			logger.error("Error creating factory targets", e);
		}
	}

	public void run() {
		// Set start benchmark time to now
		startBenchmarkTime = System.currentTimeMillis();

		while (schedule.hasNext()) {
			// Next target configuration
			TargetConfiguration conf = schedule.next();

			// How long to wait for the next target
			long relativeTime = System.currentTimeMillis() - startBenchmarkTime;
			long toWait = conf.getDelay() - relativeTime;

			// Wait for target to start
			delay(toWait);

			// Create and start target with its agents
			try {
				logger.info("Creating target on " + conf.getHostname());
				createTarget(conf);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Wait for all targets to finish
		logger.info("Waiting for all targets to stop");
		for (ITarget target : targetsToJoin) {
			try {
				// Join agents on the target
				target.join();

				// Dispose target
				target.dispose();

				logger.info("Target joined: " + target.getId());

			} catch (InterruptedException e) {
				logger.info("Interrupted while joining target", e);
				continue;
			}
		}
	}

	private void delay(long wait) {
		try {
			if (wait > 0)
				Thread.sleep(wait);
		} catch (InterruptedException e) {
			logger.error("Interrupted thread in target manager", e);
		}
	}

}
