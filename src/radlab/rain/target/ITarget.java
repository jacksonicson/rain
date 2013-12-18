package radlab.rain.target;

import radlab.rain.Timing;
import radlab.rain.scoreboard.IScoreboard;

public interface ITarget extends Runnable {
	// Timing is set
	void setTiming(Timing timing);

	// Set identifier
	void setId(int id);

	// Start (Thread method)
	void start();

	// Wait for target to finish
	boolean joinTarget(long time) throws InterruptedException;

	// Stops all agents and helper threads (is called after join)
	void dispose();

	// Get statistics for this target
	IScoreboard getScoreboard();

	// Statistics aggregation identifier
	String getAggregationIdentifier();

	// Returns identifier
	long getId();
	
	// Get remain
	long getEnd();
}
