/*
 * Copyright (c) 2010, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  * Neither the name of the University of California, Berkeley
 * nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package radlab.rain.workload.daytrader;

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.LoadUnit;
import radlab.rain.LoadScheduleCreator;

public class DayTraderScheduleCreator extends LoadScheduleCreator 
{
	@Override
	public LinkedList<LoadUnit> createSchedule(String track, JSONObject params ) throws JSONException 
	{
		LinkedList<LoadUnit> loadSchedule = new LinkedList<LoadUnit>();
		
		// Mix names used here should match what's in the behavior
		LoadUnit i1 = new LoadUnit( 30, 20, "default", 0 );
		LoadUnit i2 = new LoadUnit( 15, 40, "default", 0 );
		LoadUnit i3 = new LoadUnit( 45, 100, "default", 0 );
		LoadUnit i4 = new LoadUnit( 40, 150, "default", 0 );
		LoadUnit i5 = new LoadUnit( 60, 200, "default", 0 );
		LoadUnit i6 = new LoadUnit( 40, 150, "default", 0 );
		LoadUnit i7 = new LoadUnit( 45, 100, "default", 0 );
		LoadUnit i8 = new LoadUnit( 35, 40, "default", 0 );
		
		loadSchedule.add( i1 );
		loadSchedule.add( i2 );
		loadSchedule.add( i3 );
		loadSchedule.add( i4 );
		loadSchedule.add( i5 );
		loadSchedule.add( i6 );
		loadSchedule.add( i7 );
		loadSchedule.add( i8 );
		
		return loadSchedule;
	}
}
