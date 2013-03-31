package radlab.rain.scoreboard;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.util.MetricWriter;

/**
 * Receives @see ResponseTimeStat objects. It aggregates all incoming data and writes the aggregated stats using a @see
 * MetricWriter
 */
class SnapshotWriterThread extends Thread {
	private static Logger logger = LoggerFactory.getLogger(SnapshotWriterThread.class);

	// Interrupt metric writer thread
	private boolean interrupted = false;

	// Reference to the metric writer
	private MetricWriter metricWriter = null;

	// Queue for processing stats objects
	private BlockingQueue<ResponseTimeStat> queue = new LinkedBlockingQueue<ResponseTimeStat>();

	/**
	 * Put a new stat object into the incoming queue
	 */
	void accept(ResponseTimeStat responseTimeStat) {
		try {
			queue.put(responseTimeStat);
		} catch (InterruptedException e) {
			logger.debug("Interrupted while logging response time stat");
		}
	}

	@Override
	public void run() {
		while (!interrupted) {
			try {
				ResponseTimeStat nextStat = queue.take();
				metricWriter.write(nextStat);
			} catch (InterruptedException e) {
				// Thread will terminate if interrupt flag is set
				continue;
			} catch (Exception e) {
				logger.error("Metric writer threw an exception", e);
			}
		}

		// Close metric writer as thread will terminate
		if (metricWriter != null) {
			try {
				metricWriter.close();
			} catch (Exception e) {
				logger.error("failed while closing metric writer", e);
			}
		}
	}

	@Override
	public void interrupt() {
		// Set interrupted flag
		interrupted = true;

		// Interrupt this thread
		interrupt();
	}

	void setMetricWriter(MetricWriter metricWriter) {
		this.metricWriter = metricWriter;
	}
}
