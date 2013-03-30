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

package radlab.rain.load;

import org.json.JSONException;
import org.json.JSONObject;

public class LoadDefinition {
	public static String CFG_LOAD_PROFILE_INTERVAL_KEY = "interval";
	public static String CFG_LOAD_PROFILE_TRANSITION_TIME_KEY = "transitionTime";
	public static String CFG_LOAD_PROFILE_USERS_KEY = "users";
	public static String CFG_LOAD_PROFILE_MIX_KEY = "mix";
	public static String CFG_LOAD_PROFILE_NAME_KEY = "name";
	public static String CFG_OPEN_LOOP_MAX_OPS_PER_SEC_KEY = "openLoopMaxOpsPerSec";
	// Allow LoadProfile intervals to have names (no getter/setter)
	public String _name = "";

	public long interval;
	protected long transitionTime;
	public int numberOfUsers;
	protected String mixName = "";
	private long activeCount = 0; // How often has this interval become active, the load scheduler updates this
	public int openLoopMaxOpsPerSec = 0; // Rate limit on async operations. A value of 0 means no rate limiting.
	protected JSONObject config = null; // Save the original configuration object if its passed

	private long _timeStarted = -1; // LoadManagerThreads need to update this every time they advance the "clock"

	public LoadDefinition(JSONObject profileObj) throws JSONException {
		this.interval = profileObj.getLong(CFG_LOAD_PROFILE_INTERVAL_KEY);
		this.numberOfUsers = profileObj.getInt(CFG_LOAD_PROFILE_USERS_KEY);
		this.mixName = profileObj.getString(CFG_LOAD_PROFILE_MIX_KEY);

		// Load the transition time (if specified)
		if (profileObj.has(CFG_LOAD_PROFILE_TRANSITION_TIME_KEY))
			this.transitionTime = profileObj.getLong(CFG_LOAD_PROFILE_TRANSITION_TIME_KEY);

		// Load the interval name (if specified)
		if (profileObj.has(CFG_LOAD_PROFILE_NAME_KEY))
			this._name = profileObj.getString(CFG_LOAD_PROFILE_NAME_KEY);

		// Open loop rate limiting (if that's configured). By default there's no rate limiting
		if (profileObj.has(CFG_OPEN_LOOP_MAX_OPS_PER_SEC_KEY)) {
			this.openLoopMaxOpsPerSec = profileObj.getInt(CFG_OPEN_LOOP_MAX_OPS_PER_SEC_KEY);
			if (this.openLoopMaxOpsPerSec < 0)
				this.openLoopMaxOpsPerSec = 0;
		}

		this.config = profileObj;
	}

	public LoadDefinition(long interval, int numberOfUsers, String mixName) {
		this(interval, numberOfUsers, mixName, 0);
	}

	public LoadDefinition(long interval, int numberOfUsers, String mixName, long transitionTime) {
		this.interval = interval;
		this.numberOfUsers = numberOfUsers;
		this.mixName = mixName;
		this.transitionTime = transitionTime;
	}

	public LoadDefinition(long interval, int numberOfUsers, String mixName, long transitionTime, String name) {
		this.interval = interval;
		this.numberOfUsers = numberOfUsers;
		this.mixName = mixName;
		this.transitionTime = transitionTime;
		this._name = name;
	}

	public long getInterval() {
		return interval * 1000;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public int getNumberOfUsers() {
		return this.numberOfUsers;
	}

	public void setNumberOfUsers(int val) {
		this.numberOfUsers = val;
	}

	public String getMixName() {
		return this.mixName;
	}

	public void setMixName(String val) {
		this.mixName = val;
	}

	public long getTransitionTime() {
		return (this.transitionTime * 1000);
	}

	public void setTransitionTime(long val) {
		this.transitionTime = val;
	}

	public long getTimeStarted() {
		return this._timeStarted;
	}

	public void setTimeStarted(long val) {
		this._timeStarted = val;
	}

	public JSONObject getConfig() {
		return this.config;
	}

	public void setConfig(JSONObject val) {
		this.config = val;
	}

	public int getOpenLoopMaxOpsPerSec() {
		return this.openLoopMaxOpsPerSec;
	}

	public void setOpenLoopMaxOpsPerSec(int val) {
		this.openLoopMaxOpsPerSec = val;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		if (this._name == null || this._name.trim().length() == 0)
			buf.append("[Duration: " + this.interval + " Users: " + this.numberOfUsers + " Mix: " + this.mixName
					+ " Transition time: " + this.transitionTime + "]");
		else
			buf.append("[Duration: " + this.interval + " Users: " + this.numberOfUsers + " Mix: " + this.mixName
					+ " Transition time: " + this.transitionTime + " Name: " + this._name + "]");
		return buf.toString();
	}

	public void activate() {
		activeCount++;
		setTimeStarted(System.currentTimeMillis());
	}

	public long getActivations() {
		return activeCount;
	}
}
