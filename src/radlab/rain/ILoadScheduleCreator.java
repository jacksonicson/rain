package radlab.rain;

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

public interface ILoadScheduleCreator {
	LinkedList<LoadUnit> createSchedule(String track, JSONObject params) throws JSONException;
}
