package radlab.rain;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public final class Timing {
	private static Logger logger = Logger.getLogger(Timing.class);

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

		start = System.currentTimeMillis();
		startSteadyState = start + rampUp;
		endSteadyState = startSteadyState + duration;
		endRun = endSteadyState + rampDown;

		try {
			log();
		} catch (JSONException e) {
			logger.error("Could not log timings", e);
			throw new BenchmarkFailedException("Could not log timings", e);
		}
	}

	public Timing(Timing timing) throws BenchmarkFailedException {
		this(timing.rampUp, timing.duration, timing.rampDown);
	}

	public final long steadyStateDuration() {
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

	public boolean inSteadyState(long time) {
		return (time >= startSteadyState && time <= endSteadyState);
	}

	public boolean inRampDown(long time) {
		return (time > endSteadyState);
	}
}
