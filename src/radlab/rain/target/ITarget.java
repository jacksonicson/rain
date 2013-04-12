package radlab.rain.target;

import radlab.rain.Timing;
import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.util.MetricWriter;

public interface ITarget extends Runnable {
	// Timing is set
	void setTiming(Timing timing);

	// Metric writer is set
	void setMetricWriter(MetricWriter metricWriter);

	// Start
	void start();

	// Wait for target to finish
	void join() throws InterruptedException;

	// Stops all agents and helper threads
	void dispose();

	// Statistics for this target
	IScoreboard getScoreboard();

	// Set identifier
	void setId(int id); 
	
	// Returns identifier
	long getId();

	// Statistics aggregation identifier
	String getAggregationIdentifier();
}
