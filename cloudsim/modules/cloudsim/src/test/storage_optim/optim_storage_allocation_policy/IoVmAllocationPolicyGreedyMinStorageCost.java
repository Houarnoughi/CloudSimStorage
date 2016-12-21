/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package optim_storage_allocation_policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.util.ExecutionTimeMeasurer;

import optim_storage_infrastructure.BcomStorageCostModel;
import optim_storage_infrastructure.IoHost;
import optim_storage_infrastructure.IoVm;
import optim_storage_selection_policy.IoVmSelectionPolicy;

/**
 * This a simple class representing a simple VM allocation policy that does not perform any
 * optimization of the VM allocation.
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
public class IoVmAllocationPolicyGreedyMinStorageCost extends IoVmAllocationPolicyMigrationAbstract {
	
	/** The max min utilization threshold. */
	private double maxThreshold = 0.9;
	private double minThreshold = 0.9;

	/**
	 * Instantiates a new power vm allocation policy simple.
	 * 
	 * @param list the list
	 */
	public IoVmAllocationPolicyGreedyMinStorageCost(List<? extends Host> hostList,
			IoVmSelectionPolicy vmSelectionPolicy, 
			double maxThr,
			double minThr,
			double utilization) {
		super(hostList, vmSelectionPolicy);
		setMaxThreshold(maxThr);
		setMinThreshold(minThr);
	}
	
	/**
	 * Optimize allocation of the VMs according to current utilization.
	 * 
	 * @param vmList the vm list
	 * 
	 * @return the array list< hash map< string, object>>
	 */
	@Override
	public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
		Log.printLine("Hamza: optimizeAllocation");
		ExecutionTimeMeasurer.start("optimizeAllocationTotal");

		ExecutionTimeMeasurer.start("optimizeAllocationHostSelection");
		List<IoHost> overUtilizedHosts = getOverUtilizedHosts();
		getExecutionTimeHistoryHostSelection().add(
				ExecutionTimeMeasurer.end("optimizeAllocationHostSelection"));

		printOverUtilizedHosts(overUtilizedHosts);

		saveAllocation();

		ExecutionTimeMeasurer.start("optimizeAllocationVmSelection");
		List<? extends Vm> vmsToMigrate = getVmsToMigrateFromHosts(overUtilizedHosts);
		getExecutionTimeHistoryVmSelection().add(ExecutionTimeMeasurer.end("optimizeAllocationVmSelection"));

		Log.printLine("Reallocation of VMs from the over-utilized hosts:");
		ExecutionTimeMeasurer.start("optimizeAllocationVmReallocation");
		List<Map<String, Object>> migrationMap = getNewVmPlacement(vmsToMigrate, new HashSet<Host>(
				overUtilizedHosts));
		getExecutionTimeHistoryVmReallocation().add(
				ExecutionTimeMeasurer.end("optimizeAllocationVmReallocation"));
		Log.printLine();

		migrationMap.addAll(getMigrationMapFromUnderUtilizedHosts(overUtilizedHosts));

		restoreAllocation();

		getExecutionTimeHistoryTotal().add(ExecutionTimeMeasurer.end("optimizeAllocationTotal"));

		return migrationMap;
	}
	
	/**
	 * Gets the over utilized hosts.
	 * 
	 * @return the over utilized hosts
	 */
	@Override
	protected List<IoHost> getOverUtilizedHosts() {
		List<IoHost> overUtilizedHosts = new LinkedList<IoHost>();
		for (IoHost host : this.<IoHost> getHostList()) {
			if (isHostOverUtilized(host)) {
				overUtilizedHosts.add(host);
			}
		}
		return overUtilizedHosts;
	}
	
	@Override
	protected List<Map<String, Object>> getMigrationMapFromUnderUtilizedHosts(List<IoHost> overUtilizedHosts) {
		List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
		List<IoHost> switchedOffHosts = getSwitchedOffHosts();

		// over-utilized hosts + hosts that are selected to migrate VMs to from over-utilized hosts
		Set<IoHost> excludedHostsForFindingUnderUtilizedHost = new HashSet<IoHost>();
		excludedHostsForFindingUnderUtilizedHost.addAll(overUtilizedHosts);
		excludedHostsForFindingUnderUtilizedHost.addAll(switchedOffHosts);
		excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(migrationMap));

		// over-utilized + under-utilized hosts
		Set<IoHost> excludedHostsForFindingNewVmPlacement = new HashSet<IoHost>();
		excludedHostsForFindingNewVmPlacement.addAll(overUtilizedHosts);
		excludedHostsForFindingNewVmPlacement.addAll(switchedOffHosts);

		int numberOfHosts = getHostList().size();

		while (true) {
			if (numberOfHosts == excludedHostsForFindingUnderUtilizedHost.size()) {
				break;
			}

			IoHost underUtilizedHost = getUnderUtilizedHost(excludedHostsForFindingUnderUtilizedHost);
			if (underUtilizedHost == null) {
				break;
			}

			Log.printLine("Under-utilized host: host #" + underUtilizedHost.getId() + "\n");

			excludedHostsForFindingUnderUtilizedHost.add(underUtilizedHost);
			excludedHostsForFindingNewVmPlacement.add(underUtilizedHost);

			List<? extends Vm> vmsToMigrateFromUnderUtilizedHost = getVmsToMigrateFromUnderUtilizedHost(underUtilizedHost);
			if (vmsToMigrateFromUnderUtilizedHost.isEmpty()) {
				continue;
			}

			Log.print("Reallocation of VMs from the under-utilized host: ");
			if (!Log.isDisabled()) {
				for (Vm vm : vmsToMigrateFromUnderUtilizedHost) {
					Log.print(vm.getId() + " ");
				}
			}
			Log.printLine();

			List<Map<String, Object>> newVmPlacement = getNewVmPlacementFromUnderUtilizedHost(
					vmsToMigrateFromUnderUtilizedHost,
					excludedHostsForFindingNewVmPlacement);

			excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(newVmPlacement));

			migrationMap.addAll(newVmPlacement);
			Log.printLine();
		}

		return migrationMap;
	}
	
	@Override
	protected IoHost getUnderUtilizedHost(Set<? extends Host> excludedHosts) {
		IoHost underUtilizedHost = null;
		for (IoHost host : this.<IoHost> getHostList()) {
			if (excludedHosts.contains(host)) {
				continue;
			}
			
			if (isHostUnderUtilized(host)) {
				underUtilizedHost = host;
			}
		}
		return underUtilizedHost;
	}
	
	/**
	 * Gets the vms to migrate from hosts.
	 * 
	 * @param overUtilizedHosts the over utilized hosts
	 * @return the vms to migrate from hosts
	 */
	protected
			List<? extends Vm>
			getVmsToMigrateFromHosts(List<IoHost> overUtilizedHosts) {
		List<Vm> vmsToMigrate = new LinkedList<Vm>();
		for (IoHost host : overUtilizedHosts) {
			while (true) {
				Vm vm = getVmSelectionPolicy().getVmToMigrate(host);
				if (vm == null) {
					break;
				}
				vmsToMigrate.add(vm);
				host.vmDestroy(vm);
				if (!isHostOverUtilized(host)) {
					break;
				}
			}
		}
		return vmsToMigrate;
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.VmAllocationPolicy#optimizeAllocation(java.util.List)
	 */
	public List<Map<String, Object>> getNewVmPlacement(List<? extends Vm> vmsToMigrate,
			Set<? extends Host> excludedHosts) {
		List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
		//Log.printLine("Hamza: IoVmAllocationPolicyMinStorageCost called");
		
		for (Vm vm : vmsToMigrate) {
			if (vm != null) {
				IoHost allocatedHost = findHostForVm(vm, excludedHosts);
				if (allocatedHost != null) {
					allocatedHost.vmCreate(vm);
					Log.printLine("VM #" + vm.getId() + " allocated to host #" + allocatedHost.getId());
					Map<String, Object> migrate = new HashMap<String, Object>();
					migrate.put("vm", vm);
					migrate.put("host", allocatedHost);
					migrationMap.add(migrate);
				}
			}
		}
		return migrationMap;
	}

	/**
	 * Check if this host is over utilized in terms of IOPS
	 * @param host
	 * @return boolean
	 */
	@Override
	protected boolean isHostOverUtilized(IoHost host) {
		//addHistoryEntry(host, getUtilizationThreshold());
		double totalRequestedIops = 0;
		double totalIops = 0;
		
		// Get the total available IOPS on the 
		for (Storage device : host.getStorageDevices()) {
			totalIops += device.getMaxIops();
		}
		
		// Get the total requested IOPS by VMs
		for (Vm vm : host.getVmList()) {
			IoVm ioVm = (IoVm) vm;
			totalRequestedIops += ioVm.getIoWorkloadModel().getArrivalRate(CloudSim.clock());
		}
		
		//Log.printLine("isHostOverUtilized : offered IOPS "+totalIops+ " Requested "+totalRequestedIops); 
		
		double utilization = totalRequestedIops / totalIops;
		return utilization > getMaxThreshold();
	}
	
	/**
	 * Check if this host is under utilized in terms of IOPS
	 * @param host
	 * @return boolean
	 */
	protected boolean isHostUnderUtilized(IoHost host) {
		double totalRequestedIops = 0;
		double totalIops = 0;
		
		// Get the total available IOPS on the 
		for (Storage device : host.getStorageDevices()) {
			totalIops += device.getMaxIops();
			}
				
		// Get the total requested IOPS by VMs
		for (Vm vm : host.getVmList()) {
			IoVm ioVm = (IoVm) vm;
			totalRequestedIops += ioVm.getRequestedIops();
			}
		
		//Log.printLine("isHostUnderUtilized : offered IOPS "+totalIops+ " Requested "+totalRequestedIops);
		
		double utilization = totalRequestedIops / totalIops;
		return utilization < getMaxThreshold();
		
	}
	
	/**
	 * Find host for vm.
	 * 
	 * @param vm the vm
	 * @param excludedHosts the excluded hosts
	 * @return the power host
	 */
	public IoHost findHostForVm(Vm vm, Set<? extends Host> excludedHosts) {
		double minCost = Double.MAX_VALUE;
		IoHost allocatedHost = null;
		Storage allocatedDevice = null;
		
		// Get the cost model used to run the optimization
		double constCost = 0.0 ; 				//non recurring costs
		double egyPriceKwh = 0.0887; 			// 0.0887 euros / kWh
		double egyPrice =  egyPriceKwh/3600000; // Price per WattSec (Joule)
		double CloudServPrice = 0.5;			// Cloud Service Price / hour
		double bill = CloudServPrice * 30 * 24; // Bill amount / month
		BcomStorageCostModel costModel = new BcomStorageCostModel(constCost, egyPrice, bill);

		for (IoHost host : this.<IoHost> getHostList()) {
			if (excludedHosts.contains(host)) {
				continue;
			}
			
			if (host.isSuitableForVm(vm)) {
				if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterAllocation(host, vm)) {
					continue;
				}
				
				for (Storage device: host.getStorageDevices()) {
					if (device != null) {
						double costAfterAllocation = costModel.getVmStorageCost(vm, device);
						if (costAfterAllocation < minCost) {
						minCost = costAfterAllocation;
						allocatedHost = host;
						allocatedDevice = device;
						vm.setStorageDevice(allocatedDevice.getUid());
						}
					}
				}
			}
		}
		
		return allocatedHost;
	}
	
	/**
	 * Restore allocation.
	 */
	protected void restoreAllocation() {
		for (Host host : getHostList()) {
			host.vmDestroyAll();
			host.reallocateMigratingInVms();
		}
		for (Map<String, Object> map : getSavedAllocation()) {
			Vm vm = (Vm) map.get("vm");
			IoHost host = (IoHost) map.get("host");
			if (!host.vmCreate(vm)) {
				Log.printLine("Couldn't restore VM #" + vm.getId() + " on host #" + host.getId());
				System.exit(0);
			}
			getVmTable().put(vm.getUid(), host);
		}
	}

	public double getMaxThreshold() {
		return maxThreshold;
	}

	public void setMaxThreshold(double maxThreshold) {
		this.maxThreshold = maxThreshold;
	}

	public double getMinThreshold() {
		return minThreshold;
	}

	public void setMinThreshold(double minThreshold) {
		this.minThreshold = minThreshold;
	}

}
