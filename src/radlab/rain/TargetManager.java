package radlab.rain;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.target.ITarget;

public class TargetManager extends Thread {
	private static Logger logger = Logger.getLogger(TargetManager.class);

	// Reference to the target schedule
	private TargetSchedule schedule;

	// Timestamp when the whole benchmark (target manager) was started
	private long startBenchmarkTime;

	// List contains all targets that are created
	private List<ITarget> targetsToJoin = new LinkedList<ITarget>();

	// Target ID
	private int targetId;

	TargetManager(JSONObject config, TargetSchedule schedule) throws JSONException {
		// Set thread name
		setName("TargetManager");

		// Set scheduler
		this.schedule = schedule;
	}

	List<ITarget> getAllTargets() {
		return targetsToJoin;
	}

	private void createAndStartTargets(TargetConfiguration conf) throws BenchmarkFailedException {
		try {
			// Create targets
			List<ITarget> targets = conf.getFactory().createTargets(conf);

			// Configure all generated targets
			for (ITarget target : targets) {
				// Set a global target Id
				target.setId(targetId++);
				
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
		int i = 0;
		for (ITarget target : targetsToJoin) {
			while (true) {
				try {
					if (target.joinTarget(30000))
						break;
				} catch (InterruptedException e) {
					logger.warn("Interrupted while joining target " + target.getId());
				}
				logger.info("Retrying to join target ... " + target.getId() + " is " + i + " of "
						+ targetsToJoin.size() + " in " + target.getEnd());
			}

			// Dispose target
			logger.info("Dispose target: " + target.getId());
			try {
				target.dispose();
				logger.info("Target disposed: " + target.getId());
			} catch (Exception ne) {
				logger.error("Could not dispose target", ne);
			}
			i++;
		}
	}

	private void processSchedule() throws BenchmarkFailedException {
		// Set start benchmark time to now
		startBenchmarkTime = System.currentTimeMillis();

		while (schedule.hasNext()) {
			// Next target configuration
			TargetConfiguration conf = schedule.next();

			// How long to wait for the next target
			long relativeTime = System.currentTimeMillis() - startBenchmarkTime;
			long toWait = conf.getOffset() - relativeTime;

			// Wait until target start time is reached
			delay(toWait);

			// Create and start target with its agents
			createAndStartTargets(conf);
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

	public void joinTargetManagear() {
		// Wait until the thread joins
		while (true) {
			try {
				join();
				break;
			} catch (InterruptedException e) {
				// Continue if interrupted
				logger.info("Target manager interrupted while joining");
				continue;
			}
		}
	}
}
