package radlab.rain;

public class BenchmarkFailedException extends Exception {

	private static final long serialVersionUID = -2944585954503323609L;

	private final String reason;

	public BenchmarkFailedException(String reason, Throwable t) {
		super(t);
		this.reason = reason;
	}

	public String toString() {
		return reason;
	}
}
