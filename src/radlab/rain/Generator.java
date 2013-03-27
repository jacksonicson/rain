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

package radlab.rain;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.scoreboard.IScoreboard;

/**
 * The Generator abstract class provides a default constructor, required properties, and specifies the methods that must
 * be implemented in order to interface with the benchmark architecture.
 * 
 * The basic Generator has a name, associates itself with a scenario track, and keeps a reference to a scoreboard in
 * which operation results are dropped off.
 */
public abstract class Generator {
	// Think time and cycle time
	protected long thinkTime;
	protected long cycleTime;

	// Scoreboard to drop results off at
	protected IScoreboard scoreboard = null;

	// Latest load profile used
	protected LoadUnit latestLoadProfile = null;

	public Generator() {
	}

	public abstract void initialize();

	public abstract Operation nextRequest(int lastOperation);

	public abstract void dispose();

	public void configure(JSONObject config) throws JSONException {
		// Overwrite this
	}

	public void setScoreboard(IScoreboard scoreboard) {
		this.scoreboard = scoreboard;
	}

	public void setMeanCycleTime(long cycleTime) {
		this.cycleTime = cycleTime;
	}

	public void setMeanThinkTime(long thinkTime) {
		this.thinkTime = thinkTime;
	}

	public IScoreboard getScoreboard() {
		return scoreboard;
	}

	public long getCycleTime() {
		return cycleTime;
	}

	public long getThinkTime() {
		return thinkTime;
	}
}
