package radlab.rain;

public interface IAgent {

	void run();

	void dispose();

	void doOperation(Operation operation);

	void setTimeToStart(long timeStarted);

	void start();

	void join() throws InterruptedException;

	String getName();
}
