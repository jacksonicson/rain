package radlab.rain.scoreboard;

class ErrorSummary {
	private final String failureClass;
	private long errorCount;

	ErrorSummary(String failureClass) {
		this.failureClass = failureClass;
	}

	void increment() {
		errorCount++;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(this.failureClass).append(": ").append(this.errorCount);
		return buf.toString();
	}
}
