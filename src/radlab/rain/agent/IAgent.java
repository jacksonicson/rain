package radlab.rain.agent;

import radlab.rain.Timing;
import radlab.rain.load.LoadManager;
import radlab.rain.operation.Generator;
import radlab.rain.operation.IOperation;
import radlab.rain.scoreboard.IScoreboard;

public interface IAgent {

	void setScoreboard(IScoreboard scoreboard);

	void setTiming(Timing timing);

	void setLoadManager(LoadManager loadManager);

	void setGenerator(Generator generator);

	// Start agent
	void start();

	// Wait for the agent to join
	void join() throws InterruptedException;
	
	// Stop the agent thread
	void interrupt();
	
	// Clean up after stopping the agent
	void dispose();
	
	// Agent name
	String getName();
}
