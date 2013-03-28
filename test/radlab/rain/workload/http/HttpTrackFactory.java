package radlab.rain.workload.http;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.DefaultTrack;
import radlab.rain.Generator;
import radlab.rain.GeneratorFactory;
import radlab.rain.ITrack;
import radlab.rain.TrackFactory;

public class HttpTrackFactory implements TrackFactory, GeneratorFactory {

	private long amount;

	@Override
	public void configure(JSONObject params) throws JSONException {
		amount = params.getInt("amount");
	}

	@Override
	public List<ITrack> createTracks() throws JSONException {
		List<ITrack> tracks = new LinkedList<ITrack>();
		for (int i = 0; i < amount; i++) {
			tracks.add(createTrack());
		}

		return tracks;
	}

	protected ITrack createTrack() {
		DefaultTrack track = new DefaultTrack();
		track.setLoadScheduleCreator(new HttpTestScheduleCreator());
		track.setGeneratorFactory(this);
		return track;
	}

	@Override
	public Generator createGenerator() {
		HttpTestGenerator generator = new HttpTestGenerator();
		return generator;
	}
}
