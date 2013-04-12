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

	private TargetSchedule schedule;

	private MetricWriterFactory.Type metricWriterType;
	private JSONObject metricWriterConf;

	private long currentTargetId;

	private long startBenchmarkTime;

	private List<ITarget> targetsToJoin = new LinkedList<ITarget>();

	public TargetManager(TargetSchedule schedule) {
		this.schedule = schedule;
	}

	public void setMetricWriterType(MetricWriterFactory.Type metricWriterType) {
		this.metricWriterType = metricWriterType;
	}

	public void setMetricWriterConf(JSONObject conf) {
		this.metricWriterConf = conf;
	}

	private void createTarget(TargetSchedule.TargetConf conf) throws Exception {
		try {

			List<ITarget> targets = conf.factory.createTargets(conf.hostname);

			// Configure all generated targets
			for (ITarget target : targets) {
				logger.debug("Initializing target " + target.getId());

				// Create a metric writer
				MetricWriter metricWriter = MetricWriterFactory.createMetricWriter(metricWriterType, metricWriterConf);
				target.setMetricWriter(metricWriter);

				// Set custom timing
				Timing timing = new Timing(conf.rampUp, conf.duration, conf.rampDown);
				target.setTiming(timing);

				// Initialize
				target.init(currentTargetId++);
			}

			// Start targets and add them to the join list
			for (ITarget target : targets) {
				targetsToJoin.add(target);
				
				target.setup(); 
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
			TargetSchedule.TargetConf conf = schedule.next();

			// How long to wait for the next target
			long relativeTime = System.currentTimeMillis() - startBenchmarkTime;
			long toWait = conf.delay - relativeTime;

			// Wait for target to start
			delay(toWait);

			// Create and start target with its agents
			try {
				logger.info("Creating target on " + conf.hostname);
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
				target.joinAgents();

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
