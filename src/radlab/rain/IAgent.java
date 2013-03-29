package radlab.rain;

import radlab.rain.scoreboard.IScoreboard;

public interface IAgent {

	void setScoreboard(IScoreboard scoreboard);
	
	void run();

	void dispose();

	void doOperation(Operation operation);

	void start();

	// Stop the agent thread
	void interrupt();

	void join() throws InterruptedException;

	String getName();
}
