package radlab.rain.target;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.TargetConfiguration.DomainSize;

public interface ITargetFactory {
	// Is called after creating the factory
	public abstract void configure(JSONObject params) throws JSONException;

	// Called to create all targets
	public abstract List<ITarget> createTargets(int loadProfileIndex, String loadProfileName, long loadProfileOffset,
			DomainSize domainSize) throws JSONException;
}
