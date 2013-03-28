package radlab.rain;

import org.json.JSONException;

public interface LoadScheduleFactory {
	LoadSchedule createSchedule() throws JSONException;
}
