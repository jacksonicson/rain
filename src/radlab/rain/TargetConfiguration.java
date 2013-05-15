package radlab.rain;

import radlab.rain.target.ITargetFactory;

public class TargetConfiguration {

	private long offset;
	private long rampUp;
	private long duration;
	private long rampDown;
	private ITargetFactory factory;

	private int workloadProfile;
	private String workloadProfileName;
	private long workloadProfileOffset;

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
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

	public ITargetFactory getFactory() {
		return factory;
	}

	public void setFactory(ITargetFactory factory) {
		this.factory = factory;
	}

	public int getWorkloadProfile() {
		return workloadProfile;
	}

	public void setWorkloadProfile(int workloadProfile) {
		this.workloadProfile = workloadProfile;
	}

	public void setWorkloadProfileName(String workloadProfileName) {
		this.workloadProfileName = workloadProfileName;
	}

	public String getWorkloadProfileName() {
		return this.workloadProfileName;
	}

	public long getWorkloadProfileOffset() {
		return workloadProfileOffset;
	}

	public void setWorkloadProfileOffset(long workloadProfileOffset) {
		this.workloadProfileOffset = workloadProfileOffset;
	}

}