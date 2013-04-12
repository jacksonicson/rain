package radlab.rain;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.target.ITarget;
import radlab.rain.target.ITargetFactory;
import radlab.rain.util.MetricWriter;
import radlab.rain.util.MetricWriterFactory;

public class TargetManager extends Thread {
	private static Logger logger = LoggerFactory.getLogger(TargetManager.class);

	private Timing timing;
	private TargetSchedule schedule;

	private MetricWriterFactory.Type metricWriterType;
	private JSONObject metricWriterConf;

	private long currentTargetId;

	private List<ITarget> targetsToJoin = new LinkedList<ITarget>();

	public TargetManager(Timing timing, TargetSchedule schedule) {
		this.schedule = schedule;
		this.timing = timing;
	}

	public void setMetricWriterType(MetricWriterFactory.Type metricWriterType) {
		this.metricWriterType = metricWriterType;
	}

	public void setMetricWriterConf(JSONObject conf) {
		this.metricWriterConf = conf;
	}

	private void createTarget(ITargetFactory factory, String hostname) throws Exception {
		try {

			List<ITarget> targets = factory.createTargets(hostname);

			// Configure all generated targets
			for (ITarget target : targets) {
				logger.debug("Initializing target " + target.getId());

				// Create a metric writer
				MetricWriter metricWriter = MetricWriterFactory.createMetricWriter(metricWriterType, metricWriterConf);

				target.setTiming(timing);
				target.setMetricWriter(metricWriter);
				target.init(currentTargetId++);
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
		while (schedule.hasNext()) {
			// Next target configuration
			TargetSchedule.TargetConf conf = schedule.next();

			// How long to wait for the next target
			long relativeTime = System.currentTimeMillis() - this.timing.startSteadyState;
			long toWait = conf.delay - relativeTime;

			// Wait for target to start
			delay(toWait);

			// Create and start target with its agents
			try {
				createTarget(conf.factory, conf.hostname);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Wait for all targets to finish
		for (ITarget target : targetsToJoin) {
			try {
				// Join agents on the target
				target.joinAgents();

				// Dispose target
				target.dispose();
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
