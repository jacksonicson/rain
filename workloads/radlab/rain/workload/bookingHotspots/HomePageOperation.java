package radlab.rain.workload.bookingHotspots;

import java.io.IOException;

import radlab.rain.scoreboard.IScoreboard;


/**
 * The Home Page operation displays the Booking home page.
 */
public class HomePageOperation extends BookingOperation 
{
	public HomePageOperation( boolean interactive, IScoreboard scoreboard ) 
	{
		super( interactive, scoreboard );
		this.operationName = "Home Page";
		this.operationIndex = BookingGenerator.HOME_PAGE;
		this.enforceSync = true;
	}
	
	@Override
	public void execute() throws Throwable
	{
		StringBuilder response = this._http.fetchUrl( this.getGenerator().homePageUrl );
		this.trace( this.getGenerator().getCurrentUser() + " GET  " + this.getGenerator().homePageUrl );
		if ( response.length() == 0 )
		{
			throw new IOException( "Received empty response" );
		}
		
		// Load the static files (CSS/JS) associated with the Home Page.
		if (!this.getGenerator().staticHomePageUrlsLoaded) {
			loadStatics( this.getGenerator().staticHomePageUrls );
			this.trace( this.getGenerator().staticHomePageUrls );
			this.getGenerator().staticHomePageUrlsLoaded = true;
		}
		
		this.setFailed( false );
	}
	
}