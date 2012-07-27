package radlab.rain.workload.booking;

import org.apache.http.client.methods.HttpGet;

import radlab.rain.IScoreboard;

	/**
	 * The Search Hotel Results operation displays the hotels that match
	 * the user's search criteria.  Results are displayed in a table.  The
	 * number of rows displayed in the table is controlled by a setting
	 * in the search form.  If not set it defaults to 5 rows.  The user
	 * can click on the hotel name to View Hotel Details.  The user can
	 * also chick on the some column heading to change the sort order of
	 * hotels in the results table.
	 */
public class SearchHotelResultsOperation extends BookingOperation 
{
	public static String NAME = "Search Hotel Results"; 
	
	public SearchHotelResultsOperation( boolean interactive, IScoreboard scoreboard ) 
	{
		super( interactive, scoreboard );
		this._operationName = NAME;
		this._operationIndex = BookingGenerator.SEARCH_HOTEL_RESULTS;
		this._mustBeSync = true;
	}
		
	@Override
	public void execute() throws Throwable
	{
		// The results URL from a previously completed Search Hotel operation should
		// have been saved.
		String searchResultsUrl = this.getGenerator().getLastUrl();
		if (searchResultsUrl == null || (searchResultsUrl != null && searchResultsUrl.length() == 0)) {
			String errorMessage = "SearchResults INTERNAL ERROR - No search results URL saved."; 
			this.debugTrace(errorMessage);
			//this.setFailed(true);
        	//this.setFailureReason(new Exception(errorMessage));
			//return;
			throw new Exception(errorMessage);        	
		}

		this.trace(this.getGenerator().getCurrentUser() + " GET  " + searchResultsUrl);

		HttpGet hotelSearchResultsGet = new HttpGet( searchResultsUrl );
		StringBuilder response = this._http.fetch( hotelSearchResultsGet );
		//System.out.println( "Hotel results: " + response.toString() );

		if ( this._http.getStatusCode() != 200 )
		{
			// We should probably bail here
			String errorMessage = "SearchResults ERROR - GET search result error status: " + this._http.getStatusCode();
        	this.debugTrace(errorMessage);
			//this.setFailed(true);
        	//this.setFailureReason(new Exception(errorMessage));
			//return;
 			throw new Exception(errorMessage);        	
		}		

		this.traceUser(response);

		// TODO: Make a request.
		this.debugTrace( "NYI - Process the search results." );
		
		// TODO: Fill me in.
			
		this.setFailed( false );
	}
}
