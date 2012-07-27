package radlab.rain.communication.thrift;

import java.io.IOException;

import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;

import de.tum.in.storm.rain.RainService;

public class ThriftService {

	public static final int DEFAULT_PORT = 7852;

	private static ThriftService instance;

	private int port = ThriftService.DEFAULT_PORT;
	
	private static Object lock = new Object();

	private RainNonblockingService serviceThread;

	class RainNonblockingService extends Thread {

		private TServer server;

		public void run() {
			try {
				TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(port);

				AsyncRainServiceImpl service = new AsyncRainServiceImpl();
				RainService.Processor<RainService.Iface> processor = new RainService.Processor<RainService.Iface>(service);

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

	public static ThriftService getInstance() {
		synchronized (ThriftService.lock) {
			if (instance == null)
				instance = new ThriftService();

			return instance;
		}
	}

	private ThriftService() {
		// No construction allowed
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int val) {
		this.port = val;
	}

	public void start() throws IOException {
		if (this.serviceThread == null) {
			this.serviceThread = new RainNonblockingService();
			this.serviceThread.start();
		}
	}

	public boolean stop() {
		return this.disconnect();
	}

	public boolean disconnect() {
		this.serviceThread.stopServer();
		return true;
	}

	public static void main(String arg[]) {
		ThriftService service = new ThriftService();
		try {
			service.start();
			service.serviceThread.join();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
