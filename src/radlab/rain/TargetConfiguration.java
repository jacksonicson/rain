package radlab.rain;

import radlab.rain.target.ITargetFactory;

public class TargetConfiguration {

	// Target factory
	private ITargetFactory factory;

	// Domain size
	private int domainSize;

	// Timings
	private long rampUp;
	private long duration;
	private long rampDown;

	// Start offset (beginning at time 0)
	private long offset;

	// Workload profile
	private int workloadProfileIndex;
	private String workloadProfileName;

	// Domain name of the target (optional for static targets)
	private String targetDomainName; 
	
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

	public int getWorkloadProfileIndex() {
		return workloadProfileIndex;
	}

	public void setWorkloadProfileIndex(int workloadProfile) {
		this.workloadProfileIndex = workloadProfile;
	}

	public void setWorkloadProfileName(String workloadProfileName) {
		this.workloadProfileName = workloadProfileName;
	}

	public String getWorkloadProfileName() {
		return this.workloadProfileName;
	}

	public int getDomainSize() {
		return domainSize;
	}

	public void setDomainSize(int domainSize) {
		this.domainSize = domainSize;
	}

	public String getTargetDomainName() {
		return targetDomainName;
	}

	public void setTargetDomainName(String targetDomainName) {
		this.targetDomainName = targetDomainName;
	}
}