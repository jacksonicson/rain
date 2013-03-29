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

import radlab.rain.IShutdown;
import radlab.rain.RainConfig;
import radlab.rain.scoreboard.Scorecard;
import de.tum.in.sonar.collector.CollectService;
import de.tum.in.sonar.collector.Identifier;
import de.tum.in.sonar.collector.MetricReading;

public class SonarRecorder extends Thread implements IShutdown {
	private static Logger logger = LoggerFactory.getLogger(Scorecard.class);

	private static Object lock = new Object();
	private static SonarRecorder singleton;

	private CollectService.Client client;
	private TTransport transport;
	private String hostname;

	private class Job {
		Identifier id;
		MetricReading value;
	}

	private boolean running = true;
	private BlockingQueue<Job> queue = new LinkedBlockingQueue<Job>();

	private SonarRecorder() {
		try {
			connect();
		} catch (TTransportException e) {
			logger.error("Connection with sonar failed", e);
		} catch (UnknownHostException e) {
			logger.error("Connection with sonar failed, could not determine INet address", e);
		}

		// Set thread name
		setName("SonarRecorder");

		// Launch thread
		this.start();

		// Register for shutdown
		RainConfig.getInstance().register(this);
	}

	public void shutdown() {
		logger.info("Shutting down SonarRecorder"); 
		this.running = false;
		this.interrupt();
	}

	public static SonarRecorder getInstance() {
		synchronized (lock) {
			if (SonarRecorder.singleton == null)
				SonarRecorder.singleton = new SonarRecorder();
		}

		return SonarRecorder.singleton;
	}

	private void connect() throws TTransportException, UnknownHostException {
		// Read configuration
		String sonarServer = RainConfig.getInstance().sonarHost;
		logger.debug("sonar server: " + sonarServer);

		// Get hostname
		InetAddress addr = InetAddress.getLocalHost();
		hostname = addr.getHostName();

		// Get Sonar connection
		transport = new TSocket(sonarServer, 7921);
		transport.open();

		TProtocol protocol = new TBinaryProtocol(transport);

		// Create new client
		client = new CollectService.Client(protocol);
	}

	public void disconnect() {
		transport.close();
	}

	public void run() {
		while (running || !queue.isEmpty()) {
			try {
				Job job = queue.take();
				job.id.setHostname(hostname);
				client.logMetric(job.id, job.value);
			} catch (InterruptedException e) {
			} catch (TException e) {
				logger.error("Sonar recorder could not log metric", e);
			}
		}
	}

	public void record(Identifier id, MetricReading value) {
		// Only accept new records if recorder is still running
		if (!running)
			return;

		// Get a new object from pool
		Job job = new Job();
		job.id = id;
		job.value = value;
		queue.add(job);
	}

}
