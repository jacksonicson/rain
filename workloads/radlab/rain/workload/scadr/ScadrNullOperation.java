package radlab.rain.workload.scadr;

import radlab.rain.scoreboard.IScoreboard;

public class ScadrNullOperation extends ScadrOperation 
{
	public static final String NAME = "NullOp";
	
	public ScadrNullOperation( boolean interactive, IScoreboard scoreboard ) 
	{
		super(interactive, scoreboard);
		this.operationName = NAME;
		this.operationIndex = ScadrGenerator.NULL_OP;
	}

	@Override
	public void execute() throws Throwable 
	{
		// Do nothing for 25 msecs
		Thread.sleep( 25 );
		this.setFailed( false );
	}

}
