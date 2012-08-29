package radlab.rain;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.util.MetricWriter;

class SnapshotWriterThread extends Thread {
	private static Logger log = LoggerFactory.getLogger(Scoreboard.class);

	// Owning scoreboard
	private Scoreboard _owner = null;
	private boolean _done = false;
	private MetricWriter _metricWriter = null;

	private boolean _jdbcDriverLoaded = false;
	// Use JDBC to talk to the db
	private Connection _conn = null;
	// Keep track of the last stats sent so that we
	// can just send the deltas
	private long _lastTotalResponseTime = -1;
	private long _lastTotalOpsSuccessful = -1;
	private long _lastTotalActionsSuccessful = -1;

	// private File _statsLog;

	public boolean getDone() {
		return this._done;
	}

	public void setDone(boolean val) {
		this._done = val;
	}

	public MetricWriter getMetricWriter() {
		return this._metricWriter;
	}

	public void setMetricWriter(MetricWriter val) {
		this._metricWriter = val;
	}

	private LinkedList<ResponseTimeStat> _todoQ = new LinkedList<ResponseTimeStat>();

	public SnapshotWriterThread(Scoreboard owner) {
		this._owner = owner;
	}

	public String toString() {
		return "[SNAPSHOTWRITER TRACK: " + this._owner._trackName + "]";
	}

	@SuppressWarnings("unused")
	private void pushStatsToMetricDB() throws SQLException {
		long metricTime = System.currentTimeMillis();
		if (this._jdbcDriverLoaded) {
			// Try to create or reuse a connection and put data in the db
			if (this._conn == null) {
				String connectionString = System.getProperty("dashboarddb");
				if (connectionString != null && connectionString.trim().length() > 0)
					this._conn = DriverManager.getConnection(connectionString);
			}

			long responseTimeDelta = 0;
			long opsSuccessfulDelta = 0;
			long actionsSuccessfulDelta = 0;

			long currentTotalOpsResponseTime = this._owner.finalCard._totalOpResponseTime;
			long currentTotalOpsSuccessful = this._owner.finalCard._totalOpsSuccessful;
			long currentTotalActionsSuccessful = this._owner.finalCard._totalActionsSuccessful;

			// Should we send the total response time etc. or just the deltas
			if (this._lastTotalResponseTime == -1) {
				responseTimeDelta = currentTotalOpsResponseTime;
				opsSuccessfulDelta = currentTotalOpsSuccessful;
				actionsSuccessfulDelta = currentTotalActionsSuccessful;
			} else {
				responseTimeDelta = currentTotalOpsResponseTime - this._lastTotalResponseTime;
				opsSuccessfulDelta = currentTotalOpsSuccessful - this._lastTotalOpsSuccessful;
				actionsSuccessfulDelta = currentTotalActionsSuccessful - this._lastTotalActionsSuccessful;
			}

			// Update the previous observations with the current
			this._lastTotalResponseTime = currentTotalOpsResponseTime;
			this._lastTotalOpsSuccessful = currentTotalOpsSuccessful;
			this._lastTotalActionsSuccessful = currentTotalActionsSuccessful;

			// Do the printing
			log.info(this + " " + metricTime + " ttl response time delta (msecs): " + responseTimeDelta
					+ " operations successful delta: " + opsSuccessfulDelta + " actions successful delta: "
					+ actionsSuccessfulDelta);

			if (this._conn != null && !this._conn.isClosed()) {

				PreparedStatement stmnt = this._conn
						.prepareStatement("insert into rainStats (timestamp,trackName,totalResponseTime,operationsSuccessful, actionsSuccessful) values (?,?,?,?,?)");
				stmnt.setLong(1, metricTime);
				stmnt.setString(2, this._owner._owner._name);
				// stmnt.setLong( 3, this._owner.finalCard._totalOpResponseTime );
				// stmnt.setLong( 4, this._owner.finalCard._totalOpsSuccessful );
				// stmnt.setLong( 5, this._owner.finalCard._totalActionsSuccessful );
				stmnt.setLong(3, responseTimeDelta);
				stmnt.setLong(4, opsSuccessfulDelta);
				stmnt.setLong(5, actionsSuccessfulDelta);
				stmnt.execute();
			}
		}
	}

	public void run() {
		// Do the queue swap and then write until there's nothing left to write
		while (!this._done || this._owner._responseTimeQ.size() > 0) {
			if (this._owner._responseTimeQ.size() > 0) {
				// Print pre-swap sizes
				// log.info( "Pre-swap todoQ: " + this._todoQ.size() + " responseTimeQ: " + this._owner._responseTimeQ.size()
				// );

				// grab the queue lock and swap queues so we can write what's currently there
				synchronized (this._owner._responseTimeQLock) {
					LinkedList<ResponseTimeStat> temp = this._owner._responseTimeQ;
					this._owner._responseTimeQ = this._todoQ;
					this._todoQ = temp;
				}

				// log.info( "Post-swap todoQ: " + this._todoQ.size() + " responseTimeQ: " + this._owner._responseTimeQ.size()
				// );

				// Now write everything out
				while (!this._todoQ.isEmpty()) {
					ResponseTimeStat stat = this._todoQ.removeFirst();

					try {
						if (this._metricWriter != null)
							this._metricWriter.write(stat);
					} catch (Exception e) {
					} finally {
						// Return the stats object to the pool
						if (stat != null) {
							this._owner._statsObjPool.returnObject(stat);
						}
					}
				}
				// log.info( this + " todoQ empty, re-checking..." );
			} else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException tie) {
					log.info(this + " snapshot thread interrupted.");
					// Close the log file if we're interrupted
					if (this._metricWriter != null) {
						try {
							this._metricWriter.close();
						} catch (Exception e) {
						}
					}
				}
			}
		}// end-while there's work to do

		// Close nicely if we're not interrupted
		if (this._metricWriter != null) {
			try {
				this._metricWriter.close();
			} catch (Exception e) {
			}
		}
	}

	public void set_done(boolean _done) {
		this._done = _done;
	}
}
