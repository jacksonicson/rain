package radlab.rain.scoreboard;

import java.util.List;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.Scenario;
import radlab.rain.Timing;

public class Aggregation {
	private static Logger logger = LoggerFactory.getLogger(Scenario.class);

	public void aggregateScoreboards(Timing timing, List<IScoreboard> scoreboards) throws JSONException {
		TreeMap<String, Scorecard> aggStats = new TreeMap<String, Scorecard>();
		Scorecard globalCard = new Scorecard("global", "global", timing.steadyStateDuration());

		// Shutdown the scoreboards and tally up the results.
		for (IScoreboard scoreboard : scoreboards) {
			// Write detailed statistics to sonar
			JSONObject stats = scoreboard.getStatistics();
			String strStats = stats.toString();
			logger.info("Target metrics: " + strStats);

			// Get the name of the generator active for this track
			String generatorClassName = "TODO";

			// Get the final scorecard for this track
			Scorecard finalScorecard = scoreboard.getFinalScorecard();
			if (!aggStats.containsKey(generatorClassName)) {
				Scorecard aggCard = new Scorecard("aggregated", generatorClassName, finalScorecard
						.getIntervalDuration());
				aggStats.put(generatorClassName, aggCard);
			}
			// Get the current aggregated scorecard for this generator
			Scorecard aggCard = aggStats.get(generatorClassName);
			// Merge the final card for this track with the current per-driver
			// aggregated scorecard
			aggCard.merge(finalScorecard);
			aggStats.put(generatorClassName, aggCard);
			// Collect scoreboard results
			// Collect object pool results

			// Merge global card
			globalCard.merge(finalScorecard);
		}

		// Aggregate stats
		if (aggStats.size() > 0)
			logger.info("# aggregated stats: " + aggStats.size());

		for (String generatorName : aggStats.keySet()) {
			Scorecard card = aggStats.get(generatorName);

			// Sonar output
			JSONObject stats = card.getIntervalStatistics();
			String strStats = stats.toString();
			logger.info("Rain metrics: " + strStats);
		}

		// Dump global card
		logger.info("Global metrics: " + globalCard.getIntervalStatistics().toString());
	}

}
