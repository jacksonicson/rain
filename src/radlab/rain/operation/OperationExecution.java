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

package radlab.rain.operation;

import radlab.rain.load.LoadDefinition;
import radlab.rain.scoreboard.TraceLabels;

/**
 * The OperationExecution class is a wrapper for the results recorded from the execution of an operation. This wrapper
 * can be passed off to an IScoreboard to be recorded and presented at a later time.
 */
public class OperationExecution {

	final public String operationName;
	final public String operationRequest;

	final public LoadDefinition generatedDuring;

	final public boolean async;
	final public boolean failed;

	final public long timeStarted;
	final public long timeFinished;
	final public long profileStartTime;

	final public long actionsPerformed;

	private TraceLabels traceLabel = TraceLabels.NO_TRACE_LABEL;

	/**
	 * Copy constructor
	 */
	public OperationExecution(Operation operation) {
		this.timeStarted = operation.getTimeStarted();
		this.timeFinished = operation.getTimeFinished();
		this.operationName = operation.operationName;
		this.operationRequest = operation.operationRequest;
		this.async = operation.getAsync();
		this.failed = operation.failed;
		this.generatedDuring = operation.getGeneratedDuringProfile();
		this.profileStartTime = operation.getProfileStartTime();
		this.actionsPerformed = operation.getActionsPerformed();
	}

	public TraceLabels getTraceLabel() {
		return this.traceLabel;
	}

	public void setTraceLabel(TraceLabels label) {
		this.traceLabel = label;
	}

	public long getExecutionTime() {
		return timeFinished - timeStarted;
	}
}
