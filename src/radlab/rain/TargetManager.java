package radlab.rain;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.target.ITarget;
import radlab.rain.util.MetricWriter;
import radlab.rain.util.SonarMetricWriter;

public class TargetManager extends Thread {
	private static Logger logger = LoggerFactory.getLogger(TargetManager.class);

	// Reference to the target schedule
	private TargetSchedule schedule;

	// Metric writer configuration
	private JSONObject metricWriterConf;

	// Timestamp when the whole benchmark (target manager) was started
	private long startBenchmarkTime;

	// List contains all targets that are created
	private List<ITarget> targetsToJoin = new LinkedList<ITarget>();

	TargetManager(JSONObject config, TargetSchedule schedule) throws JSONException {
		this.schedule = schedule;
		metricWriterConf = config.getJSONObject("metricWriterConf");
	}

	List<ITarget> getAllTargets() {
		return targetsToJoin;
	}

	private void createTarget(TargetConfiguration conf) throws BenchmarkFailedException {
		try {
			// Create targets
			List<ITarget> targets = conf.getFactory().createTargets();

			// Configure all generated targets
			for (ITarget target : targets) {
				// Create a metric writer
				MetricWriter metricWriter = new SonarMetricWriter(metricWriterConf);
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
			throw new BenchmarkFailedException("Error creating target factory", e);
		}
	}

	private void waitForShutdown() {
		// Wait for all targets to finish
		logger.info("Waiting for all targets to stop...");
		for (ITarget target : targetsToJoin) {
			try {
				// Join agents on the target
				target.join();

				// Dispose target
				target.dispose();

				// Log
				logger.info("Target joined: " + target.getId());
			} catch (InterruptedException e) {
				// Doesn't matter, resume shutting down next target
				logger.debug("Interrupted while joining target", e);
				continue;
			}
		}
	}

	private void processSchedule() throws BenchmarkFailedException {
		// Set start benchmark time to now
		startBenchmarkTime = System.currentTimeMillis();

		// Go over the schedule
		while (schedule.hasNext()) {
			// Next target configuration
			TargetConfiguration conf = schedule.next();

			// How long to wait for the next target
			long relativeTime = System.currentTimeMillis() - startBenchmarkTime;
			long toWait = conf.getDelay() - relativeTime;

			// Wait for target to start
			delay(toWait);

			// Create and start target with its agents
			createTarget(conf);
		}
	}

	public void run() {
		// Process the complete schedule
		try {
			processSchedule();
		} catch (BenchmarkFailedException e) {
			logger.error("Benchmark failed while processing target schedule", e);
			System.exit(1);
		}

		// Wait for shutdown
		waitForShutdown();
	}

	private void delay(long wait) {
		long endTime = System.currentTimeMillis() + wait;
		while (System.currentTimeMillis() < endTime) {
			try {
				// Sleep for the given delay
				Thread.sleep(endTime - System.currentTimeMillis());
			} catch (InterruptedException e) {
				// Doesn't matter, this runs in a loop
				logger.debug("Interrupted thread in target manager", e);
			}
		}
	}
}
