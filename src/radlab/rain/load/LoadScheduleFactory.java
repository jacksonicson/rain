package radlab.rain.load;

import org.json.JSONException;

public interface LoadScheduleFactory {
	LoadSchedule createSchedule() throws JSONException;
}
