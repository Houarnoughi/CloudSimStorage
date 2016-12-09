package optim_storage_allocation_policy;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;

import optim_storage_infrastructure.IoHost;
import optim_storage_infrastructure.IoVm;
import optim_storage_selection_policy.IoVmSelectionPolicy;

/**
 * The Static Iops Threshold (IOPSTHR) VM allocation policy.
 * 
 * @author Hamza Ouarnoughi
 * @since CloudSim Toolkit 3.0
 */
public class IoVmAllocationPolicyMigrationStaticIopsThreshold extends IoVmAllocationPolicyMigrationAbstract {

	/** The utilization threshold. */
	private double utilizationThreshold = 0.9;

	/**
	 * Instantiates a new power vm allocation policy migration mad.
	 * 
	 * @param hostList the host list
	 * @param vmSelectionPolicy the vm selection policy
	 * @param utilizationThreshold the utilization threshold
	 */
	public IoVmAllocationPolicyMigrationStaticIopsThreshold(
			List<? extends Host> hostList,
			IoVmSelectionPolicy vmSelectionPolicy,
			double utilizationThreshold) {
		super(hostList, vmSelectionPolicy);
		setUtilizationThreshold(utilizationThreshold);
	}

	/**
	 * Checks if is host over utilized.
	 * 
	 * @param _host the _host
	 * @return true, if is host over utilized
	 */
	@Override
	protected boolean isHostOverUtilized(IoHost host) {
		addHistoryEntry(host, getUtilizationThreshold());
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
		double utilization = totalRequestedIops / totalIops;
		return utilization > getUtilizationThreshold();
	}

	/**
	 * Sets the utilization threshold.
	 * 
	 * @param utilizationThreshold the new utilization threshold
	 */
	protected void setUtilizationThreshold(double utilizationThreshold) {
		this.utilizationThreshold = utilizationThreshold;
	}

	/**
	 * Gets the utilization threshold.
	 * 
	 * @return the utilization threshold
	 */
	protected double getUtilizationThreshold() {
		return utilizationThreshold;
	}

}
