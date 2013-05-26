package radlab.rain.workload.http;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import radlab.rain.target.DefaultTarget;
import radlab.rain.util.InfrastructureControl;
import de.tum.in.storm.iaas.DomainSize;

public class TestTarget extends DefaultTarget {
	private static final Logger logger = Logger.getLogger(TestTarget.class);

	private String targetDomain;

	private InfrastructureControl iaas;

	private DomainSize size;

	public TestTarget(DomainSize size) {
		super();
		this.iaas = new InfrastructureControl();
		this.size = size;
	}

	@Override
	public void setup() {
		try {
			// Get a new domain
			switch (size) {
			case LARGE:
				targetDomain = iaas.getClient().allocateDomain(2, iaas.SIZE_LARGE);
				break;
			case MEDIUM:
				targetDomain = iaas.getClient().allocateDomain(2, iaas.SIZE_MEDIUM);
				break;
			case SMALL:
				targetDomain = iaas.getClient().allocateDomain(2, iaas.SIZE_SMALL);
				break;
			}

			// Wait until the domain is available
			while (!iaas.getClient().isDomainReady(targetDomain)) {
				logger.info("waiting for target domain...");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// Launch all necessary services
			logger.info("Launching glassfish and database services");
			iaas.getClient().launchDrone("start", targetDomain);
		} catch (TException e) {
			logger.error("error while creating target domain");
		}
	}

	@Override
	public void teardown() {
		try {
			// Get rid of the domain
			iaas.getClient().deleteDomain(targetDomain);
		} catch (TException e) {
			logger.error("error while deleting target domain");
		}

		// Disconnect from IaaS
		iaas.disconnect();
	}
}
