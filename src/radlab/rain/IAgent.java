package radlab.rain;

public interface IAgent {

	void run();

	void dispose();

	void doOperation(Operation operation);

	void start();

	// Stop the agent thread
	void interrupt();

	void join() throws InterruptedException;

	String getName();
}
