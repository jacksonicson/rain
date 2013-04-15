package radlab.rain;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.target.ITargetFactory;

public class TargetSchedule {

	private Queue<TargetConfiguration> configs = new LinkedList<TargetConfiguration>();

	public TargetSchedule(JSONObject scheduleConf, JSONObject factoryConf) throws JSONException,
			BenchmarkFailedException {
		configure(scheduleConf, factoryConf);
	}

	private ITargetFactory buildTargetFactory(JSONObject config) throws BenchmarkFailedException {
		try {
			String className = config.getString("targetFactoryClass");
			JSONObject factoryConfig = config.getJSONObject("targetFactoryParams");
			Class<ITargetFactory> creatorClass = (Class<ITargetFactory>) Class.forName(className);
			Constructor<ITargetFactory> creatorCtor = creatorClass.getConstructor(new Class[] {});
			ITargetFactory creator = (ITargetFactory) creatorCtor.newInstance((Object[]) null);
			creator.configure(factoryConfig);
			return creator;
		} catch (Exception e) {
			throw new BenchmarkFailedException("Unable to instantiate track", e);
		}
	}

	private void configure(JSONObject scheduleConf, JSONObject factoryConf) throws JSONException,
			BenchmarkFailedException {
		// Read target factory configuration
		Map<String, JSONObject> factoryConfigurations = new HashMap<String, JSONObject>();
		Iterator<String> ikeys = factoryConf.keys();
		while (ikeys.hasNext()) {
			String key = ikeys.next();
			JSONObject factory = factoryConf.getJSONObject(key);
			factoryConfigurations.put(key, factory);
		}

		// Read target schedule configuration
		JSONArray configs = scheduleConf.getJSONArray("sequence");
		for (int i = 0; i < configs.length(); i++) {
			TargetConfiguration targetConf = new TargetConfiguration();
			this.configs.add(targetConf);

			JSONObject jsonConf = configs.getJSONObject(i);
			targetConf.setDelay(jsonConf.getLong("delay") * 1000); // to milliseconds
			targetConf.setHostname(jsonConf.getString("hostname"));
			targetConf.setRampUp(jsonConf.getLong("rampUp") * 1000); // to milliseconds
			targetConf.setDuration(jsonConf.getLong("duration") * 1000); // to milliseconds
			targetConf.setRampDown(jsonConf.getLong("rampDown") * 1000); // to milliseconds

			JSONObject jsonFactoryConfig = factoryConfigurations.get(jsonConf.getString("targetFactory"));
			targetConf.setFactory(buildTargetFactory(jsonFactoryConfig));
		}
	}

	TargetConfiguration next() {
		return configs.poll();
	}

	boolean hasNext() {
		return configs.isEmpty() == false;
	}
}
