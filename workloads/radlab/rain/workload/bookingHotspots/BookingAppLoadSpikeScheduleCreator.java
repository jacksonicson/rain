package radlab.rain.workload.bookingHotspots;

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.LoadDefinition;
import radlab.rain.LoadScheduleCreator;

public class BookingAppLoadSpikeScheduleCreator extends LoadScheduleCreator {

	@Override
	public LinkedList<LoadDefinition> createSchedule(String track, JSONObject params) throws JSONException {
		LinkedList<LoadDefinition> loadSchedule = new LinkedList<LoadDefinition>();

		/*
		 * LoadProfile i1 = new LoadProfile( 30, 400, "default" ); LoadProfile i2 = new LoadProfile( 60, 1000, "default" );
		 * LoadProfile i3 = new LoadProfile( 40, 1200, "default" ); LoadProfile i4 = new LoadProfile( 40, 900, "default" );
		 * LoadProfile i5 = new LoadProfile( 40, 500, "default" ); LoadProfile i6 = new LoadProfile( 40, 200, "default" );
		 * 
		 * loadSchedule.add( i1 ); loadSchedule.add( i2 ); loadSchedule.add( i3 ); loadSchedule.add( i4 ); loadSchedule.add( i5 );
		 * loadSchedule.add( i6 );
		 */

		// 4X increase over 100 seconds, sustained for 10 minutes followed by a return to the original load level over 100 seconds
		LoadDefinition i1 = new LoadDefinition(50, 100, "default", 5);
		LoadDefinition i2 = new LoadDefinition(20, 160, "default", 5);
		LoadDefinition i3 = new LoadDefinition(20, 220, "default", 5);
		LoadDefinition i4 = new LoadDefinition(20, 280, "default", 5);
		LoadDefinition i5 = new LoadDefinition(20, 340, "default", 5);
		LoadDefinition i6 = new LoadDefinition(20, 400, "default", 5);
		LoadDefinition i7 = new LoadDefinition(600, 400, "default", 5);
		LoadDefinition i8 = new LoadDefinition(30, 300, "default", 5);
		LoadDefinition i9 = new LoadDefinition(30, 200, "default", 5);
		LoadDefinition i10 = new LoadDefinition(30, 100, "default", 5);
		LoadDefinition i11 = new LoadDefinition(400, 100, "default", 5);

		loadSchedule.add(i1);
		loadSchedule.add(i2);
		loadSchedule.add(i3);
		loadSchedule.add(i4);
		loadSchedule.add(i5);
		loadSchedule.add(i6);
		loadSchedule.add(i7);
		loadSchedule.add(i8);
		loadSchedule.add(i9);
		loadSchedule.add(i10);
		loadSchedule.add(i11);

		return loadSchedule;
	}

}
