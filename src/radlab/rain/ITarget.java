package radlab.rain;

import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.util.MetricWriter;

public interface ITarget {
	// Timing is set
	public void setTiming(Timing timing);

	// Metric writer is set
	public void setMetricWriter(MetricWriter metricWriter);

	// Initialize is called after setting all references
	public void init(long id) throws Exception;

	// Start is called after initialization
	public void start() throws Exception;

	// Stops target and all its agents
	public void end();

	// Statistics for this target
	public IScoreboard getScoreboard();

	// Returns identifier
	public long getId();
}
