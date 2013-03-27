package radlab.rain.workload.gradit;

import radlab.rain.scoreboard.IScoreboard;

public class DashboardOperation extends GraditOperation 
{
	public static String NAME = "Dashboard";
	
	public DashboardOperation(boolean interactive, IScoreboard scoreboard) 
	{
		super(interactive, scoreboard);
		this.operationName = NAME;
		this.operationIndex = GraditGenerator.DASHBOARD;
	}
	
	@Override
	public void execute() throws Throwable 
	{
		this.doDashboard();
		this.setFailed( false );
	}

}
