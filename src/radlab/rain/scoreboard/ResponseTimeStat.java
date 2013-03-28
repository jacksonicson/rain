package radlab.rain.scoreboard;

import java.io.Serializable;

public class ResponseTimeStat implements Serializable {
	private static final long serialVersionUID = -9080553943704545836L;

	public long timestamp = -1;
	public long responseTime = -1;
	public long totalResponseTime = -1;
	public long numObservations = -1;
	public String operationName = "";
	public String operationRequest = "";
	public String generatedDuring = "";
	public String trackName = "";

	public ResponseTimeStat() {
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[").append(this.generatedDuring).append("] ").append(this.timestamp).append(" ").append(
				this.responseTime).append(" [").append(this.operationRequest).append("] ").append(
				this.totalResponseTime).append(" ").append(this.numObservations);
		return buf.toString();
	}
}
