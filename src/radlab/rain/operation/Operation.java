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
import radlab.rain.scoreboard.IScoreboard;

/**
 * The Operation class is a encapsulation of "tasks to be done". An operation contains all features of the state
 * necessary to execute at an arbitrary point in time (i.e. immediately or in the future). It follows the Command
 * pattern (GoF). It implements <code>Runnable</code> in case another thread has to execute it.
 */
public abstract class Operation implements IOperation {

	// Describes the operation
	protected int operationIndex = -1;
	protected String operationName;
	protected String operationRequest;

	// Load definition in effect when this operation was generated
	private LoadDefinition loadDefinition;
	private long loadDefinitionStartTime;

	// Reference to the generator that created this operation
	protected Generator generator;

	// Used to collect execution metrics
	protected IScoreboard scoreboard;

	// Statistics
	private long timeStarted;
	private long timeFinished;

	// Outcome of executing the operation
	protected boolean failed = true;
	protected Throwable failure;

	// Counts number of actions (like http requests)
	protected long numberOfActionsPerformed;

	/**
	 * Constructor
	 */
	public Operation(IScoreboard scoreboard) {
		this.scoreboard = scoreboard;
	}

	public final void run() {
		// Invoke the pre-execute hook here before we start the clock to time the operation's execution
		preExecute();
		timeStarted = System.currentTimeMillis();
		try {
			execute();
		} catch (Throwable e) {
			failed = true;
			failure = e;
		} finally {
			timeFinished = System.currentTimeMillis();

			// Invoke the post-execute hook here after we stop the clock to time the
			// operation's execution
			postExecute();

			// Dump operation results into the scoreboard
			OperationExecution result = new OperationExecution(this);
			scoreboard.dropOffOperation(result);

			// Run cleanup
			cleanup();
		}
	}

	protected abstract void execute() throws Throwable;

	public void prepare() {
	}

	protected void preExecute() {
	}

	protected void postExecute() {
	}

	public void cleanup() {

	}

	protected void trace() {
		numberOfActionsPerformed++;
	}

	protected void trace(String msg) {
		numberOfActionsPerformed++;
	}

	/**
	 * Getter & Setters
	 */
	public void setGeneratedByGenerator(Generator generator) {
		this.generator = generator;
	}

	public void setLoadDefinition(LoadDefinition loadDefinition) {
		this.loadDefinition = loadDefinition;
		this.loadDefinitionStartTime = loadDefinition.getTimeStarted();
	}

	public long getTimeStarted() {
		return timeStarted;
	}

	public long getTimeFinished() {
		return timeFinished;
	}

	public String getOperationName() {
		return operationName;
	}

	public String getOperationRequest() {
		return operationRequest;
	}

	public abstract boolean isAsync();

	public boolean isForceSync() {
		return false;
	}

	public boolean isFailed() {
		return failed;
	}

	public LoadDefinition getLoadDefinition() {
		return loadDefinition;
	}

	public long getLoadDefinitionStartTime() {
		return loadDefinitionStartTime;
	}

	public long getNumberOfActionsPerformed() {
		return numberOfActionsPerformed;
	}

	public int getOperationIndex() {
		return operationIndex;
	}
}
