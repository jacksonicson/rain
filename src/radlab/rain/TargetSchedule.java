package radlab.rain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.target.ITargetFactory;
import de.tum.in.storm.iaas.DomainSize;

public class TargetSchedule {
	private static Logger logger = LoggerFactory.getLogger(TargetSchedule.class);

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

			// Create a new factory instance
			Class<?> classFactory = Class.forName(className);
			ITargetFactory creator = (ITargetFactory) classFactory.newInstance();
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

		// Log scheduler configuration
		logger.info("Scheduler entries: " + targetsToLaunch.size());
	}

	public long duration() {
		return duration;
	}

	TargetConfiguration next() {
		TargetConfiguration next = targetsToLaunch.poll();
		logger.info("Schedule entries left: " + targetsToLaunch.size());
		return next;
	}

	boolean hasNext() {
		return targetsToLaunch.isEmpty() == false;
	}
}
