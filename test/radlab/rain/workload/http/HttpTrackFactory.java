package radlab.rain.workload.http;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.Generator;
import radlab.rain.GeneratorFactory;
import radlab.rain.ITarget;
import radlab.rain.LoadManagedTarget;
import radlab.rain.TargetFactory;

public class HttpTrackFactory implements TargetFactory, GeneratorFactory {

	private long amount;
	private JSONObject targetConfig;

	@Override
	public void configure(JSONObject params) throws JSONException {
		amount = params.getInt("amount");
		targetConfig = params.getJSONObject("trackConfig");
	}

	@Override
	public List<ITarget> createTracks() throws JSONException {
		List<ITarget> tracks = new LinkedList<ITarget>();
		for (int i = 0; i < amount; i++) {
			tracks.add(createTrack());
		}
		return tracks;
	}

	protected ITarget createTrack() {
		LoadManagedTarget track = new LoadManagedTarget();
		try {
			track.configure(targetConfig);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		track.setLoadScheduleFactory(new HttpTestScheduleCreator());
		track.setGeneratorFactory(this);
		return track;
	}

	@Override
	public Generator createGenerator() {
		HttpTestGenerator generator = new HttpTestGenerator();
		return generator;
	}
}
