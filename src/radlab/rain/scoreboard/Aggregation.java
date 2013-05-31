package radlab.rain.scoreboard;

import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.Scenario;
import radlab.rain.target.ITarget;

public class Aggregation {
	private static Logger logger = Logger.getLogger(Scenario.class);

	private void dumpTarget(ITarget target) throws JSONException {
		// Write detailed statistics to sonar
		IScoreboard scoreboard = target.getScoreboard();
		if (scoreboard == null) {
			logger.error("Scoreboard is NULL");
			return;
		}

		// Get stats
		JSONObject stats = scoreboard.getStatistics();

		// Log summary of the target
		String strStats = stats.toString();
		logger.info("Target scoreboard statistics - " + target.getId() + ": " + strStats);
	}

	public void aggregateScoreboards(List<ITarget> targets, long benchmarkDuration) throws JSONException {
		TreeMap<String, Scorecard> aggStats = new TreeMap<String, Scorecard>();
		Scorecard globalScorecard = new Scorecard(-1, benchmarkDuration);

		// Aggregate all targets
		for (ITarget target : targets) {
			// 0. Dump target statistics to Sonar
			try {
				dumpTarget(target);
			} catch (NullPointerException e) {
				logger.error("Could not dump target: " + target.getId());
				continue; 
			}

			// 1. Merge everything into global scorecard
			IScoreboard scoreboard = target.getScoreboard();
			Scorecard summary = scoreboard.getScorecard();
			globalScorecard.merge(summary);

			// 2. Merge on operation level into global scorecards
			String aggregationIdentifier = target.getAggregationIdentifier();
			if (!aggStats.containsKey(aggregationIdentifier)) {
				Scorecard aggCard = new Scorecard(-1, summary.getTimeActive(), aggregationIdentifier);
				aggStats.put(aggregationIdentifier, aggCard);
			}

			// Get the current aggregated scorecard for this generator
			Scorecard aggCard = aggStats.get(aggregationIdentifier);
			aggCard.merge(summary);
		}

		// Dump global scorecard
		logger.info("Global scorecard: " + globalScorecard.getSummarizedStatistics().toString());

		// Dump merged scorecards on a operational level
		logger.info("# aggregated stats: " + aggStats.size());
		for (String aggregationKey : aggStats.keySet()) {
			Scorecard card = aggStats.get(aggregationKey);

			// Sonar output
			String stats = card.getSummarizedStatistics().toString();
			logger.info("Aggregated scorecard for - " + aggregationKey + ": " + stats);
		}

	}
}
