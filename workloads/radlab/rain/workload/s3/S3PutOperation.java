package radlab.rain.workload.s3;

import radlab.rain.scoreboard.IScoreboard;

public class S3PutOperation extends S3Operation 
{
	public static String NAME = "Put";
	
	public S3PutOperation(boolean interactive, IScoreboard scoreboard) 
	{
		super(interactive, scoreboard);
		this.operationName = NAME;
		this.operationIndex = S3Generator.PUT;
	}

	@Override
	public void execute() throws Throwable
	{
		// Append the bytes to write to the operation name
		this.operationName = NAME + "_" + this._value.length;
		
		this.doPut( this._bucket, this._key, this._value );
		this.setFailed( false );
	}
}
