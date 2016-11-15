package io_storage;

import java.util.List;

import org.cloudbus.cloudsim.Storage;


/**
 * The Wear out cost model.
 * for more informations, please refer to  the paper 
 * Hamza Ouarnoughi, Jalil Boukhobza, Frank Singhoff, Stéphane Rubini "A Cost Model for Virtual Machine Storage in Cloud IaaS Context"
 * @author hamza
 *
 */
public class IoStorageWearOutModel {

	/**
	 * Gets storage system wear out cost (Data center level)
	 * 
	 * @return cost in dollars
	 */
	public double getDatacenterStorageWearOut(List<IoHost> hosts, double time) {
		
		double wearout = 0.0;
		
		for (IoHost host: hosts) {
			
			wearout += getHostStorageWearOut(host, time);
		}
		return wearout;
	}
	
	/**
	 * Gets storage system wear out cost (Host machine level)
	 * 
	 * @return cost in dollars
	 */
	public double getHostStorageWearOut(IoHost host, double time) {
		
		double wearout = 0.0;
		
		// Get all storage devices connected to this host
		for (Storage device: host.getStorageDevices()) {
			if(device instanceof IoHarddriveStorage) {
				
				// I/O Workload Execution cost
				wearout += getWearOutCostFromHdd((IoHarddriveStorage)device, host.getVmList(), time);
				
			}else if (device instanceof IoSolidStateStorage) {
				
				// I/O workload execution cost
				wearout += getWearOutCostFromSsd((IoSolidStateStorage)device, host.getVmList(), time);
					}
			}
		return wearout;
	}
	
	/**
	 * Gets storage system wear out cost (Storage device machine level)
	 * 
	 * @return cost in dollars
	 */
	private double getWearOutCostFromHdd(IoHarddriveStorage hdd, List<IoVm> vmlist, double time){
		List<String> fileNameList = hdd.getFileNameList();
		double wearout = 0;
		
		// If there are no vm files
		if (fileNameList.size() <= 0 ) {
			return 0;
		}
		
		// We assume that files' name are the same as VMs' name (ID)
				for (IoVm vm: vmlist) {
					if(fileNameList.contains(vm.getUid())) {
						
						wearout += getCostPerPowerOnSecond(hdd);
					}
				}
		return wearout;
	}
	
	/**
	 * Gets storage system wear out cost (Storage device machine level)
	 * 
	 * @return cost in dollars
	 */
	private double getWearOutCostFromSsd(IoSolidStateStorage ssd, List<IoVm> vmlist, double time){
		List<String> fileNameList = ssd.getFileNameList();
		double wearout = 0;
		double costPerMb = getCostPerMb(ssd);
		
		// If there are no vm files
		if (fileNameList.size() <= 0 ) {
			return 0;
		}
		double writeTot = 0.0;
		// We assume that files' name are the same as VMs' name (ID)
		for (IoVm vm: vmlist) {
			if(fileNameList.contains(vm.getUid())) {
				
				// The amount of written data, in MB
				writeTot = Math.ceil((1 - vm.getIoWorkloadModel().getReadRate(time)) * vm.getIoWorkloadModel().getVolume(time));
				
				// So the wear out cost is
				wearout += writeTot * costPerMb;
			}
			
		}
		return wearout;
	}
	
	/**
	 * Gets storage system wear out cost for migration operation (Storage device machine level)
	 * 
	 * @return cost in dollars
	 */
	@SuppressWarnings("unused")
	private double getHddToSsdMigrationWearOutCost(IoHarddriveStorage src, IoSolidStateStorage dst, IoVm vm) {
		// The amount of data written is the vm image file size
		return vm.getSize() * getCostPerMb(dst);
	}
	
	/**
	 * Gets storage system wear out cost for migration operation (Storage device machine level)
	 * 
	 * @return cost in dollars
	 */
	@SuppressWarnings("unused")
	private double getSsdToHddMigrationWearOutCost() {
		return 0;
	}
	
	/**
	 * Gets the cost per MB written in SSD
	 * @param ssd storage device
	 * @return cost in dollars
	 */
	private double getCostPerMb(IoSolidStateStorage ssd) {
		// The cost per MB written on SSD. 1024 *1024 is for conversion of $/GB to $/MB from 
		// and MaxdataWrite from GB to MB 
		return (ssd.getStorageUnitPrice() * ssd.getCapacity()) / 
							(ssd.getMaxDataWrite() * 1024 * 1024);  
	}
	
	/**
	 * Gets the cost per start-stop cycle 
	 * @param hdd the device
	 * @return the cost
	 */
	private double getCostPerStartStop(IoHarddriveStorage hdd) {
		return (hdd.getStorageUnitPrice() * hdd.getCapacity()) / hdd.getTotalStartStop();
	}

	/**
	 * Gets the wear out of adding a VM storage to a Host
	 * @param host the host where the VM will be added
	 * @param vm the VM to be added
	 * @return the wear out cost
	 */
	public double getVmAddToHostWearOut(IoHost host, IoVm vm) {
		Storage device = IoStorageList.getDeviceContainsVm(host.getStorageDevices(), vm);
		double wearOUtCost = 0.0;
		
		if (device instanceof IoSolidStateStorage){
			wearOUtCost = getCostPerMb((IoSolidStateStorage) device) * vm.getSize();
			return wearOUtCost;
		}else if (device instanceof IoHarddriveStorage) {
			wearOUtCost = getCostPerStartStop((IoHarddriveStorage) device) * 0;
		}
		return 0;
	}
	
	/**
	 * Gets the storage cost using MTTF
	 * @param device the storage device
	 * @return the cost per second
	 */
	private double getCostPerPowerOnSecond(Storage device) {

		if (device instanceof IoSolidStateStorage){
			IoHarddriveStorage hdd = (IoHarddriveStorage)device;
			return hdd.getStorageUnitPrice() / (hdd.getMttf()*60);
		} else if (device instanceof IoSolidStateStorage) {
			IoSolidStateStorage ssd = (IoSolidStateStorage) device;
			return ssd.getStorageUnitPrice() / ssd.getMttf();
		}
		return 0;
	}

}
