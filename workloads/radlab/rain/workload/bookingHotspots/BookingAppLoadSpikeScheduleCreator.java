package radlab.rain.workload.bookingHotspots;

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.LoadUnit;
import radlab.rain.LoadScheduleCreator;

public class BookingAppLoadSpikeScheduleCreator extends LoadScheduleCreator {

	@Override
	public LinkedList<LoadUnit> createSchedule(String track, JSONObject params) throws JSONException {
		LinkedList<LoadUnit> loadSchedule = new LinkedList<LoadUnit>();

		/*
		 * LoadProfile i1 = new LoadProfile( 30, 400, "default" ); LoadProfile i2 = new LoadProfile( 60, 1000, "default" );
		 * LoadProfile i3 = new LoadProfile( 40, 1200, "default" ); LoadProfile i4 = new LoadProfile( 40, 900, "default" );
		 * LoadProfile i5 = new LoadProfile( 40, 500, "default" ); LoadProfile i6 = new LoadProfile( 40, 200, "default" );
		 * 
		 * loadSchedule.add( i1 ); loadSchedule.add( i2 ); loadSchedule.add( i3 ); loadSchedule.add( i4 ); loadSchedule.add( i5 );
		 * loadSchedule.add( i6 );
		 */

		// 4X increase over 100 seconds, sustained for 10 minutes followed by a return to the original load level over 100 seconds
		LoadUnit i1 = new LoadUnit(50, 100, "default", 5);
		LoadUnit i2 = new LoadUnit(20, 160, "default", 5);
		LoadUnit i3 = new LoadUnit(20, 220, "default", 5);
		LoadUnit i4 = new LoadUnit(20, 280, "default", 5);
		LoadUnit i5 = new LoadUnit(20, 340, "default", 5);
		LoadUnit i6 = new LoadUnit(20, 400, "default", 5);
		LoadUnit i7 = new LoadUnit(600, 400, "default", 5);
		LoadUnit i8 = new LoadUnit(30, 300, "default", 5);
		LoadUnit i9 = new LoadUnit(30, 200, "default", 5);
		LoadUnit i10 = new LoadUnit(30, 100, "default", 5);
		LoadUnit i11 = new LoadUnit(400, 100, "default", 5);

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
