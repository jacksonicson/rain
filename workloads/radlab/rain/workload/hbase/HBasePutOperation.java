package radlab.rain.workload.hbase;

import radlab.rain.scoreboard.IScoreboard;

public class HBasePutOperation extends HBaseOperation 
{
	public static String NAME = "Put";
	
	public HBasePutOperation(boolean interactive, IScoreboard scoreboard) 
	{
		super(interactive, scoreboard);
		this.operationName = NAME;
		this.operationIndex = HBaseGenerator.WRITE;
	}
	
	@Override
	public void execute() throws Throwable 
	{
		this.doPut( this._key, this._value );
		this.setFailed( false );
	}
}
