package radlab.rain;

public interface IAgent {

	void run();

	void dispose();

	void doOperation(Operation operation);

	void start();

	void join() throws InterruptedException;

	String getName();
}
