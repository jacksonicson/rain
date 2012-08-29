package radlab.rain.util;

import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.json.JSONObject;

import radlab.rain.ResponseTimeStat;
import de.tum.in.sonar.collector.CollectService;
import de.tum.in.sonar.collector.Identifier;
import de.tum.in.sonar.collector.MetricReading;

public class SonarMetricWriter extends MetricWriter {

	private static final Logger logger = Logger.getLogger(SonarMetricWriter.class);

	private final String HOSTNAME;

	private CollectService.Client client;
	private TTransport transport;

	public SonarMetricWriter(JSONObject config) throws Exception {
		super(config);

		System.out.println("Start SonarMetricWriter");

		// Read configuration
		String sonarServer = config.getString("sonarServer");
		logger.debug("sonar server: " + sonarServer);

		// Get hostname
		InetAddress addr = InetAddress.getLocalHost();
		this.HOSTNAME = addr.getHostName();

		// Get Sonar connection
		transport = new TSocket(sonarServer, 7921);
		transport.open();

		TProtocol protocol = new TBinaryProtocol(transport);

		// Create new client
		client = new CollectService.Client(protocol);
	}

	@Override
	public String getDetails() {
		return "SonarMetricWriter";
	}

	@Override
	public boolean write(ResponseTimeStat stat) throws Exception {
		System.out.println("...");
		return false;
	}

	@Override
	public boolean writeSnapshot(ResponseTimeStat stat) throws Exception {
		double avgResponseTime = stat._totalResponseTime / stat._numObservations;
		System.out.println("response_time " + avgResponseTime + " " + HOSTNAME + " " + stat._timestamp);

		Identifier id = new Identifier();
		id.setHostname(HOSTNAME);
		id.setSensor("rain.rtime." + stat._trackName);
		id.setTimestamp(stat._timestamp / 1000);

		MetricReading value = new MetricReading();
		value.setValue(avgResponseTime);

		client.logMetric(id, value);

		return true;
	}

	@Override
	public void close() throws Exception {
		transport.close();
	}

}
