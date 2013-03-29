package radlab.rain;

public interface AgentFactory {

	IAgent createAgent(int i, LoadManager loadManager, Generator generator, Timing timing);
}
