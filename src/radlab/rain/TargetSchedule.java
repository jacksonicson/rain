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
import de.tum.in.storm.iaas.DomainSize;

public class TargetSchedule {

	// Target queue
	private Queue<TargetConfiguration> targetsToLaunch = new LinkedList<TargetConfiguration>();

	// Global timing
	private long duration;

	public TargetSchedule(JSONObject config) throws JSONException, BenchmarkFailedException {
		loadConfigurations(config);
	}

	private ITargetFactory buildTargetFactory(JSONObject config) throws BenchmarkFailedException {
		try {
			// Factory class
			String className = config.getString("targetFactoryClass");

			// Parameters
			JSONObject factoryConfig = config.getJSONObject("targetFactoryParams");

			// Create class instance
			Class<ITargetFactory> creatorClass = (Class<ITargetFactory>) Class.forName(className);
			Constructor<ITargetFactory> creatorCtor = creatorClass.getConstructor(new Class[] {});
			ITargetFactory creator = (ITargetFactory) creatorCtor.newInstance((Object[]) null);

			// Configure factory
			creator.configure(factoryConfig);

			return creator;
		} catch (Exception e) {
			throw new BenchmarkFailedException("Unable to instantiate track", e);
		}
	}

	private void loadConfigurations(JSONObject config) throws JSONException, BenchmarkFailedException {

		// Read target factory configurations
		JSONObject factoriesConf = config.getJSONObject("targetFactories");
		Map<String, JSONObject> factoryConfigurations = new HashMap<String, JSONObject>();
		Iterator<String> ikeys = factoriesConf.keys();
		while (ikeys.hasNext()) {
			String key = ikeys.next();
			JSONObject factory = factoriesConf.getJSONObject(key);
			factoryConfigurations.put(key, factory);
		}

		// Read target schedule configuration
		JSONArray scheduleConf = config.getJSONArray("targetSchedule");
		for (int i = 0; i < scheduleConf.length(); i++) {
			TargetConfiguration targetConf = new TargetConfiguration();
			targetsToLaunch.add(targetConf);

			// JSON configuration
			JSONObject jsonConf = scheduleConf.getJSONObject(i);

			// Offset
			targetConf.setOffset(jsonConf.getLong("offset") * 1000); // to milliseconds

			// Timing
			targetConf.setRampUp(jsonConf.getLong("rampUp") * 1000); // to milliseconds
			targetConf.setDuration(jsonConf.getLong("duration") * 1000); // to milliseconds
			targetConf.setRampDown(jsonConf.getLong("rampDown") * 1000); // to milliseconds

			// Workload profile
			targetConf.setWorkloadProfileIndex(jsonConf.getInt("workloadProfileIndex")); // workload profile index
			targetConf.setWorkloadProfileName(jsonConf.getString("workloadProfileName")); // workload profile name
			targetConf.setWorkloadProfileOffset(jsonConf.getLong("workloadProfileOffset")); // workload profile offset

			// Set domain size
			String domainSize = jsonConf.getString("domainSize");
			targetConf.setDomainSize(DomainSize.valueOf(domainSize));
			
			// Create factory instance
			JSONObject jsonFactoryConfig = factoryConfigurations.get(jsonConf.getString("targetFactory"));
			ITargetFactory factory = buildTargetFactory(jsonFactoryConfig);
			targetConf.setFactory(factory);

			// Update duration
			long finishTime = targetConf.getOffset() + targetConf.getRampUp() + targetConf.getDuration()
					+ targetConf.getRampDown();
			duration = Math.max(finishTime, duration);
		}
	}

	public long duration() {
		return duration;
	}

	TargetConfiguration next() {
		return targetsToLaunch.poll();
	}

	boolean hasNext() {
		return targetsToLaunch.isEmpty() == false;
	}
}
