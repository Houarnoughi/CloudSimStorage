package io_storage;

import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;

/**
 * An interface to implement a storage cost model for virtual machines
 * with different levels. The first level is storing a VM is a given storage device
 * 
 * @author hamza
 *
 */
public interface IoStorageCostModel {
	/**
	 * 
	 * @param vms a virtual machines
	 * @param host a host in the data center
	 * @return the cost of storing VM data and executing its IO workload on this host
	 */
	<VM extends Vm, SD extends Storage> 
	double getVmStorageCost(VM vm, SD device);

}
