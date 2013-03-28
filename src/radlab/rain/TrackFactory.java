package radlab.rain;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public interface TrackFactory {
	public abstract void configure(JSONObject params) throws JSONException;
	
	public abstract List<Track> createTracks() throws JSONException;
}
