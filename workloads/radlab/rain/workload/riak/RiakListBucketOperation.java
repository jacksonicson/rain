package radlab.rain.workload.riak;

import radlab.rain.scoreboard.IScoreboard;

public class RiakListBucketOperation extends RiakOperation 
{
	public static final String NAME = "Delete";
	
	public RiakListBucketOperation(boolean interactive, IScoreboard scoreboard) 
	{
		super(interactive, scoreboard);
		this.operationIndex = RiakGenerator.LIST_BUCKET;
		this.operationName = NAME;
	}

	@Override
	public void execute() throws Throwable 
	{
		Iterable<String> response = this.doListBucket( this._bucket );
		if( response == null )
			throw new Exception( "Empty response for list bucket: " + this._bucket );
		this.setFailed( false );
	}
}
