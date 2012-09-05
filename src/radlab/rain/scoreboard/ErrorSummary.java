package radlab.rain.scoreboard;

public class ErrorSummary {
	private String failureClass = "";
	private long errorCount = 0;

	public ErrorSummary(String failureClass) {
		this.failureClass = failureClass;
		this.errorCount = 0;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(this.failureClass).append(": ").append(this.errorCount);
		return buf.toString();
	}
}
