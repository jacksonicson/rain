package radlab.rain.workload.mongodb;

import radlab.rain.Generator;
import radlab.rain.LoadUnit;
import radlab.rain.Operation;
import radlab.rain.scoreboard.IScoreboard;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBCursor;
import com.mongodb.WriteResult;

public abstract class MongoOperation extends Operation 
{
	protected String _dbName = "";
	protected String _collectionName = "";
	protected String _key = "";
	protected byte[] _value = null;
	protected MongoTransport _mongoClient = null;
	
	public MongoOperation(boolean interactive, IScoreboard scoreboard) 
	{
		super(interactive, scoreboard);
	}

	@Override
	public void cleanup() 
	{
		this._dbName = "";
		this._collectionName = "";
		this._key = "";
		this._value = null;
	}

	@Override
	public void prepare(Generator generator) 
	{
		this._generator = generator;
		MongoGenerator mongoGenerator = (MongoGenerator) generator;
		
		this._mongoClient = mongoGenerator.getMongoTransport();
		
		LoadUnit currentLoadProfile = generator.getLatestLoadProfile();
		if( currentLoadProfile != null )
			this.setGeneratedDuringProfile( currentLoadProfile );
		
		this._dbName = mongoGenerator._dbName;
		this._collectionName = mongoGenerator._collectionName;
	}

	public DBCursor doGet( String key ) throws Exception
	{
		BasicDBObject query = new BasicDBObject();
		query.put( "key", key );
		this._operationRequest = key; // save a record of which key we're getting
		DBCursor cursor = this._mongoClient.get( this._dbName, this._collectionName, query );
		return cursor;
	}
	
	// Make sure that when we put, we insert if it doesn't exist and that we update if it does
	public void doPut( String key, byte[] value ) throws Exception
	{
		BasicDBObject obj = new BasicDBObject();
		this._operationRequest = key; // save a record of which key we're updating
		
		obj.put( "key", key );
		obj.put( "value", value );
		
		BasicDBObject query = new BasicDBObject();
		query.put( "key", key );
		
		WriteResult res = this._mongoClient.updateOne( this._dbName, this._collectionName, query, obj );
		CommandResult cmdRes = res.getLastError();
		if( cmdRes == null )
			throw new Exception( "Error getting command result after write." );
		
		if( !cmdRes.ok() )
			throw new Exception( "Write failed!" );
	}
}
