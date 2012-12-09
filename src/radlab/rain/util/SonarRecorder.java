package radlab.rain.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private boolean running = true;
	
	private SonarRecorder(String sonarHost) {
		this.SONAR_HOST = sonarHost;

		try {
			connect();
		} catch (TTransportException e) {
			logger.error("Connection with sonar failed", e);
		} catch (UnknownHostException e) {
			logger.error("Connection with sonar failed, could not determine INet address", e);
		}
	}

	public static SonarRecorder getInstance() {
		if (SonarRecorder.singleton == null) {
			String sonarHost = RainConfig.getInstance()._sonarHost;
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
	
	public void run()
	{
		while(running)
		{
			
		}
	}

	public synchronized void record(Identifier id, MetricReading value) {
		id.setHostname(this.hostname);
		try {
			client.logMetric(id, value);
		} catch (TException e) {
			logger.error("could not log Sonar reading", e);
		}
	}

}
