package radlab.rain.workload.http;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import radlab.rain.target.DefaultTarget;
import radlab.rain.util.InfrastructureControl;

public class TestTarget extends DefaultTarget {
	private static final Logger logger = Logger.getLogger(TestTarget.class);

	private String targetDomain;

	private InfrastructureControl iaas;

	public TestTarget() {
		super();
		iaas = new InfrastructureControl();
	}

	@Override
	public void setup() {
		try {
			targetDomain = iaas.getClient().allocateDomain();
			while (!iaas.getClient().isDomainReady(targetDomain)) {
				logger.info("waiting for target domain...");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (TException e) {
			logger.error("error while creating target domain");
		}
	}

	@Override
	public void teardown() {
		try {
			iaas.getClient().deleteDomain(targetDomain);
		} catch (TException e) {
			logger.error("error while deleting target domain");
		}
	}
}
