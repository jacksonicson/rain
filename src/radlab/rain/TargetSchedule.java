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

	class TargetConf {
		long delay;
		String hostname;
		ITargetFactory factory;
	}

	private Queue<TargetConf> configs = new LinkedList<TargetConf>();

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
			TargetConf targetConf = new TargetConf();
			this.configs.add(targetConf);

			JSONObject jsonConf = configs.getJSONObject(i);
			targetConf.delay = jsonConf.getLong("delay");
			targetConf.hostname = jsonConf.getString("hostname");
			
			JSONObject jsonFactoryConfig = factoryConfigurations.get(jsonConf.getString("targetFactory"));
			targetConf.factory = buildTargetFactory(jsonFactoryConfig);
		}
	}

	TargetConf next() {
		return configs.poll();
	}

	boolean hasNext() {
		return configs.isEmpty() == false;
	}
}
