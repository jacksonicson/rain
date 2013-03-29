package radlab.rain;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public interface TargetFactory {
	// Is called after creating the factory
	public abstract void configure(JSONObject params) throws JSONException;

	// Called to create all targets
	public abstract List<ITarget> createTargets() throws JSONException;
}
