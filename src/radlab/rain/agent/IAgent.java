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

	void run();

	void dispose();

	void doOperation(IOperation operation);

	void start();

	// Stop the agent thread
	void interrupt();

	void join() throws InterruptedException;

	String getName();
}
