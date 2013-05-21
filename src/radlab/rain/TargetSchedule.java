package radlab.rain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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

	private Object[] buildTargetFactory(JSONObject config) throws BenchmarkFailedException {
		try {
			// Factory class
			String className = config.getString("targetFactoryClass");

			// Parameters
			JSONObject factoryConfig = config.getJSONObject("targetFactoryParams");

			// Create a new classpath as a URL list
			String classPathFile = config.getString("targetClasspathFile");
			String rootFolder = config.getString("rootFolder");
			
			List<URL> urls = new ArrayList<URL>();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(classPathFile));
				String buffer = null;
				while ((buffer = reader.readLine()) != null) {
					if (buffer.startsWith(".")) {
						buffer = buffer.replaceFirst(".", rootFolder);
					}
					File f = new File(buffer);
					URL url = f.toURI().toURL();
					urls.add(url);
					logger.info("URL: " + url);
				}
			} catch (IOException e) {
				logger.error("Could not read classpath of target", e);
			} finally {
				if (reader != null)
					reader.close();
			}

			// Setup new class loader
			ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[] {}), ClassLoader.getSystemClassLoader());
			Class<?> classFactory = classLoader.loadClass(className);
			ITargetFactory creator = (ITargetFactory) classFactory.newInstance();

			// Configure factory
			creator.configure(factoryConfig);

			return new Object[] { creator, classLoader };
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
			Object[] re = buildTargetFactory(jsonFactoryConfig);
			ITargetFactory factory = (ITargetFactory) re[0];
			ClassLoader cl = (ClassLoader) re[1];
			targetConf.setFactory(factory, cl);

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
