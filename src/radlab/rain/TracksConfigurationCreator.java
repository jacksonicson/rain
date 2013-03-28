package radlab.rain;

import org.json.JSONException;
import org.json.JSONObject;

public interface TracksConfigurationCreator {
	public abstract TrackConfiguration createConfiguration(JSONObject params) throws JSONException;

	public abstract void configure(JSONObject params) throws JSONException;
}
