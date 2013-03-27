package radlab.rain;

import org.json.JSONException;
import org.json.JSONObject;

public interface LoadScheduleCreator {
	LoadSchedule createSchedule(JSONObject params) throws JSONException;
}
