package radlab.rain;

import org.json.JSONObject;

import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.util.MetricWriterFactory.Type;

public interface ITarget {
	public void setTiming(Timing timing);

	public void setMetricWriter(Type metricWriterType, JSONObject metricWriterConf);

	public void init() throws Exception;

	public void start() throws Exception;

	public void end();

	public IScoreboard getScoreboard();

}
