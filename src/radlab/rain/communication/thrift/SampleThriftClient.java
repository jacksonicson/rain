package radlab.rain.communication.thrift;

import java.io.IOException;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TNonblockingSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tum.in.storm.rain.RainService;
import de.tum.in.storm.rain.RainService.AsyncClient.startBenchmark_call;

public class SampleThriftClient {
	
	private static Logger logger = LoggerFactory.getLogger(SampleThriftClient.class);

	private RainService.AsyncClient client;
	private TNonblockingSocket socket;

	public SampleThriftClient() throws IOException, TException {

		socket = new TNonblockingSocket("localhost", ThriftService.DEFAULT_PORT);
		client = new RainService.AsyncClient(new TBinaryProtocol.Factory(), new TAsyncClientManager(), socket);

		client.startBenchmark(System.currentTimeMillis(), new StartCallback());
	}

	class StartCallback implements AsyncMethodCallback<RainService.AsyncClient.startBenchmark_call> {

		@Override
		public void onComplete(startBenchmark_call response) {
			logger.info("Benchmark started");
			socket.close(); 
			System.exit(0); 
		}

		@Override
		public void onError(Exception exception) {
			logger.error("Failed");
		}
	}

	public static void main(String arg[]) {
		try {
			new SampleThriftClient();
			Thread.sleep(5000);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
