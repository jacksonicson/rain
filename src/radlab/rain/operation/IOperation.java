package radlab.rain.operation;

import radlab.rain.load.LoadDefinition;

public interface IOperation {

	// Reference to the load definition during which the operation was created
	void setLoadDefinition(LoadDefinition loadDefinition);

	// Return the load definition. This is used to rate limit operation execution.
	LoadDefinition getLoadDefinition();

	// Internal operation index
	public int getOperationIndex();

	// Internal operation name
	public String getOperationName();

	// Execute this operation in asynchronous or synchronous mode
	boolean isAsync();

	// Force synchronous operation
	boolean isForceSync();

	// Update execution sate if it was changed (e.g. by open loop load generation)
	void setAsync(boolean async);

	// Prepare before executing
	void prepare();

	// Cleanup after execution
	void cleanup();
	
	// Run this operation
	public OperationExecution run();
}
