package radlab.rain.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import radlab.rain.ObjectPoolGeneric;
import radlab.rain.Poolable;
import radlab.rain.RainConfig;
import radlab.rain.scoreboard.Scorecard;
import de.tum.in.sonar.collector.CollectService;
import de.tum.in.sonar.collector.Identifier;
import de.tum.in.sonar.collector.MetricReading;

public class SonarRecorder extends Thread {

	private static Logger logger = LoggerFactory.getLogger(Scorecard.class);

	private String SONAR_HOST;

	private CollectService.Client client;
	private TTransport transport;
	private String hostname;

	private static SonarRecorder singleton;

	private class Job extends Poolable {
		Identifier id;
		MetricReading value;

		private Job(String tag) {
			super(tag);
		}

		@Override
		public void cleanup() {
			id = null;
			value = null;
		}
	}

	private boolean running = true;
	private ObjectPoolGeneric pool = new ObjectPoolGeneric(10000);
	private BlockingQueue<Job> queue = new LinkedBlockingQueue<Job>();

	private SonarRecorder(String sonarHost) {
		this.SONAR_HOST = sonarHost;

		try {
			connect();
		} catch (TTransportException e) {
			logger.error("Connection with sonar failed", e);
		} catch (UnknownHostException e) {
			logger.error(
					"Connection with sonar failed, could not determine INet address",
					e);
		}

		// Launch thread
		this.start();
	}

	public void shutdown() {
		this.running = false;
	}

	public static SonarRecorder getInstance() {
		if (SonarRecorder.singleton == null) {
			String sonarHost = RainConfig.getInstance().sonarHost;
			SonarRecorder.singleton = new SonarRecorder(sonarHost);
		}

		return SonarRecorder.singleton;
	}

	private void connect() throws TTransportException, UnknownHostException {
		// Read configuration
		String sonarServer = SONAR_HOST;
		logger.debug("sonar server: " + sonarServer);

		// Get hostname
		InetAddress addr = InetAddress.getLocalHost();
		this.hostname = addr.getHostName();

		// Get Sonar connection
		transport = new TSocket(sonarServer, 7921);
		transport.open();

		TProtocol protocol = new TBinaryProtocol(transport);

		// Create new client
		client = new CollectService.Client(protocol);
	}

	public void disconnect() {
		this.transport.close();
	}

	public void run() {
		while (running || !queue.isEmpty()) {
			try {
				Job job = queue.take();
				job.id.setHostname(this.hostname);
				client.logMetric(job.id, job.value);

				// Return object to pool
				pool.returnObject(job);

			} catch (InterruptedException e) {
				logger.warn("Iterrupted sonar recorder");
			} catch (TException e) {
				logger.error("Sonar recorder could not log metric", e);
			}
		}
	}

	public synchronized void record(Identifier id, MetricReading value) {
		// Only accept new records if recorder is still running
		if (!running)
			return;

		// Get a new object from pool
		Job job = (Job) pool.rentObject("metric");
		if (job == null)
			job = new Job("metric");

		job.id = id;
		job.value = value;
		queue.add(job);
	}

}
