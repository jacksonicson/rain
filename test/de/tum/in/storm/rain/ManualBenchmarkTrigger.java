package de.tum.in.storm.rain;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class ManualBenchmarkTrigger {

	public ManualBenchmarkTrigger() throws TTransportException {
		TTransport tr = new TFramedTransport(new TSocket("localhost", 7852));
		tr.open();

		TProtocol proto = new TBinaryProtocol(tr);
		RainService.Client client = new RainService.Client(proto);
		try {
			client.startBenchmark(System.currentTimeMillis());
		} catch (TException e) {
			e.printStackTrace();
		}

		tr.close();
	}

	public static void main(String arg[]) {
		try {
			new ManualBenchmarkTrigger();
		} catch (TTransportException e) {
			e.printStackTrace();
		}
	}

}
