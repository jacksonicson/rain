package radlab.rain.util;

import org.json.JSONObject;

import radlab.rain.scoreboard.ResponseTimeStat;

public abstract class MetricWriter {
	public static String CFG_TYPE_KEY = "type";
	public static String CFG_FILENAME_KEY = "filename";
	public static String CFG_IP_ADDRESS_KEY = "ipaddress";
	public static String CFG_PORT_KEY = "port";

	public MetricWriter(JSONObject config) {
	}

	public abstract String getDetails();

	public abstract boolean write(ResponseTimeStat stat);

	public abstract void close() throws Exception;
}
