package radlab.rain.workload.hbase;

import java.util.ArrayList;

import radlab.rain.scoreboard.IScoreboard;

public class HBaseScanOperation extends HBaseOperation 
{
	public static String NAME = "Scan";
	
	public HBaseScanOperation(boolean interactive, IScoreboard scoreboard) 
	{
		super(interactive, scoreboard);
		this.operationName = NAME;
		this.operationIndex = HBaseGenerator.SCAN;
	}
	
	@Override
	public void execute() throws Throwable
	{
		ArrayList<byte[]> results = this.doScan( this._key, this._maxScanRows );
		if( results.size() == 0 )
			throw new Exception( "Empty scan results for stsart key: " + this._key + " rows: " + this._maxScanRows );
		
		this.setFailed( false );
	}
}
