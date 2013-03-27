/*
 * Copyright (c) 2010, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  * Neither the name of the University of California, Berkeley
 * nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package radlab.rain.workload.comrades;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import radlab.rain.Generator;
import radlab.rain.LoadProfile;
import radlab.rain.ObjectPool;
import radlab.rain.Operation;
import radlab.rain.RainConfig;
import radlab.rain.Target;
import radlab.rain.util.AppServerStats;
import radlab.rain.util.HttpTransport;
import radlab.rain.util.NegativeExponential;

public class ComradesGenerator extends Generator 
{
	public static String CFG_USE_POOLING_KEY 			= "usePooling";
	public static String CFG_DEBUG_KEY		 			= "debug";
	public static String CFG_ZOOKEEPER_CONN_STRING		= "zookeeperConnString";
	public static String CFG_ZOOKEEPER_APP_SERVER_PATH	= "zookeeperAppServerPath";
	public static int DEFAULT_APP_SERVER_PORT 			= 8080;
	
	
	
	/* Test matrix
	25    75     0     0     0     0 UpdateInterview
     0     0   100     0     0     0 CandidateDetails
     0     0    20     5    55    20 SubmitInterview
    10     0    50     0    20    20 Add Candidate
     0     0     0    10    40    50 Search
     0     0     0    20    50    30 HomePage
     
     Should give us the following steady-state results:
     0.015555301383965 UpdateInterview (~2%) 
     0.011666476037974 CandidateDetails (~1%)
     0.087498570284802 SubmitInterview (~9%)
     0.116664760379736 Add Candidate (~12%) 
     0.414331465172151 Search (~41%)
     0.354283426741404 HomePage (~35%)    
	 */
		
	public static final int HOME_PAGE					= 5;
	public static final int SEARCH_CANDIDATES			= 4;
	public static final int ADD_CANDIDATE				= 3;
	public static final int SUBMIT_INTERVIEW			= 2;
	public static final int CANDIDATE_DETAILS 			= 1;
	public static final int UPDATE_INTERVIEW			= 0;
	
	// Statics URLs
	public String[] homepageStatics; 
	public String[] candidateStatics;
	public String[] interviewStatics;
	
	protected static final String[] HOMEPAGE_STATICS = 
	{
		/*"/stylesheets/base.css?1296794014",
		"/javascripts/jquery.js?1296577051",
		"/javascripts/jquery-ui.js?1296577051",
		"/javascripts/jrails.js?1296577051",
		"/javascripts/application.js?1296577051"*/
	};
	
	protected static final String[] CANDIDATEPAGE_STATICS = 
	{
		/*"/stylesheets/base.css?1296794014",
		"/javascripts/jquery.js?1296577051",
		"/javascripts/jquery-ui.js?1296577051",
		"/javascripts/jrails.js?1296577051",
		"/javascripts/application.js?1296577051"*/
	};
	
	protected static final String[] INTERVIEWPAGE_STATICS = 
	{
		/*"/stylesheets/base.css?1296794014",
		"/javascripts/jquery.js?1296577051",
		"/javascripts/jquery-ui.js?1296577051",
		"/javascripts/jrails.js?1296577051",
		"/javascripts/application.js?1296577051"*/
	};
	
	public static final String HOSTNAME_PORT_SEPARATOR	= ":";
	
	private boolean _usePooling 					= false;
	private boolean _debug 							= false;
	private HttpTransport _http;
	private Random _rand;
	private NegativeExponential _thinkTimeGenerator = null;
	private NegativeExponential _cycleTimeGenerator = null;
	private Vector<AppServerStats> _appServers 		= new Vector<AppServerStats>();
	private boolean _usingZookeeper 				= false;
	
	// App urls
	public String _appServerUrl;
	public String _baseUrl;
	public String _homeUrl;
	public String _candidatesUrl;
	public String _newCandidateUrl;
	
	public ComradesGenerator(Target track) 
	{
		super(track);
		this._rand = new Random();
		this._thinkTime = (long)( track.getMeanThinkTime() * 1000 );
		this._cycleTime = (long)( track.getMeanCycleTime() * 1000 );
	}

	public void initializeUrls( String targetHost, int port )
	{
		this._appServerUrl = targetHost;
		this._baseUrl 	= "http://" + this._appServerUrl + ":" + port;
		this._homeUrl = this._baseUrl;
		
		this._candidatesUrl = this._baseUrl + "/candidates";
		this._newCandidateUrl = this._candidatesUrl + "/new";
				
		this.initializeStaticUrls();
	}
	
	public boolean getIsDebugMode()
	{ return this._debug; }
	
	@Override
	public void initialize() 
	{
		this._http = new HttpTransport();
		// Initialize think/cycle time random number generators (if you need/want them)
		this._cycleTimeGenerator = new NegativeExponential( this._cycleTime );
		this._thinkTimeGenerator = new NegativeExponential( this._thinkTime );
	}
	
	@Override
	public void configure( JSONObject config ) throws JSONException 
	{
		if( config.has(CFG_USE_POOLING_KEY) )
			this._usePooling = config.getBoolean(CFG_USE_POOLING_KEY);

		if( config.has(CFG_DEBUG_KEY) )
			this._debug = config.getBoolean(CFG_DEBUG_KEY);

		ComradesScenarioTrack comradesTrack = null;

		String zkConnString = "";
		String zkPath = "";

		// Get the zookeeper parameter from the RainConfig first. If that
		// doesn't
		// exist then get it from the generator config parameters
		try 
		{
			String zkString = RainConfig.getInstance()._zooKeeper;
			if( zkString != null && zkString.trim().length() > 0 )
				zkConnString = zkString;
			else zkConnString = config.getString( CFG_ZOOKEEPER_CONN_STRING );

			zkPath = RainConfig.getInstance()._zkPath;
			if( zkPath == null || zkPath.trim().length() == 0 )
				zkPath = config.getString( CFG_ZOOKEEPER_APP_SERVER_PATH );

			// Get the track - see whether it's the "right" kind of track
			if( this._loadTrack instanceof ComradesScenarioTrack ) 
			{
				comradesTrack = (ComradesScenarioTrack) this._loadTrack;
				comradesTrack.configureZooKeeper(zkConnString, zkPath);
				if( comradesTrack.isConfigured() )
					this._usingZookeeper = true;
			}
		} 
		catch( JSONException e ) 
		{
			System.out.println( this + " Error obtaining ZooKeeper info from RainConfig instance or generator paramters. Falling back on targetHost and port." );
			this._usingZookeeper = false;
		}

		if( this._usingZookeeper ) 
		{
			// Get the servers
			Hashtable<String, AppServerStats> appServerTraffic = comradesTrack.getAppServers();
			// Push them all into a list and sort them
			if( appServerTraffic.size() > 0 )
			{
				this._appServers.clear();
				// Purge the traffic stats list and then re-populate and sort so that we
				// get the least loaded server
				Iterator<AppServerStats> trafficIt = appServerTraffic.values().iterator();
				while( trafficIt.hasNext() )
					this._appServers.add( trafficIt.next() );
				
				// Sort the traffic stats so that we can find the least loaded server
				Collections.sort( this._appServers );
			}
			
			// Pick the first server, this should be the most lightly loaded server
			// based on the sort
			String[] appServerNamePort = this._appServers.firstElement()._appServer.split( HOSTNAME_PORT_SEPARATOR );//this._appServers[this._currentAppServer].split( HOSTNAME_PORT_SEPARATOR );
		
			if( appServerNamePort.length == 2 )
				this.initializeUrls( appServerNamePort[0], Integer.parseInt( appServerNamePort[1] ) );
			else if( appServerNamePort.length == 1 )
				this.initializeUrls( appServerNamePort[0], DEFAULT_APP_SERVER_PORT );
		} 
		else 
		{
			String appServer = this.getTrack().getTargetHostName() + HOSTNAME_PORT_SEPARATOR + this.getTrack().getTargetHostPort();
			this._appServers.add( new AppServerStats( appServer, 0L ) );
			this.initializeUrls( this.getTrack().getTargetHostName(), this.getTrack().getTargetHostPort() );
		}

		// Initialize think/cycle time random number generators (if you
		// need/want them)
		//System.out.println("Think time: " + this._thinkTime);
		this._cycleTimeGenerator = new NegativeExponential( this._cycleTime );
		this._thinkTimeGenerator = new NegativeExponential( this._thinkTime );
	}

	public void initializeStaticUrls()
	{
		this.homepageStatics	= joinStatics( HOMEPAGE_STATICS );
		this.candidateStatics 	= joinStatics( CANDIDATEPAGE_STATICS );
		this.interviewStatics 	= joinStatics( INTERVIEWPAGE_STATICS );
	}
	
	private String[] joinStatics(String[]... staticsLists) 
	{
		LinkedHashSet<String> urlSet = new LinkedHashSet<String>();

		for( String[] staticList : staticsLists ) 
		{
			for( int i = 0; i < staticList.length; i++ ) 
			{
				String url = "";
				if( staticList[i].trim().startsWith("http://") )
					url = staticList[i].trim();
				else url = this._baseUrl + staticList[i].trim();

				urlSet.add(url);
			}
		}

		return (String[]) urlSet.toArray( new String[0] );
	}

	/**
	 * Returns the pre-existing HTTP transport.
	 * 
	 * @return An HTTP transport.
	 */
	public HttpTransport getHttpTransport() 
	{
		return this._http;
	}
	
	@Override
	public void dispose() 
	{
		// TODO Auto-generated method stub
	}

	@Override
	public long getCycleTime() 
	{
		if( this._cycleTime == 0 )
			return 0;
		else 
		{
			// Example cycle time generator
			long nextCycleTime = (long) this._cycleTimeGenerator.nextDouble();
			// Truncate at 5 times the mean (arbitrary truncation)
			return Math.min(nextCycleTime, (5 * this._cycleTime));
		}
	}

	@Override
	public long getThinkTime() 
	{
		if (this._thinkTime == 0)
			return 0;
		else 
		{
			// Example think time generator
			long nextThinkTime = (long) this._thinkTimeGenerator.nextDouble();
			// Truncate at 5 times the mean (arbitrary truncation)
			return Math.min(nextThinkTime, (5 * this._thinkTime));
		}
	}

	@Override
	public String toString() 
	{
		StringBuffer buf = new StringBuffer();
		buf.append("[").append(this._name).append("]");
		return buf.toString();
	}
	
	@Override
	public Operation nextRequest( int lastOperation ) 
	{
		// Get the current load profile if we need to look inside of it to
		// decide
		// what to do next
		LoadProfile currentLoad = this.getTrack().getCurrentLoadProfile();
		this._latestLoadProfile = currentLoad;

		// if( true )
		// return getOperation( 0 );

		int nextOperation = -1;

		if (lastOperation == -1) 
		{
			nextOperation = HOME_PAGE;
		} 
		else 
		{
			if (this._usingZookeeper) 
			{
				ComradesScenarioTrack comradesTrack = (ComradesScenarioTrack) this._loadTrack;
				if( comradesTrack.getAppServerListChanged() )
					comradesTrack.updateAppServerList();
				
				// Always get the list of app servers cached in the track - this doesn't cause a query to
				// ZooKeeper
				Hashtable<String, AppServerStats> appServerTraffic = comradesTrack.getAppServers();
				// Push them all into a list and sort them
				if( appServerTraffic.size() > 0 )
				{
					this._appServers.clear();
					// Purge the traffic stats list and then re-populate and sort so that we
					// get the least loaded server
					Iterator<AppServerStats> trafficIt = appServerTraffic.values().iterator();
					while( trafficIt.hasNext() )
						this._appServers.add( trafficIt.next() );
					
					// Sort the traffic stats so that we can find the least loaded server
					if( appServerTraffic.size() > 1 )
					{
						Collections.sort( this._appServers );
						if( this._debug )
						{
							// Print out the list of servers
							for( AppServerStats stats :  this._appServers )
								System.out.println( this + stats.toString() );
						}
					}
				}
			}

			if (this._appServers == null) 
			{
				String appServer = this.getTrack().getTargetHostName() + HOSTNAME_PORT_SEPARATOR + this.getTrack().getTargetHostPort();
				this._appServers.add( new AppServerStats( appServer, 0L ) );
				this.initializeUrls( this.getTrack().getTargetHostName(), this.getTrack().getTargetHostPort() );
			}

			// Pick the new target based on the current app server value
			String nextAppServerHostPort[] = null;
			
			if (this._appServers.size() == 0) 
			{
				System.out.println( "No app servers available to target. Executing no-op." );
				return null; // no-op
			}

			nextAppServerHostPort = this._appServers.firstElement()._appServer.split( HOSTNAME_PORT_SEPARATOR );

			if( nextAppServerHostPort.length == 2 )
				this.initializeUrls(nextAppServerHostPort[0], Integer.parseInt( nextAppServerHostPort[1] ) );
			else if( nextAppServerHostPort.length == 1 )
				this.initializeUrls(nextAppServerHostPort[0], DEFAULT_APP_SERVER_PORT );

			// Get the selection matrix
			double[][] selectionMix = this.getTrack().getMixMatrix( currentLoad.getMixName() ).getSelectionMix();
			double rand = this._rand.nextDouble();

			int j;
			for( j = 0; j < selectionMix.length; j++ ) 
			{
				if( rand <= selectionMix[lastOperation][j] ) 
				{
					break;
				}
			}
			nextOperation = j;
		}
		return getOperation( nextOperation );
	}

	/*
	 public static final int HOME_PAGE					= 0;
	public static final int ADD_CANDIDATE				= 1;
	public static final int SUBMIT_INTERVIEW			= 2;
	public static final int VIEW_CANDIDATE_DETAILS 		= 3;
	public static final int UPDATE_INTERVIEW			= 4;
	 */
	
	private ComradesOperation getOperation(int opIndex) 
	{
		switch (opIndex) 
		{
			case HOME_PAGE: return this.createHomePageOperation();
			case SEARCH_CANDIDATES: return this.createSearchCandidatesOperation();
			case ADD_CANDIDATE: return this.createCandidateOperation();
			case SUBMIT_INTERVIEW: return this.createSubmitInterviewOperation();
			case CANDIDATE_DETAILS: return this.createCandidateDetailsOperation();
			case UPDATE_INTERVIEW: return this.createUpdateInterviewOperation();
			default: return null;
		}
	}
	
	// Factory methods for creating operations
	public HomePageOperation createHomePageOperation()
	{
		HomePageOperation op = null;
		
		if( this._usePooling )
		{
			ObjectPool pool = this.getTrack().getObjectPool();
			op = (HomePageOperation) pool.rentObject( HomePageOperation.NAME );	
		}
		
		if( op == null )
			op = new HomePageOperation( this.getTrack().getInteractive(), this.getScoreboard() );
		
		op.prepare( this );
		return op;
	}
	
	public AddCandidateOperation createCandidateOperation()
	{
		AddCandidateOperation op = null;
		
		if( this._usePooling )
		{
			ObjectPool pool = this.getTrack().getObjectPool();
			op = (AddCandidateOperation) pool.rentObject( AddCandidateOperation.NAME );	
		}
		
		if( op == null )
			op = new AddCandidateOperation( this.getTrack().getInteractive(), this.getScoreboard() );
		
		op.prepare( this );
		return op;
	}
	
	public SubmitInterviewOperation createSubmitInterviewOperation() 
	{
		SubmitInterviewOperation op = null;
		
		if( this._usePooling )
		{
			ObjectPool pool = this.getTrack().getObjectPool();
			op = (SubmitInterviewOperation) pool.rentObject( SubmitInterviewOperation.NAME );	
		}
		
		if( op == null )
			op = new SubmitInterviewOperation( this.getTrack().getInteractive(), this.getScoreboard() );
		
		op.prepare( this );
		return op;
	}
	
	public CandidateDetailsOperation createCandidateDetailsOperation()
	{
		CandidateDetailsOperation op = null;
		
		if( this._usePooling )
		{
			ObjectPool pool = this.getTrack().getObjectPool();
			op = (CandidateDetailsOperation) pool.rentObject( CandidateDetailsOperation.NAME );	
		}
		
		if( op == null )
			op = new CandidateDetailsOperation( this.getTrack().getInteractive(), this.getScoreboard() );
		
		op.prepare( this );
		return op;
	}
	
	public UpdateInterviewOperation createUpdateInterviewOperation()
	{
		UpdateInterviewOperation op = null;
		
		if( this._usePooling )
		{
			ObjectPool pool = this.getTrack().getObjectPool();
			op = (UpdateInterviewOperation) pool.rentObject( UpdateInterviewOperation.NAME );	
		}
		
		if( op == null )
			op = new UpdateInterviewOperation( this.getTrack().getInteractive(), this.getScoreboard() );
		
		op.prepare( this );
		return op;
	}
	
	public SearchCandidatesOperation createSearchCandidatesOperation()
	{
		SearchCandidatesOperation op = null;
		
		if( this._usePooling )
		{
			ObjectPool pool = this.getTrack().getObjectPool();
			op = (SearchCandidatesOperation) pool.rentObject( SearchCandidatesOperation.NAME );	
		}
		
		if( op == null )
			op = new SearchCandidatesOperation( this.getTrack().getInteractive(), this.getScoreboard() );
		
		op.prepare( this );
		return op;
	}
}
