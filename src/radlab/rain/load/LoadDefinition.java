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

public class LoadDefinition {

	private final long interval;
	private final long transitionTime;
	private final long numberOfUsers;

	private final String mixName;
	private final int openLoopMaxOpsPerSec;

	private long activationCount;
	private long timeStarted = -1;

	public LoadDefinition(long interval, int numberOfUsers, String mixName) {
		this(interval, numberOfUsers, mixName, 0);
	}

	public LoadDefinition(long interval, int numberOfUsers, String mixName, long transitionTime) {
		this.interval = interval;
		this.transitionTime = transitionTime;
		this.numberOfUsers = numberOfUsers;
		this.mixName = mixName;
		this.openLoopMaxOpsPerSec = 0;
	}

	public LoadDefinition(long interval, int numberOfUsers, String mixName, long transitionTime, String name) {
		this.interval = interval;
		this.numberOfUsers = numberOfUsers;
		this.mixName = mixName;
		this.transitionTime = transitionTime;
		this.openLoopMaxOpsPerSec = 0;
	}

	public long getInterval() {
		return interval;
	}

	public long getNumberOfUsers() {
		return numberOfUsers;
	}

	public String getMixName() {
		return this.mixName;
	}

	public long getTransitionTime() {
		return transitionTime;
	}

	public long getTimeStarted() {
		return timeStarted;
	}

	public int getOpenLoopMaxOpsPerSec() {
		return openLoopMaxOpsPerSec;
	}

	public void activate() {
		activationCount++;
		timeStarted = System.currentTimeMillis();
	}

	public long getActivations() {
		return activationCount;
	}
}
