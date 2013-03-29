package radlab.rain;

import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadManager extends Thread {
	private static Logger logger = LoggerFactory.getLogger(LoadManager.class);

	// Current load unit
	private LoadDefinition currentLoad = null;

	// Ramp up time
	private final long rampUp;

	// Keeps running as long as this flag is false
	private boolean interrupted = false;

	// The current load profile index
	private int loadScheduleIndex = 0;

	// Random number generator
	private Random random = new Random();

	// Reference to the load schedule
	private LoadSchedule loadSchedule;

	// List of workload mix identifiers
	private Set<String> mixes;

	public LoadManager(Timing timing, LoadSchedule loadSchedule, Set<String> mixes) {
		this.rampUp = timing.rampUp;
		this.loadSchedule = loadSchedule;
		this.mixes = mixes;
		setName("LoadManager");
	}

	public void interrupt() {
		this.interrupted = true;
		super.interrupt();
	}

	/**
	 * Returns the current load profile.<br />
	 * <br />
	 * This method handles transitions by splitting each load profile interval into two parts:<br />
	 * 
	 * <pre>
	 *     start                               end
	 *     [ interval proper | transition period ]
	 *                intervalEndTime    transitionEndTime
	 * </pre>
	 * 
	 * During the interval proper the current load profile is simply returned. However, during the transition period,
	 * there is a probability that the next load profile (modulo the entire load profile sequence) will be returned
	 * instead. This probability is proportional to the elapsed time within the transition period (e.g. 10% into the
	 * transition period will yield the current load profile with 10% likelihood and the next load profile with 90%
	 * likelihood).
	 */
	public LoadDefinition getCurrentLoadProfile() {
		// Leave it up to the load manager thread to determine the current and next load profiles
		LoadDefinition nextProfile = getNextLoadProfile();

		// Calculate when the current interval ends and when the transition ends.
		long now = System.currentTimeMillis();
		long intervalEndTime = currentLoad.getTimeStarted() + currentLoad.getInterval();
		long transitionEndTime = intervalEndTime + currentLoad.getTransitionTime();

		if (now >= currentLoad.getTimeStarted() && now <= transitionEndTime) {
			// Must either be in 1) interval proper, or 2) transition period.
			if (now <= intervalEndTime) {
				return currentLoad;
			} else {
				double elapsedRatio = (double) (now - intervalEndTime) / (double) currentLoad.getTransitionTime();
				double randomDouble = this.random.nextDouble();

				// If elapsedTime = 90% then we'll want to select the currentProfile 10% of the time.
				// If elapsedTime = 10% then we'll want to select the currentProfile 90% of the time.
				if (randomDouble <= elapsedRatio) {
					return nextProfile;
				} else {
					return currentLoad;
				}
			}
		} else if (now > transitionEndTime) {
			/*
			 * If we make it here then the load scheduler thread has overslept and has not yet woken up to advance the
			 * load schedule. No worries, we'll just point at what should be the next profile.
			 */
			return nextProfile;
		} else // ( now < currentProfile.getTimestarted() )
		{
			/*
			 * If we make it here, that means the current time is before the current load profile interval. This should
			 * only happen during ramp up when we use the first load profile as the "ramp up" profile.
			 */
			return currentLoad;
		}
	}

	public void run() {
		// The first load profile will be used during ramp-up phase
		currentLoad = loadSchedule.get(loadScheduleIndex);

		// Log ramp up
		logger.info("Ramping up for " + rampUp + "ms.");

		// Sleep during ramp up phase
		try {
			Thread.sleep(rampUp);
		} catch (InterruptedException e) {
			logger.error("Load manager was interrupted during ramp up phase");
			return;
		}

		// Log ramp up finished
		logger.info("Ramp up finished");

		// Activate load profile
		currentLoad.activate();

		// Main loop that goes over all load units in the load schedule
		while (!interrupted) {
			try {
				// Sleep until the next load/behavior change.
				Thread.sleep(currentLoad.getInterval() + currentLoad.getTransitionTime());

				// Advance the schedule and if that returns false, then we're done
				currentLoad = advanceSchedule();
			} catch (InterruptedException e) {
				// This is ok
				interrupted = true;
			} catch (Exception e) {
				logger.error("Unknown error in load manager", e);
				interrupted = true;
			}
		}

		// Log finished
		logger.info("finished");
	}

	private LoadDefinition getNextLoadProfile() {
		int nextLoadScheduleIndex = (loadScheduleIndex + 1) % loadSchedule.size();
		return loadSchedule.get(nextLoadScheduleIndex);
	}

	private boolean validateLoadDefinition(LoadDefinition profile) {
		// Check number of users
		if (profile.numberOfUsers <= 0) {
			logger.info("Invalid load profile. Number of users <= 0. Profile details: " + profile.toString());
			return false;
		}

		// Check references to the mix matrix
		if (profile.mixName.length() > 0 && !mixes.contains(profile.mixName)) {
			logger.info("Invalid load profile. mixname not in track's mixmap. Profile details: " + profile.toString());
			return false;
		}

		return true;
	}

	public LoadDefinition advanceSchedule() {
		// Update load schedule index
		loadScheduleIndex = (loadScheduleIndex + 1) % loadSchedule.size();

		// If we reach index 0, we cycled
		if (loadScheduleIndex == 0)
			logger.info("cycling load schedule");

		// Update the track's reference of the current load profile.
		logger.debug("advancing load schedule");

		// Get next load unit
		LoadDefinition next = loadSchedule.get(loadScheduleIndex);

		// Validate
		if (!validateLoadDefinition(next))
			logger.warn("invalid load definition found");

		// Update profile stats
		next.activate();

		return next;
	}
}
