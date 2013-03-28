package radlab.rain.util;

import org.json.JSONObject;

public class MetricWriterFactory {

	public enum Type {
		FILE_WRITER_TYPE("file"), SOCKET_WRITER_TYPE("socket"), SOCKET_OBJECT_WRITER_TYPE("object"), SONAR_WRITER_TYPE(
				"sonar");

		private final String value;

		private Type(String value) {
			this.value = value;
		}

		public String toString() {
			return value;
		}
	}

	private MetricWriterFactory() {
	}

	public static MetricWriter createMetricWriter(Type type, JSONObject config) throws Exception {
		switch (type) {
		case FILE_WRITER_TYPE:
			return new FileMetricWriter(config);
		case SOCKET_OBJECT_WRITER_TYPE:
			return new SocketMetricWriter(config);
		case SOCKET_WRITER_TYPE:
			return new SocketMetricObjectWriter(config);
		case SONAR_WRITER_TYPE:
			return new SonarMetricWriter(config);
		}

		return null;
	}
}
