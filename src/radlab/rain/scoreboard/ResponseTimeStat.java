package radlab.rain.scoreboard;

public final class ResponseTimeStat {
	public final long timestamp;
	public final long responseTime;
	public final long totalResponseTime;
	public final long numObservations;
	public final long targetId;
	public final String operationName;
	public final String operationRequest;
	public final String generatedDuring;

	public ResponseTimeStat(long timestamp, long responseTime, long totalResponseTime, long numObservations,
			String operationName, String operationRequest, String generatedDuring, long targetId) {
		this.timestamp = timestamp;
		this.responseTime = responseTime;
		this.totalResponseTime = totalResponseTime;
		this.numObservations = numObservations;
		this.targetId = targetId;
		this.operationName = operationName;
		this.operationRequest = operationRequest;
		this.generatedDuring = generatedDuring;
	}
}
