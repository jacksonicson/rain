package radlab.rain.workload.riak;

import radlab.rain.scoreboard.IScoreboard;

public class RiakDeleteOperation extends RiakOperation 
{
	public static final String NAME = "Delete";
	
	public RiakDeleteOperation(boolean interactive, IScoreboard scoreboard) 
	{
		super(interactive, scoreboard);
		this.operationIndex = RiakGenerator.DELETE;
		this.operationName = NAME;
	}

	@Override
	public void execute() throws Throwable 
	{
		this.doDelete( this._bucket, this._key );
		this.setFailed( false );
	}
}
