package radlab.rain;

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

//import java.util.LinkedList;

public abstract class LoadScheduleCreator implements ILoadScheduleCreator {
	public abstract LinkedList<LoadProfile> createSchedule(String track, JSONObject params) throws JSONException;
}
