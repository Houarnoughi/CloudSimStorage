/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package optim_storage_allocation_policy;

import java.util.ArrayList;
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
public class IoVmAllocationPolicyMinCostSolutionEnum extends IoVmAllocationPolicyMigrationAbstract {
	
	/** The max min utilization threshold. */
	private double maxThreshold = 0.9;
	private double minThreshold = 0.9;

	/**
	 * Instantiates a new power vm allocation policy simple.
	 * 
	 * @param list the list
	 */
	public IoVmAllocationPolicyMinCostSolutionEnum(List<? extends Host> hostList,
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
			
			addHistoryEntry(host, getMaxThreshold());
			double totalRequestedMips = 0;
			for (Vm vm : host.getVmList()) {
				totalRequestedMips += vm.getCurrentRequestedTotalMips();
			}
			double utilization = totalRequestedMips / host.getTotalMips();
			if (utilization - getMaxThreshold() > 0.000) {
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
		
		List<Host> ready_pm = new ArrayList<Host>();
		ready_pm = getReadyPmList(excludedHosts);
		IoSolutionsEnumeration exhaustive = new IoSolutionsEnumeration(vmsToMigrate, ready_pm);
		List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
		
		/* Get the min migration Map*/
		migrationMap = exhaustive.getMinPlacementPlan(vmsToMigrate.size());
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
	 * Gets the new vm placement from under utilized host.
	 * 
	 * @param vmsToMigrate the vms to migrate
	 * @param excludedHosts the excluded hosts
	 * @return the new vm placement from under utilized host
	 */
	protected List<Map<String, Object>> getNewVmPlacementFromUnderUtilizedHost(
			List<? extends Vm> vmsToMigrate,
			Set<? extends Host> excludedHosts) {
		List<Host> ready_pm = new ArrayList<Host>();
		ready_pm = getReadyPmList(excludedHosts);
		IoSolutionsEnumeration exhaustive = new IoSolutionsEnumeration(vmsToMigrate, ready_pm);
		List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
		
		/* Get the min migration Map*/
		migrationMap = exhaustive.getMinPlacementPlan(vmsToMigrate.size());
		return migrationMap;
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
	
	@SuppressWarnings("unchecked")
	public <T extends Host> List<T> getReadyPmList(Set<? extends Host> excludedHosts) {
		List<Host> ready_pm = new ArrayList<Host>();
		for (Host pm: getHostList()) {
			if (!excludedHosts.contains(pm)) {
				ready_pm.add(pm);
			}
		}
		return (List<T>) ready_pm;
	}
	
	/**
	 * Gets the utilization threshold.
	 * 
	 * @return the utilization threshold
	 */
	protected double getUtilizationThreshold() {
		return maxThreshold;
	}

}
