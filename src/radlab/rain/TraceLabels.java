package radlab.rain;

public enum TraceLabels {

	NO_TRACE_LABEL("[NONE]"), STEADY_STATE_TRACE_LABEL("[STEADY-STATE]"), LATE_LABEL("[LATE]"), RAMP_UP_LABEL("[RAMP-UP]"), RAMP_DOWN_LABEL(
			"[RAMP-DOWN]");

	private String label; 
	
	private TraceLabels(String label) {
		this.label = label; 
	}
	
	public String toString()
	{
		return this.label; 
	}
}
