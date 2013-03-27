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

import java.util.Set;

import radlab.rain.scoreboard.IScoreboard;

/**
 * The Operation class is a encapsulation of "tasks to be done". An operation contains all features of the state
 * necessary to execute at an arbitrary point in time (i.e. immediately or in the future). It follows the Command
 * pattern (GoF). It implements <code>Runnable</code> in case another thread has to execute it.
 */
public abstract class Operation implements Runnable {

	// Describes the operation
	protected int operationIndex = -1;
	protected String operationName;
	protected String operationRequest;

	// Describes who generated the operation and when (during what interval)
	protected String generatedBy;

	// Load definition in effect when this operation was generated/initialized
	private LoadDefinition generatedDuringLoadDefinition;
	protected long profileStartTime = -1;

	// Reference to the generator that created this operation
	protected Generator generatedByGenerator;

	// Used to collect execution metrics
	protected IScoreboard scoreboard;

	// Identifies the generator within the track
	private long generatorId = -1;

	// Statistics
	private long timeQueued;
	private long timeStarted;
	private long timeFinished;

	private long thinkTimeUsed; // Track how much thinktime we used
	private long cycleTimeUsed; // Track how much cycle delays we took advantage of

	// Synchron or asynchron execution mode
	private boolean async = false;
	protected boolean enforceSync = false; // if order of operation execution is important

	// Outcome of executing the operation
	protected boolean failed = true;
	protected Throwable failureReason;
	protected TraceRecord trace;

	protected long numberOfActionsPerformed;

	public Operation(IScoreboard scoreboard) {
		this.scoreboard = scoreboard;
	}

	public int getOperationIndex() {
		return this.operationIndex;
	}

	public String getOperationName() {
		return this.operationName;
	}

	public long getTimeQueued() {
		return this.timeQueued;
	}

	public void setTimeQueued(long val) {
		this.timeQueued = val;
	}

	public long getTimeStarted() {
		return this.timeStarted;
	}

	public void setTimeStarted(long val) {
		this.timeStarted = val;
	}

	public long getTimeFinished() {
		return this.timeFinished;
	}

	public void setTimeFinished(long val) {
		this.timeFinished = val;
	}

	public long getThinkTimeUsed() {
		return this.thinkTimeUsed;
	}

	public void setThinkTimeUsed(long val) {
		this.thinkTimeUsed = val;
	}

	public long getCycleTimeUsed() {
		return this.cycleTimeUsed;
	}

	public void setCycleTimeUsed(long val) {
		this.cycleTimeUsed = val;
	}

	public boolean getAsync() {
		return this.async;
	}

	public void setAsync(boolean val) {
		this.async = val;
	}

	public String getGeneratedBy() {
		return this.generatedBy;
	}

	public void setGeneratedBy(String val) {
		this.generatedBy = val;
	}

	public LoadDefinition getGeneratedDuringProfile() {
		return this.generatedDuringLoadDefinition;
	}

	public void setGeneratedDuringProfile(LoadDefinition val) {
		// Save the load profile
		this.generatedDuringLoadDefinition = val;
		// Save the time started now since the load manager thread updates this
		// field - we can then use timestarted+intervalduration
		// to see whether the operation finished during the interval
		this.profileStartTime = val.getTimeStarted();
	}

	public long getProfileStartTime() {
		return this.profileStartTime;
	}

	public boolean isFailed() {
		return this.failed;
	}

	public void setFailed(boolean val) {
		this.failed = val;
	}

	public Throwable getFailureReason() {
		return this.failureReason;
	}

	public void setFailureReason(Throwable t) {
		this.failureReason = t;
	}

	public long getActionsPerformed() {
		return numberOfActionsPerformed;
	}

	public void setActionsPerformed(long val) {
		this.numberOfActionsPerformed = val;
	}

	public long getGeneratorThreadID() {
		return this.generatorId;
	}

	public void setGeneratorThreadID(long val) {
		this.generatorId = val;
	}

	public void trace(String request) {
		if (this.trace == null)
			this.trace = new TraceRecord();
		this.trace._lstRequests.add(request);
	}

	public void trace(String[] requests) {
		if (this.trace == null)
			this.trace = new TraceRecord();

		for (String request : requests)
			this.trace._lstRequests.add(request);
	}

	public void trace(Set<String> requests) {
		if (this.trace == null)
			this.trace = new TraceRecord();

		for (String request : requests)
			this.trace._lstRequests.add(request);
	}

	public TraceRecord getTrace() {
		return this.trace;
	}

	public StringBuffer dumpTrace() {
		StringBuffer buf = new StringBuffer();
		TraceRecord traceRec = this.trace;
		if (traceRec == null)
			return buf;

		// |-----Workload details----------------|----------|-------|--------|--------------|
		// [RU] [Workload Interval#?] [Max users] Start time, opName, action#, actual request
		int i = 0;
		for (String request : traceRec._lstRequests) {
			buf.append(this.timeStarted);
			buf.append(" ");
			buf.append(this.operationName);
			buf.append(" ");
			buf.append(i);
			buf.append(" ");
			buf.append(request);
			buf.append("\n");
			i++;
		}

		return buf;
	}

	public void disposeOfTrace() {
		if (this.trace == null)
			return;

		this.trace._lstRequests.clear();
		this.trace = null;
	}

	/**
	 * This method is used to run this operation. By default, it records any metrics when executing. This can be
	 * overridden to make a single call to <code>execute()</code> for more fine-grained control. This method must catch
	 * any <code>Throwable</code>s.
	 */
	public void run() {
		// Invoke the pre-execute hook here before we start the clock to time the
		// operation's execution
		this.preExecute();

		this.setTimeStarted(System.currentTimeMillis());
		try {
			this.execute();
		} catch (Throwable e) {
			this.setFailed(true);
			this.setFailureReason(e);
		} finally {
			this.setTimeFinished(System.currentTimeMillis());
			// Invoke the post-execute hook here after we stop the clock to time the
			// operation's execution
			this.postExecute();

			if (this.scoreboard != null) {
				OperationExecution result = new OperationExecution(this);
				this.scoreboard.dropOffOperation(result);
			}
		}
	}

	/**
	 * Prepares this operation for execution. This involves copying any features about the current state into this
	 * operation.
	 * 
	 * @param generator
	 *            The generator containing the state to copy.
	 */
	public abstract void prepare(Generator generator);

	/**
	 * Executes this operation. This method is responsible for saving its trace record and execution metrics.
	 * 
	 * @throws Throwable
	 */
	public abstract void execute() throws Throwable;

	/**
	 * Hook method for actions to be performed right before execution starts (before the clock starts to time the
	 * execute method). There's no throws clause on this method so if something fails the methods need to deal with it.
	 */
	public void preExecute() {
	}

	/**
	 * Hook method for actions to be performed right after execution finishes (after the clock stops to time the execute
	 * method). There's no throws clause on this method so if something fails the methods need to deal with it.
	 */
	public void postExecute() {
	}

	/**
	 * Do any potential cleanup necessary after execution of this operation.
	 */
	public abstract void cleanup();

}
