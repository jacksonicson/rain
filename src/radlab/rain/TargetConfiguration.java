package radlab.rain;

import radlab.rain.target.ITargetFactory;

public class TargetConfiguration {
	private long delay;
	private long rampUp;
	private long duration;
	private long rampDown;
	private String hostname;
	private ITargetFactory factory;

	public TargetConfiguration() {
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public long getRampUp() {
		return rampUp;
	}

	public void setRampUp(long rampUp) {
		this.rampUp = rampUp;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public long getRampDown() {
		return rampDown;
	}

	public void setRampDown(long rampDown) {
		this.rampDown = rampDown;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public ITargetFactory getFactory() {
		return factory;
	}

	public void setFactory(ITargetFactory factory) {
		this.factory = factory;
	}
}