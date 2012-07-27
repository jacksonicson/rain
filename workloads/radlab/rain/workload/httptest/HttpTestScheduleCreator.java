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

package radlab.rain.workload.httptest;

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.LoadProfile;
import radlab.rain.LoadScheduleCreator;

public class HttpTestScheduleCreator extends LoadScheduleCreator 
{
	//@Override
	public LinkedList<LoadProfile> createSchedule( JSONObject params ) throws JSONException  
	{
		LinkedList<LoadProfile> loadSchedule = new LinkedList<LoadProfile>();
		
		// Mix names used here should match what's in the behavior
		//LoadProfile i1 = new LoadProfile( 310, 1, "default", 0, "first" );
		
		LoadProfile i1 = new LoadProfile( 30, 20, "default", 0, "first" );// 10
		LoadProfile i2 = new LoadProfile( 40, 25, "default", 0, "second" );// 15
		/*LoadProfile i2 = new LoadProfile( 15, 40, "default", 0 );
		LoadProfile i3 = new LoadProfile( 45, 100, "default", 0 );
		LoadProfile i4 = new LoadProfile( 40, 150, "default", 0 );
		LoadProfile i5 = new LoadProfile( 60, 200, "default", 0 );
		LoadProfile i6 = new LoadProfile( 40, 150, "default", 0 );
		LoadProfile i7 = new LoadProfile( 45, 100, "default", 0 );
		LoadProfile i8 = new LoadProfile( 35, 40, "default", 0 );*/
		
		loadSchedule.add( i1 );
		loadSchedule.add( i2 );/*
		loadSchedule.add( i3 );
		loadSchedule.add( i4 );
		loadSchedule.add( i5 );
		loadSchedule.add( i6 );
		loadSchedule.add( i7 );
		loadSchedule.add( i8 );*/
		
		return loadSchedule;
	}
}
