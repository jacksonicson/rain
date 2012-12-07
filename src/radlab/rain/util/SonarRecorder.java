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

import radlab.rain.scoreboard.Scorecard;
import de.tum.in.sonar.collector.CollectService;
import de.tum.in.sonar.collector.Identifier;
import de.tum.in.sonar.collector.MetricReading;

public class SonarRecorder {

	private static Logger logger = LoggerFactory.getLogger(Scorecard.class);

	private final String SONAR_HOST;

	private CollectService.Client client;
	private TTransport transport;
	private String hostname;

	public SonarRecorder(String sonarHost) {
		this.SONAR_HOST = sonarHost;

		try {
			connect();
		} catch (TTransportException e) {
			logger.error("Connection with sonar failed", e);
		} catch (UnknownHostException e) {
			logger.error("Connection with sonar failed, could not determine INet address", e);
		}
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

	public void record(Identifier id, MetricReading value) {
		id.setHostname(this.hostname);
		try {
			client.logMetric(id, value);
		} catch (TException e) {
			logger.debug("could not log Sonar reading", e);
		}
	}

}
