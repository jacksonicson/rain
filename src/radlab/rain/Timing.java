package radlab.rain;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Timing {
	private static Logger logger = LoggerFactory.getLogger(Timing.class);

	/**
	 * Amount of time (in milliseconds) to wait before threads start issuing requests. This allows all of the threads to
	 * start synchronously.
	 */
	private static final long TIME_TO_START = 10000;

	public final long start; // start of ramp up phase
	public final long startSteadyState; // start steady state phase
	public final long endSteadyState; // end steady state phase
	public final long endRun; // end ramp down phase

	public final long rampUp;
	public final long duration;
	public final long rampDown;

	public Timing(long rampUp, long duration, long rampDown) throws BenchmarkFailedException {
		this.rampUp = rampUp;
		this.duration = duration;
		this.rampDown = rampDown;

		// Calculate the run timings that will be used for all threads.
		// | ramp up |------ duration ------| ramp down |
		start = System.currentTimeMillis() + Timing.TIME_TO_START;
		startSteadyState = start + (rampUp * 1000);
		endSteadyState = startSteadyState + (duration * 1000);
		endRun = endSteadyState + (rampDown * 1000);

		try {
			log();
		} catch (JSONException e) {
			logger.error("Could not log timings", e);
			throw new BenchmarkFailedException("COuld not log timings", e);
		}
	}

	final long steadyStateDuration() {
		return endSteadyState - startSteadyState;
	}

	final void log() throws JSONException {
		JSONObject schedule = new JSONObject();
		schedule.put("start", start);
		schedule.put("startSteadyState", startSteadyState);
		schedule.put("endSteadyState", endSteadyState);
		schedule.put("endRun", endRun);
		logger.info("Schedule: " + schedule.toString());
	}

	public boolean inRampUp(long now) {
		return now < startSteadyState;
	}
}
