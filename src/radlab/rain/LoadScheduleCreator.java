package radlab.rain;

import org.json.JSONException;

public interface LoadScheduleCreator {
	LoadSchedule createSchedule() throws JSONException;
}
