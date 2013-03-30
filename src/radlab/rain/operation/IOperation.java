package radlab.rain.operation;

import radlab.rain.load.LoadDefinition;

public interface IOperation extends Runnable {

	void setLoadDefinition(LoadDefinition loadDefinition);

	LoadDefinition getLoadDefinition();

	public int getOperationIndex();
	
	public String getOperationName();

	boolean isAsync();

	void setAsync(boolean async);

	boolean isForceSync();

	void prepare();

	void cleanup();

}
