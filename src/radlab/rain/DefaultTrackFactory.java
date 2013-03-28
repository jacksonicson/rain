package radlab.rain;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class DefaultTrackFactory implements TrackFactory {

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

		
		
		return track;
	}
}
