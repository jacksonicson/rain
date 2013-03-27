package radlab.rain.workload.s3;

import radlab.rain.scoreboard.IScoreboard;

public class S3DeleteBucketOperation extends S3Operation 
{
	public static String NAME = "DeleteBucket";
	
	public S3DeleteBucketOperation(boolean interactive, IScoreboard scoreboard) 
	{
		super(interactive, scoreboard);
		this.operationName = NAME;
		this.operationIndex = S3Generator.DELETE_BUCKET;
	}

	@Override
	public void execute() throws Throwable
	{
		this.doDeleteBucket( this._bucket );
		this.setFailed( false );
	}
}
