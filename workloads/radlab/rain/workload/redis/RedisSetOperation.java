package radlab.rain.workload.redis;

import radlab.rain.scoreboard.IScoreboard;

public class RedisSetOperation extends RedisOperation 
{
	public static final String NAME = "Set";
	
	public RedisSetOperation(boolean interactive, IScoreboard scoreboard) 
	{
		super(interactive, scoreboard);
		this.operationName = NAME;
		this.operationIndex = RedisGenerator.SET;
	}

	@Override
	public void execute() throws Throwable 
	{
		String result = this.doSet( this._key, this._value );
		if( result != null && result.equalsIgnoreCase( "ok" ) )
			this.setFailed( false );
	}
}
