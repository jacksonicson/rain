package radlab.rain.target;

import radlab.rain.Timing;
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

	// Blocks until all agent threads have joined
	public void joinAgents() throws InterruptedException;

	// Stops all agents and helper threads
	public void dispose();

	// Statistics for this target
	public IScoreboard getScoreboard();

	// Returns identifier
	public long getId();
}
