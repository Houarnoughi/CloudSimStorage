/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package optim_storage_selection_policy;

import java.util.List;

import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;

import optim_storage_infrastructure.IoHost;
import optim_storage_infrastructure.IoStorageList;
import optim_storage_infrastructure.IoVm;

/**
 * The Minimum Migration Time (MMT) VM selection policy.
 * 
 * If you are using any algorithms, policies or workload included in the power package, please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 */
public class IoVmSelectionPolicyMinimumStorageMigrationTime extends IoVmSelectionPolicy {

	/*
	 * (non-Javadoc)
	 * @see
	 * org.cloudbus.cloudsim.experiments.power.PowerVmSelectionPolicy#getVmsToMigrate(org.cloudbus
	 * .cloudsim.power.PowerHost)
	 */
	@Override
	public Vm getVmToMigrate(IoHost host) {
		List<IoVm> migratableVms = getMigratableVms(host);
		if (migratableVms.isEmpty()) {
			return null;
		}
		Vm vmToMigrate = null;
		
		double minMigTime = Double.MAX_VALUE;
		
		/** Search the VM with the minimum time to migrate :
		 1) Min (VM Image, Device)
		**/
		for (Vm vm : migratableVms) {
			if (vm.isInMigration()) {
				continue;
			}
			
			IoVm ioVm = (IoVm) vm;
			
			// Get the storage device containing this VM
			Storage device = IoStorageList.getDeviceContainsVm(host.getStorageDevices(), ioVm);
			
			// Calculate the time to read 
			double time = Math.max(ioVm.getSize()/device.getMaxTransferRate(), ioVm.getSize()/host.getBw());
			//double ioTime = 1/ioVm.getIoWorkloadModel().getArrivalRate(CloudSim.clock());
			//double all_time = time * ioTime;
			//if (all_time < minMigTime) {
			if (time < minMigTime) {
				minMigTime = time;
				vmToMigrate = vm;
			}
		}
		return vmToMigrate;
	}

}
