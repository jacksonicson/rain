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
//		iaas = new InfrastructureControl();
	}

	@Override
	public void setup() {
//		try {
//			// Get a new domain
//			targetDomain = iaas.getClient().allocateDomain();
//			
//			// Wait until the domain is available
//			while (!iaas.getClient().isDomainReady(targetDomain)) {
//				logger.info("waiting for target domain...");
//				try {
//					Thread.sleep(5000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//			
//			// Launch all necessary services
//			logger.info("Launching glassfish and database services");
//			iaas.getClient().launchDrone("start", targetDomain);
//		} catch (TException e) {
//			logger.error("error while creating target domain");
//		}
	}

	@Override
	public void teardown() {
//		try {
//			// Get rid of the domain
//			iaas.getClient().deleteDomain(targetDomain);
//		} catch (TException e) {
//			logger.error("error while deleting target domain");
//		}
		
		// Disconnect from IaaS
//		iaas.disconnect(); 
	}
}
