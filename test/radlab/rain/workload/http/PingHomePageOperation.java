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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.scoreboard.IScoreboard;

public class PingHomePageOperation extends TestOperation {
	private static final Logger logger = LoggerFactory.getLogger(PingHomePageOperation.class);
	public static String NAME = "PingHome";

	public PingHomePageOperation(IScoreboard scoreboard) {
		super(scoreboard);

		this.operationName = NAME;
		this.operationIndex = TestGenerator.PING_HOMEPAGE;
		this.setAsync(true);
	}

	@Override
	public void execute() throws Throwable {
		logger.debug("Executing ping homepage operation");

		// Fetch the base url
		try {
			StringBuilder response = this.http.fetchUrl("http://" + ((TestGenerator) generator).baseUrl);
			trace();

			if (response.length() == 0) {
				String errorMessage = "Home page GET ERROR - Received an empty response";
				throw new IOException(errorMessage);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Once we get here mark the operation as successful
		this.setFailed(false);
	}
}
