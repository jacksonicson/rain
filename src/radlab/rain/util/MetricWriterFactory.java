package radlab.rain.util;

import org.json.JSONObject;

public class MetricWriterFactory {
	public final static String FILE_WRITER_TYPE = "file";
	public final static String SOCKET_WRITER_TYPE = "socket";
	public final static String SOCKET_OBJECT_WRITER_TYPE = "socketObj";
	public final static String SONAR_WRITER_TYPE = "sonar";

	private MetricWriterFactory() {
	}

	public static MetricWriter createMetricWriter(String writerType, JSONObject config) throws Exception {
		if (writerType.equalsIgnoreCase(FILE_WRITER_TYPE))
			return new FileMetricWriter(config);
		else if (writerType.equalsIgnoreCase(SOCKET_WRITER_TYPE))
			return new SocketMetricWriter(config);
		else if (writerType.equalsIgnoreCase(SOCKET_OBJECT_WRITER_TYPE))
			return new SocketMetricObjectWriter(config);
		else if (writerType.equalsIgnoreCase(SONAR_WRITER_TYPE))
			return new SonarMetricWriter(config);
		else
			return null;
	}
}
