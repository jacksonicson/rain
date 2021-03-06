package radlab.rain.load;

import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.Timing;

public class LoadManager extends Thread {
	private static Logger logger = Logger.getLogger(LoadManager.class);

	// Target id of this load manager
	private long targetId;

	// Current load unit
	private LoadDefinition currentLoad = null;

	// Ramp up time
	private final long rampUp;

	// Keeps running as long as this flag is false
	private boolean interrupted = false;

	// The current load profile index
	private int nextLoadIndex = 0;

	// Random number generator
	private Random random = new Random();

	// Reference to the load schedule
	private LoadSchedule loadSchedule;

	// Set of workload mix identifiers which is used for validation purpose only
	private Set<String> mixes;

	public LoadManager(long targetId, Timing timing, LoadSchedule loadSchedule, Set<String> mixes) {
		this.targetId = targetId;
		this.rampUp = timing.rampUp;
		this.loadSchedule = loadSchedule;
		this.mixes = mixes;

		// Set thread name
		setName("LoadManager");
	}

	public void interrupt() {
		// Set flag and interrupt thread if its waiting
		this.interrupted = true;
		super.interrupt();
	}

	private LoadDefinition getLoadDefinitionInTransition(LoadDefinition nextLoad, long now, long intervalEndTime) {
		// Between interval end and transition end
		double elapsedRatio = (double) (now - intervalEndTime) / (double) currentLoad.getTransitionTime();
		double randomDouble = this.random.nextDouble();

		if (randomDouble <= elapsedRatio)
			return nextLoad;
		else
			return currentLoad;
	}

	public synchronized LoadDefinition getCurrentLoadProfile() {
		// Get next load definition
		LoadDefinition nextLoad = getNextLoadProfile();

		// Calculate when the current interval ends and when the transition ends.
		long now = System.currentTimeMillis();
		long intervalEndTime = currentLoad.getTimeStarted() + currentLoad.getInterval();
		long transitionEndTime = intervalEndTime + currentLoad.getTransitionTime();

		// Is now between current load start and transition end time
		if (now >= currentLoad.getTimeStarted() && now <= transitionEndTime) {
			if (now <= intervalEndTime)
				// During main interval
				return currentLoad;
			else
				// During transition time
				return getLoadDefinitionInTransition(nextLoad, now, intervalEndTime);

		} else if (now > transitionEndTime) {
			return nextLoad;
		} else {
			throw new IllegalStateException("Time now is before current load start time");
		}
	}

	public void run() {
		// The first load profile will be used during ramp-up phase
		synchronized (this) {
			currentLoad = loadSchedule.get(nextLoadIndex);
		}

		// Log ramp up
		logger.info("Ramping up for " + rampUp + "ms.");

		// Sleep during ramp up phase
		try {
			Thread.sleep(rampUp);
		} catch (InterruptedException e) {
			logger.warn("Load manager was interrupted during ramp up phase");
			return;
		}

		// Log ramp up finished
		try {
			JSONObject obj = new JSONObject(); 
			obj.put("targetId", targetId);
			logger.info("Ramp up finished: " + obj.toString());
		} catch (JSONException e1) {
			logger.error("Error while creating JSON object", e1); 
		} 

		// Activate load profile
		currentLoad.activate();

		// Main loop that goes over all load units in the load schedule
		while (!interrupted) {
			try {
				// Sleep for interval length + transition time
				// method to acquire current load will handle transition time
				Thread.sleep(currentLoad.getInterval() + currentLoad.getTransitionTime());

				logger.debug("advancing load schedule");
				synchronized (this) {
					// Advance the schedule and if that returns false, then we're done
					currentLoad = advanceSchedule();
				}
			} catch (InterruptedException e) {
				continue;
			} catch (Exception e) {
				logger.error("Unknown error in load manager", e);
				interrupted = true;
			}
		}
	}

	private LoadDefinition getNextLoadProfile() {
		int nextLoadScheduleIndex = (nextLoadIndex + 1) % loadSchedule.size();
		return loadSchedule.get(nextLoadScheduleIndex);
	}

	public LoadDefinition advanceSchedule() {
		// Update load schedule index
		nextLoadIndex = (nextLoadIndex + 1) % loadSchedule.size();

		// If we reach index 0, we cycled
		if (nextLoadIndex == 0)
			logger.info("cycling load schedule");

		// Get next load unit
		LoadDefinition next = loadSchedule.get(nextLoadIndex);

		// Validate
		if (!validateLoadDefinition(next))
			logger.warn("invalid load definition found");

		// Update profile stats
		next.activate();

		return next;
	}

	private boolean validateLoadDefinition(LoadDefinition load) {
		// Check number of users
		if (load.getNumberOfUsers() < 0) {
			logger.info("Invalid load profile. Number of users < 0. Profile details: " + load.getNumberOfUsers());
			return false;
		}

		// Check references to the mix matrix
		if (load.getMixName() != null) {
			if (load.getMixName().length() > 0 && !mixes.contains(load.getMixName())) {
				logger.info("Invalid load profile. mixname not in track's mixmap. Profile details: " + load.toString());
				return false;
			}
		}

		return true;
	}
}
