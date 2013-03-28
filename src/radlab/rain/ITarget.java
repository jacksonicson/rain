package radlab.rain;

import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.util.MetricWriter;

public interface ITarget {
	public void setTiming(Timing timing);

	public void setMetricWriter(MetricWriter metricWriter);

	public void init() throws Exception;

	public void start() throws Exception;

	public void end();

	public IScoreboard getScoreboard();

}
