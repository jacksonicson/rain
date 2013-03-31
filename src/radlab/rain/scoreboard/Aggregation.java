package radlab.rain.scoreboard;

import java.util.List;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.Scenario;
import radlab.rain.Timing;
import radlab.rain.target.ITarget;

public class Aggregation {
	private static Logger logger = LoggerFactory.getLogger(Scenario.class);

	public void aggregateScoreboards(Timing timing, List<ITarget> targets) throws JSONException {
		TreeMap<String, Scorecard> aggStats = new TreeMap<String, Scorecard>();
		Scorecard globalCard = new Scorecard(Scorecard.Type.GLOBAL, timing.steadyStateDuration());

		// Aggregate all targets
		for (ITarget target : targets) {
			// Get scoreboard for the target
			IScoreboard scoreboard = target.getScoreboard();

			// Get generator class
			String aggregationIdentifier = target.getAggregationIdentifier();

			// Write detailed statistics to sonar
			JSONObject stats = scoreboard.getStatistics();

			// Log summary of the target
			String strStats = stats.toString();
			logger.info("Target metrics: " + strStats);

			// Get the final scorecard for this track
			Scorecard finalScorecard = scoreboard.getFinalScorecard();
			if (!aggStats.containsKey(aggregationIdentifier)) {
				Scorecard aggCard = new Scorecard(Scorecard.Type.AGGREGATED, finalScorecard.getTimeActive(),
						aggregationIdentifier, 0);
				aggStats.put(aggregationIdentifier, aggCard);
			}
			// Get the current aggregated scorecard for this generator
			Scorecard aggCard = aggStats.get(aggregationIdentifier);
			// Merge the final card for this track with the current per-driver
			// aggregated scorecard
			aggCard.merge(finalScorecard);
			aggStats.put(aggregationIdentifier, aggCard);
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
