package optim_storage_selection_policy;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import optim_storage_infrastructure.IoHost;
import optim_storage_infrastructure.IoVm;

/**
 * The Minimum Utilization (MU) VM selection policy.
 * We adapt the CPU version with IOPS version
 * 
 * @author Hamza Ouarnoughi
 * @since CloudSim Toolkit 3.0
 */
public class IoVmSelectionPolicyMinimumIopsUtilization extends IoVmSelectionPolicy {

	/*
	 * (non-Javadoc)
	 * @see
	 * org.cloudbus.cloudsim.experiments.power.PowerVmSelectionPolicy#getVmsToMigrate(org.cloudbus
	 * .cloudsim.power.PowerHost)
	 */
	@Override
	public Vm getVmToMigrate(IoHost host) {
		Log.printLine("Hamza: getVmToMigrate IoVmSelectionPolicy called");
		List<IoVm> migratableVms = getMigratableVms(host);
		if (migratableVms.isEmpty()) {
			return null;
		}
		Vm vmToMigrate = null;
		double minMetric = Double.MAX_VALUE;
		for (IoVm vm : migratableVms) {
			if (vm.isInMigration()) {
				continue;
			}
			
			double iopsUtilization = vm.getIoWorkloadModel().getArrivalRate(CloudSim.clock()) / vm.getRequestedIops();
			double cpuUtilization = vm.getTotalUtilizationOfCpuMips(CloudSim.clock()) / vm.getMips();
			double metric = (1/iopsUtilization) * (1/cpuUtilization);
			if (metric < minMetric) {
				minMetric = metric;
				vmToMigrate = vm;
			}
		}
		return vmToMigrate;
	}

}
