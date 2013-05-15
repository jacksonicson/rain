package radlab.rain;

import radlab.rain.target.ITargetFactory;

public class TargetConfiguration {

	enum DomainSize {
		SMALL, MEDIUM, LARGE
	}

	// Target factory
	private ITargetFactory factory;

	// Domain size
	private DomainSize domainSize = DomainSize.MEDIUM;

	// Timings
	private long rampUp;
	private long duration;
	private long rampDown;

	// Start offset (beginning at time 0)
	private long offset;

	// Workload profile
	private int workloadProfile;
	private String workloadProfileName;

	// Timing offset within the workload profile TS
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

	public DomainSize getDomainSize() {
		return domainSize;
	}

	public void setDomainSize(DomainSize domainSize) {
		this.domainSize = domainSize;
	}
}