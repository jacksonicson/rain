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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DefaultScenarioTrack class is a generic implementation of the abstract <code>ScenarioTrack</code> class that
 * supports load profiles that specify the interval, number of users, mix behavior, and any transitions.
 */
public class DefaultTrack extends Track {
	private Logger logger = LoggerFactory.getLogger(DefaultTrack.class.getName() + " " + this);

	// Load manager runs through the load schedule
	private LoadManager loadManager;

	public DefaultTrack(ScenarioConfiguration scenarioConfig) throws Exception {
		super(scenarioConfig);
		
		// Create a new load manager
		loadManager = new LoadManager(scenarioConfiguration.getRampUp());
	}

	public boolean validateLoadProfile(LoadDefinition profile) {
		// Check number of users
		if (profile.numberOfUsers <= 0) {
			logger.info("Invalid load profile. Number of users <= 0. Profile details: " + profile.toString());
			return false;
		}

		// Check references to the mix matrix
		if (profile.mixName.length() > 0 && !config.mixMatrices.containsKey(profile.mixName)) {
			logger.info("Invalid load profile. mixname not in track's mixmap. Profile details: " + profile.toString());
			return false;
		}

		return true;
	}

	public void start() {
		if (loadManager.isAlive())
			return;

		logger.debug("Starting load manager");
		loadManager.start();
		
		super.start(); 
	}

	public void end() {
		super.end(); 
		if (!loadManager.isAlive())
			return;

		try {
			logger.debug("Stopping load manager");
			loadManager.setDone(true);
			loadManager.interrupt();
			loadManager.join();
		} catch (InterruptedException ie) {
		}
	}
}
