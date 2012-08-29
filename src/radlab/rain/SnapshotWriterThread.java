package radlab.rain;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.util.MetricWriter;

class SnapshotWriterThread extends Thread {
	private static Logger log = LoggerFactory.getLogger(Scoreboard.class);

	private final String _trackName;

	private boolean _done = false;
	private MetricWriter _metricWriter = null;

	private LinkedList<ResponseTimeStat> _responseTimeQ = new LinkedList<ResponseTimeStat>();
	private Object _responseTimeQLock = new Object();

	private LinkedList<ResponseTimeStat> _processingQ = new LinkedList<ResponseTimeStat>();
	private ObjectPoolGeneric _statsObjPool = null;

	public SnapshotWriterThread(String trackName) {
		this._trackName = trackName;

		// Create object pool
		this._statsObjPool = new ObjectPoolGeneric(80000);
		this._statsObjPool.setTrackName(trackName);
	}

	void accept(ResponseTimeStat responseTimeStat) {
		synchronized (this._responseTimeQLock) {
			this._responseTimeQ.add(responseTimeStat);
		}
	}

	public ResponseTimeStat provisionRTSObject() {
		return (ResponseTimeStat) this._statsObjPool.rentObject(ResponseTimeStat.NAME);
	}

	public void run() {
		// While there's work to do
		while (!this._done || this._responseTimeQ.size() > 0) {
			if (this._responseTimeQ.size() > 0) {
				// Do the queue swap
				synchronized (this._responseTimeQLock) {
					LinkedList<ResponseTimeStat> temp = this._responseTimeQ;
					this._responseTimeQ = this._processingQ;
					this._processingQ = temp;
				}

				// Write everything out
				while (!this._processingQ.isEmpty()) {
					ResponseTimeStat stat = this._processingQ.removeFirst();

					try {
						if (this._metricWriter != null)
							this._metricWriter.write(stat);
					} catch (Exception e) {
					} finally {
						// Important
						// Return the stats object to the pool
						if (stat != null)
							this._statsObjPool.returnObject(stat);
					}
				}
			} else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException tie) {
					log.info(this + " snapshot thread interrupted");
				}
			}
		}

		// Close metric writer
		if (this._metricWriter != null) {
			try {
				this._metricWriter.close();
			} catch (Exception e) {
				log.error("failed while closing metric writer", e);
			}
		}

		// Shutdown object pool
		if (this._statsObjPool.isActive())
			this._statsObjPool.shutdown();
	}

	public void set_done(boolean _done) {
		this._done = _done;
	}

	public boolean getDone() {
		return this._done;
	}

	public void setDone(boolean val) {
		this._done = val;
	}

	public void setMetricWriter(MetricWriter val) {
		this._metricWriter = val;
	}

	public String toString() {
		return "[SNAPSHOTWRITER TRACK: " + this._trackName + "]";
	}
}
