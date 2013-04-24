package radlab.rain.target;

import radlab.rain.Timing;
import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.util.MetricWriter;

public interface ITarget extends Runnable {
	// Timing is set
	void setTiming(Timing timing);

	// Metric writer is set
	void setMetricWriter(MetricWriter metricWriter);

	// Set identifier
	void setId(int id);

	// Start
	void start();

	// Wait for target to finish
	void join() throws InterruptedException;

	// Stops all agents and helper threads (is called after join)
	void dispose();

	// Get statistics for this target
	IScoreboard getScoreboard();

	// Statistics aggregation identifier
	String getAggregationIdentifier();

	// Returns identifier
	long getId();
}
