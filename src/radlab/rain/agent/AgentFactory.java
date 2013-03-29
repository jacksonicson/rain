package radlab.rain.agent;

import radlab.rain.Timing;
import radlab.rain.load.LoadManager;
import radlab.rain.operation.Generator;

public interface AgentFactory {

	IAgent createAgent(int id, LoadManager loadManager, Generator generator, Timing timing);
}
