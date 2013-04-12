package radlab.rain.target;

import radlab.rain.Timing;
import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.util.MetricWriter;

public interface ITarget {
	// Timing is set
	void setTiming(Timing timing);

	// Metric writer is set
	void setMetricWriter(MetricWriter metricWriter);

	// Initialize is called after setting all references
	void init(long id) throws Exception;

	// Setup process like starting VMs
	void setup(); 
	
	// Start is called after initialization
	void start() throws Exception;
	
	// Blocks until all agent threads have joined
	void joinAgents() throws InterruptedException;

	// Stops all agents and helper threads
	void dispose();

	// Statistics for this target
	IScoreboard getScoreboard();

	// Returns identifier
	long getId();

	// Statistics aggregation identifier
	String getAggregationIdentifier();
}
