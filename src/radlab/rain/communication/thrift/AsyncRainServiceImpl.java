package radlab.rain.communication.thrift;

import java.util.ArrayList;
import java.util.List;

import org.apache.thrift.TException;

import radlab.rain.Benchmark;
import radlab.rain.LoadProfile;
import radlab.rain.ScenarioTrack;
import de.tum.in.storm.rain.Profile;
import de.tum.in.storm.rain.RainService;

public class AsyncRainServiceImpl implements RainService.Iface {

	@Override
	public boolean startBenchmark(long controllerTimestamp) throws TException {
		System.out.println(this + " Received benchmark start message.");
		Benchmark.getBenchmarkInstance().waitingForStartSignal = false;
		return true;
	}

	@Override
	public boolean dynamicLoadProfile(Profile msg) throws TException {
		// Find the track it should go to and validate it.
		// We should make Scenarios singletons since there's only one
		// Scenario ever (a Scenario holds one or more ScenarioTracks)
		ScenarioTrack track = Benchmark.getBenchmarkScenario().getTracks().get(msg.getDestTrackName());
		if (track != null) {
			System.out.println(this + " Found target track");

			LoadProfile profile = new LoadProfile(msg.getInterval(), (int) msg.getNumberOfUsers(), msg.getMixName(),
					msg.getTransitionTime(), msg.getName());

			int validationResult = track.validateLoadProfile(profile);
			// Try to validate and submit to the track's load scheduler
			if (validationResult == ScenarioTrack.VALID_LOAD_PROFILE) {
				System.out.println(this + " Profile validated");
				// Submit to load scheduler thread
				track.submitDynamicLoadProfile(profile);

				return true;
			} else // Dynamic LoadProfile failed validation
			{
				System.out.println(this + " Profile validation failed!");
				return false;
			}
		} else // Could not find track
		{
			System.out.println(this + " Target track not found: " + msg.getDestTrackName());
			return false;
		}
	}

	@Override
	public List<String> getTrackNames() throws TException {
		System.out.println(this + " Received track list request message.");

		List<String> trackNames = new ArrayList<String>();
		for (ScenarioTrack track : Benchmark.BenchmarkScenario.getTracks().values()) {
			System.out.println(this + " Adding track name: " + track.getName());
			trackNames.add(track.getName());
		}

		return trackNames;
	}

	@Override
	public String toString() {
		return "[AsyncRainServiceImpl]";
	}
}
