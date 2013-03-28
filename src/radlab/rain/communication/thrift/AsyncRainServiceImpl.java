package radlab.rain.communication.thrift;

import java.util.List;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.RainConfig;
import radlab.rain.Scenario;
import de.tum.in.storm.rain.RainService;

public class AsyncRainServiceImpl implements RainService.Iface {

	private static Logger logger = LoggerFactory.getLogger(AsyncRainServiceImpl.class);

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
		List<String> trackNames = scenario.getTrackNames();
		return trackNames;
	}

	@Override
	public String toString() {
		return "[AsyncRainServiceImpl]";
	}

	@Override
	public long getRampUpTime() throws TException {
//		return scenario.getTiming(). 
	}

	@Override
	public long getRampDownTime() throws TException {
//		return scenario.getRampDown();
	}

	@Override
	public long getDurationTime() throws TException {
//		return scenario.getConfig().getDuration();
	}
}
