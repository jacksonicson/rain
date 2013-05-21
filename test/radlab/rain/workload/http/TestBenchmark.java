package radlab.rain.workload.http;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.TargetConfiguration;
import radlab.rain.agent.AgentPOL;
import radlab.rain.agent.IAgent;
import radlab.rain.agent.IAgentFactory;
import radlab.rain.operation.Generator;
import radlab.rain.operation.IGeneratorFactory;
import radlab.rain.target.ITarget;
import radlab.rain.target.ITargetFactory;
import de.tum.in.storm.iaas.DomainSize;

public class TestBenchmark implements ITargetFactory, IGeneratorFactory, IAgentFactory {

	private long targetCount;
	private JSONObject targetConfig;
	private String baseUrl;

	@Override
	public void configure(JSONObject params) throws JSONException {
		targetCount = params.getInt("targetCount");

		targetConfig = params.getJSONObject("targetConfig");
		baseUrl = targetConfig.getString("baseUrl");
	}

	@Override
	public List<ITarget> createTargets(TargetConfiguration conf) throws JSONException {

		List<ITarget> tracks = new LinkedList<ITarget>();
		for (int i = 0; i < targetCount; i++) {
			tracks.add(createTarget(conf.getDomainSize(), conf.getClassLoader()));
		}
		return tracks;
	}

	protected ITarget createTarget(DomainSize domainSize, ClassLoader cl) {
		TestTarget target = new TestTarget(domainSize, cl);
		try {
			target.loadConfiguration(targetConfig);
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
		TestGenerator generator = new TestGenerator(baseUrl);
		return generator;
	}

	@Override
	public IAgent createAgent(long targetId, long id) {
		AgentPOL agent = new AgentPOL(targetId, id);
		return agent;
	}
}
