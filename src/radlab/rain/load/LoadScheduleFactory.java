package radlab.rain.load;

import org.json.JSONException;

import radlab.rain.Timing;

public interface LoadScheduleFactory {
	LoadSchedule createSchedule(Timing timing) throws JSONException;
}
