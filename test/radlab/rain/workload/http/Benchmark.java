package radlab.rain.workload.http;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.Timing;
import radlab.rain.agent.AgentFactory;
import radlab.rain.agent.AgentPOL;
import radlab.rain.agent.IAgent;
import radlab.rain.load.LoadManager;
import radlab.rain.operation.Generator;
import radlab.rain.operation.GeneratorFactory;
import radlab.rain.target.ITarget;
import radlab.rain.target.Target;
import radlab.rain.target.ITargetFactory;

public class Benchmark implements ITargetFactory, GeneratorFactory, AgentFactory {

	private long targetCount;
	private JSONObject targetConfig;
	private String baseUrl;

	@Override
	public void configure(JSONObject params) throws JSONException {
		targetCount = params.getInt("targetCount");
		targetConfig = params.getJSONObject("targetConfig");
		baseUrl = params.getString("baseUrl");
	}

	@Override
	public List<ITarget> createTargets() throws JSONException {
		List<ITarget> tracks = new LinkedList<ITarget>();
		for (int i = 0; i < targetCount; i++) {
			tracks.add(createTarget());
		}
		return tracks;
	}

	protected ITarget createTarget() {
		Target target = new Target();
		try {
			target.configure(targetConfig);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		target.setLoadScheduleFactory(new TestScheduleCreator());
		target.setGeneratorFactory(this);
		target.setAgentFactory(this);
		return target;
	}

	@Override
	public Generator createGenerator() {
		TestGenerator generator = new TestGenerator();
		generator.baseUrl = baseUrl;
		return generator;
	}

	@Override
	public IAgent createAgent(int i, LoadManager loadManager, Generator generator, Timing timing) {
		AgentPOL agent = new AgentPOL(i, loadManager, generator, timing);
		return agent;
	}
}
