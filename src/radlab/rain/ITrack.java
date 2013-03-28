package radlab.rain;

import org.json.JSONObject;

import radlab.rain.scoreboard.IScoreboard;
import radlab.rain.util.MetricWriterFactory.Type;

public interface ITrack {
	public void setTiming(Timing timing);

	public void setMetricWriterType(Type metricWriterType);

	public void setMetricWriterConf(JSONObject metricWriterConf);

	public void init() throws Exception;

	public void start() throws Exception;

	public void end();

	public IScoreboard getScoreboard();

}
