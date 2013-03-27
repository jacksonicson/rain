package radlab.rain.communication.thrift;

import java.util.ArrayList;
import java.util.List;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.Benchmark;
import radlab.rain.LoadProfile;
import radlab.rain.Scenario;
import radlab.rain.Target;
import de.tum.in.storm.rain.Profile;
import de.tum.in.storm.rain.RainService;

public class AsyncRainServiceImpl implements RainService.Iface {

	private static Logger logger = LoggerFactory.getLogger(AsyncRainServiceImpl.class);

	@Override
	public boolean startBenchmark(long controllerTimestamp) throws TException {
		logger.debug(this + " Received benchmark start message.");
		Benchmark.getBenchmarkInstance().waitingForStartSignal = false;
		return true;
	}

	@Override
	public boolean dynamicLoadProfile(Profile msg) throws TException {
		// Find the track it should go to and validate it.
		// We should make Scenarios singletons since there's only one
		// Scenario ever (a Scenario holds one or more ScenarioTracks)
		Target track = Benchmark.getBenchmarkScenario().getTracks().get(msg.getDestTrackName());
		if (track != null) {
			logger.info(this + " Found target track");

			LoadProfile profile = new LoadProfile(msg.getInterval(), (int) msg.getNumberOfUsers(), msg.getMixName(), msg.getTransitionTime(),
					msg.getName());

			int validationResult = track.validateLoadProfile(profile);
			// Try to validate and submit to the track's load scheduler
			if (validationResult == Target.VALID_LOAD_PROFILE) {
				logger.info(this + " Profile validated");
				// Submit to load scheduler thread
				track.submitDynamicLoadProfile(profile);

				return true;
			} else // Dynamic LoadProfile failed validation
			{
				logger.info(this + " Profile validation failed!");
				return false;
			}
		} else // Could not find track
		{
			logger.info(this + " Target track not found: " + msg.getDestTrackName());
			return false;
		}
	}

	@Override
	public List<String> getTrackNames() throws TException {
		logger.info(this + " Received track list request message.");

		List<String> trackNames = new ArrayList<String>();
		for (Target track : Benchmark.BenchmarkScenario.getTracks().values()) {
			logger.info(this + " Adding track name: " + track.getName());
			trackNames.add(track.getName());
		}

		return trackNames;
	}

	@Override
	public String toString() {
		return "[AsyncRainServiceImpl]";
	}

	@Override
	public long getRampUpTime() throws TException {
		Scenario scenario = Benchmark.getBenchmarkScenario();
		return scenario.getRampUp();
	}

	@Override
	public long getRampDownTime() throws TException {
		Scenario scenario = Benchmark.getBenchmarkScenario();
		return scenario.getRampDown();
	}

	@Override
	public long getDurationTime() throws TException {
		Scenario scenario = Benchmark.getBenchmarkScenario();
		return scenario.getDuration();
	}
}
