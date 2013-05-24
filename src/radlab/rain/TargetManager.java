package radlab.rain;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.target.ITarget;

public class TargetManager extends Thread {
	private static Logger logger = LoggerFactory.getLogger(TargetManager.class);

	// Reference to the target schedule
	private TargetSchedule schedule;

	// Timestamp when the whole benchmark (target manager) was started
	private long startBenchmarkTime;

	// List contains all targets that are created
	private List<ITarget> targetsToJoin = new LinkedList<ITarget>();

	// Target ID
	private int targetId;

	TargetManager(JSONObject config, TargetSchedule schedule) throws JSONException {
		setName("TargetManager");
		this.schedule = schedule;
	}

	List<ITarget> getAllTargets() {
		return targetsToJoin;
	}

	private void createTarget(TargetConfiguration conf) throws BenchmarkFailedException {
		try {
			// Create targets
			List<ITarget> targets = conf.getFactory().createTargets(conf);

			// Configure all generated targets
			for (ITarget target : targets) {
				// Set target Id
				target.setId(targetId);
				targetId += 1;

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
		logger.info("Waiting for all targets to join...");
		for (ITarget target : targetsToJoin) {
			for (int i = 0; i < 10; i++) {
				try {
					if (target.joinTarget(30000))
						break;
				} catch (InterruptedException e) {
					logger.warn("Interrupted while joining target " + target.getId());
				}
				logger.info("Retrying to join target ... " + i + "th try");
			}

			try {
				if (!target.joinTarget(10000)) {
					logger.error("Unabel to join target " + target.getId());
					continue;
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while joining target " + target.getId());
			}

			// Target was joined
			logger.info("Target joined: " + target.getId());

			// Dispose target
			logger.info("Dispose target: " + target.getId());
			target.dispose();
			logger.info("Target disposed: " + target.getId());

			// Log
			logger.info("Target finished: " + target.getId());
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
			long toWait = conf.getOffset() - relativeTime;

			// Wait for target to start
			delay(toWait);

			// Create and start target with its agents
			createTarget(conf);
		}

		logger.info("Schedule processing complete");
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
		logger.info("Shutting down target manager");
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
