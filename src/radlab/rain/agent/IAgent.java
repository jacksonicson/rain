package radlab.rain.agent;

import radlab.rain.Timing;
import radlab.rain.load.LoadManager;
import radlab.rain.operation.Generator;
import radlab.rain.scoreboard.IScoreboard;

public interface IAgent {

	void setScoreboard(IScoreboard scoreboard);

	void setTiming(Timing timing);

	void setLoadManager(LoadManager loadManager);

	void setGenerator(Generator generator);

	// Agent name
	String getName();

	// Start agent (usually Thread.start)
	void start();

	// Wait for join
	boolean agentJoin(long wait) throws InterruptedException;

	// Interrupt the agent thread and set the interrupted flag
	public void setInterrupt();

	// Clean up after stopping the agent
	void dispose();
}
