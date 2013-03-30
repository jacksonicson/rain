package radlab.rain.agent;

public interface IAgentFactory {

	IAgent createAgent(long targetId, long id);
}
