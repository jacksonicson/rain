package radlab.rain.workload.http;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.AgentFactory;
import radlab.rain.AgentPOL;
import radlab.rain.Generator;
import radlab.rain.GeneratorFactory;
import radlab.rain.IAgent;
import radlab.rain.ITarget;
import radlab.rain.LoadManager;
import radlab.rain.Target;
import radlab.rain.TargetFactory;
import radlab.rain.Timing;

public class HttpBenchmark implements TargetFactory, GeneratorFactory, AgentFactory {

	private long amount;
	private JSONObject targetConfig;
	private String baseUrl;

	@Override
	public void configure(JSONObject params) throws JSONException {
		amount = params.getInt("amount");
		targetConfig = params.getJSONObject("targetConfig");
		baseUrl = params.getString("baseUrl");
	}

	@Override
	public List<ITarget> createTargets() throws JSONException {
		List<ITarget> tracks = new LinkedList<ITarget>();
		for (int i = 0; i < amount; i++) {
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

		target.setLoadScheduleFactory(new HttpTestScheduleCreator());
		target.setGeneratorFactory(this);
		target.setAgentFactory(this);
		return target;
	}

	@Override
	public Generator createGenerator() {
		HttpTestGenerator generator = new HttpTestGenerator();
		generator.baseUrl = baseUrl;
		return generator;
	}

	@Override
	public IAgent createAgent(int i, LoadManager loadManager, Generator generator, Timing timing) {
		AgentPOL agent = new AgentPOL(i, loadManager, generator, timing);
		agent.setTimeToStart(System.currentTimeMillis());
		return agent;
	}
}
