package radlab.rain.communication.thrift;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import radlab.rain.RainConfig;
import radlab.rain.Scenario;
import de.tum.in.storm.rain.RainService;

public class AsyncRainServiceImpl implements RainService.Iface {

	private static Logger logger = Logger.getLogger(AsyncRainServiceImpl.class);

	private Scenario scenario;

	public AsyncRainServiceImpl(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public boolean startBenchmark(long controllerTimestamp) throws TException {
		logger.debug("received benchmark start message");
		RainConfig.getInstance().waitForStartSignal = false;
		return true;
	}

	@Override
	public List<String> getTrackNames() throws TException {
		logger.info("listing all track names");
		List<String> trackNames = scenario.getTargetNames();
		return trackNames;
	}

	@Override
	public String toString() {
		return "[AsyncRainServiceImpl]";
	}

	@Override
	public long getDurationTime() throws TException {
		return scenario.getTargetSchedule().duration();
	}
}
