package radlab.rain.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import radlab.rain.RainConfig;
import de.tum.in.storm.iaas.Infrastructure;

public class InfrastructureControl {

	private static final Logger logger = Logger.getLogger(InfrastructureControl.class);

	private TTransport transport;
	private Infrastructure.Client client;

	public InfrastructureControl() {
		try {
			connect();
		} catch (UnknownHostException e) {
			logger.error("failed to connect with IaaS", e);
		} catch (TTransportException e) {
			logger.error("failed to connect with IaaS", e);
		}
	}

	Infrastructure.Client getClient() {
		return this.client;
	}

	public void connect() throws UnknownHostException, TTransportException {
		// Read configuration
		String iaasHost = RainConfig.getInstance().iaasHost;
		logger.debug("IaaS server: " + iaasHost);

		// Get hostname
		InetAddress addr = InetAddress.getLocalHost();
		String hostname = addr.getHostName();

		// Get Sonar connection
		transport = new TSocket(iaasHost, 7921);
		transport.open();

		TProtocol protocol = new TBinaryProtocol(transport);

		// Create new client
		client = new Infrastructure.Client(protocol);
	}

}
