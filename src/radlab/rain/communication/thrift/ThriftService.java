package radlab.rain.communication.thrift;

import java.io.IOException;

import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;

import radlab.rain.Scenario;
import de.tum.in.storm.rain.RainService;

public class ThriftService {

	public static final int DEFAULT_PORT = 7852;

	private int port = ThriftService.DEFAULT_PORT;

	private RainNonblockingService serviceThread;

	private Scenario scenario;

	class RainNonblockingService extends Thread {

		private TServer server;

		public void run() {
			try {
				TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(port);

				AsyncRainServiceImpl service = new AsyncRainServiceImpl(scenario);
				RainService.Processor<RainService.Iface> processor = new RainService.Processor<RainService.Iface>(
						service);

				TNonblockingServer.Args args = new TNonblockingServer.Args(serverTransport);
				args.processor(processor);

				// Server (connects transport and processor)
				server = new TNonblockingServer(args);
				server.serve();
			} catch (TTransportException e) {
				e.printStackTrace();
			}
		}

		void stopServer() {
			server.stop();
		}
	}

	public ThriftService(Scenario scenario) {
		this.scenario = scenario;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int val) {
		this.port = val;
	}

	public void start() throws IOException {
		if (serviceThread == null) {
			serviceThread = new RainNonblockingService();
			serviceThread.start();
		}
	}

	public void stop() {
		serviceThread.stopServer();
	}
}
