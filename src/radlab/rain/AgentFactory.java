package radlab.rain;

public interface AgentFactory {

	IAgent createAgent(int id, LoadManager loadManager, Generator generator, Timing timing);
}
