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

package radlab.rain.workload.http;

import org.json.JSONException;

import radlab.rain.Generator;
import radlab.rain.Operation;
import radlab.rain.TrackConfiguration;
import radlab.rain.util.HttpTransport;

public class HttpTestGenerator extends Generator {

	// Operation indices used in the mix matrix.
	public static final int PING_HOMEPAGE = 0;

	// private java.util.Random _randomNumberGenerator;
	private HttpTransport _http;
	String _baseUrl;

	public void initialize() {
		this._http = new HttpTransport();
	}

	@Override
	public void configure(TrackConfiguration trackConfig) throws JSONException {
		this._baseUrl = "http://" + trackConfig.targetHostname + ":" + trackConfig.targetPort;
	}

	/**
	 * Returns the next <code>Operation</code> given the <code>lastOperation</code> according to the current mix matrix.
	 * 
	 * @param lastOperation
	 *            The last <code>Operation</code> that was executed.
	 */
	public Operation nextRequest(int lastOperation) {
		int nextOperation = -1;

		if (lastOperation == -1) {
			nextOperation = 0;
		} else {

			// For now do the same operation over and over again
			nextOperation = PING_HOMEPAGE;
		}

		return getOperation(nextOperation);
	}

	/**
	 * Returns the current think time. The think time is duration between receiving the response of an operation and the
	 * execution of its succeeding operation during synchronous execution (i.e. closed loop).
	 */
	public long getThinkTime() {
		return 0;
	}

	/**
	 * Returns the current cycle time. The cycle time is duration between the execution of an operation and the
	 * execution of its succeeding operation during asynchronous execution (i.e. open loop).
	 */
	public long getCycleTime() {
		return 0;
	}

	public void dispose() {
	}

	public HttpTransport getHttpTransport() {
		return this._http;
	}

	public Operation getOperation(int opIndex) {
		switch (opIndex) {
		case PING_HOMEPAGE:
			return new PingHomePageOperation(scoreboard);
		default:
			return null;
		}
	}
}
