package radlab.rain;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class TracksConfigurationCreator 
{
	public abstract JSONObject createProfile( JSONObject params ) throws JSONException;
}

