package optim_storage_infrastructure;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;


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
		//List<String> fileNameList = hdd.getFileNameList();
		double wearout = 0;
		for (IoVm vm: vmlist) {
			double tmpWearout = getExeHddWearOutCost(vm, hdd);
			Log.printLine("Wear out cost Host #"+vm.getHost().getId()+" VM #"+vm.getId()+" Device #"+hdd.getUid()+" Cost "+tmpWearout);
			wearout += tmpWearout;
		}
		return wearout;
	}
	
	/**
	 * Gets storage system wear out cost (Storage device machine level)
	 * 
	 * @return cost in dollars
	 */
	private double getWearOutCostFromSsd(IoSolidStateStorage ssd, List<IoVm> vmlist, double time){
		double wearout = 0;
		for (IoVm vm: vmlist) {
			wearout += getExeSsdWearOutCost(vm, ssd);
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
	private double getCostPerMb(Storage device) {
		// The cost per MB written on SSD. 1024 *1024 is for conversion of $/GB to $/MB from 
		// and MaxdataWrite from GB to MB 
		if(device instanceof IoHarddriveStorage) {
			IoHarddriveStorage hdd = (IoHarddriveStorage)device;
			
			return (hdd.getStorageUnitPrice() * hdd.getCapacity()) / 
					(hdd.getMaxDataWrite() * 1024 * 1024);
		} else if (device instanceof IoSolidStateStorage) {
			IoSolidStateStorage ssd = (IoSolidStateStorage)device;
			return (ssd.getStorageUnitPrice() * ssd.getCapacity()) / 
					(ssd.getMaxDataWrite() * 1024 * 1024); 
		} else {
			return 0.0;
		}
		
		  
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
	/*
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
	*/
	
	/**
	 * Get the execution Wear Out Cost from HDD
	 * @param vm the virtual machine
	 * @param device HDD
	 * @return
	 */
	private double getExeHddWearOutCost(IoVm vm, IoHarddriveStorage hdd) {
		double exeWOCost = 0.0;
		IoWorkloadModel model = vm.getIoWorkloadModel();
		double schedulingTime = vm.getSchedulingInterval();
		double currentTime  = CloudSim.clock();
		
		/* First: we calculate the total volume of data written */
		Double sched = new Double(schedulingTime);
		int ioNum = model.getArrivalRate(currentTime) * sched.intValue();
		double writeRate = (1 - model.getReadRate(CloudSim.clock()));
		int writeIos = (int)Math.ceil(ioNum*writeRate);
		double writeVolume = writeIos * model.getIoSize(currentTime);
		
		/* Second we calculate the cost of writing one MB */
		double costPerMb = ((hdd.getStorageUnitPrice()/1000) * hdd.getCapacity()) 
							/ hdd.getMaxDataWrite();
		
		//Log.printLine(" Unit price "+hdd.getStorageUnitPrice()+" capacity  "+hdd.getCapacity()+ "getMaxDataWrite"+hdd.getMaxDataWrite());
		/* Finally: we calculate the cost of writing this volume */
		exeWOCost = writeVolume * costPerMb;
		
		return exeWOCost;
	}
	
	/**
	 * Get the execution Wear Out Cost from SSD
	 * @param vm the virtual machine
	 * @param device SSD
	 * @return
	 */
	private double getExeSsdWearOutCost(IoVm vm, IoSolidStateStorage ssd) {
		double exeWOCost = 0.0;
		IoWorkloadModel model = vm.getIoWorkloadModel();
		double schedulingTime = vm.getSchedulingInterval();
		double currentTime  = CloudSim.clock();
		
		/* First: we calculate the total volume of data written */
		Double sched = new Double(schedulingTime);
		int ioNum = model.getArrivalRate(currentTime) * sched.intValue();
		double writeRate = (1 - model.getReadRate(CloudSim.clock()));
		int writeIos = (int)Math.ceil(ioNum*writeRate);
		double writeVolume = writeIos * model.getIoSize(currentTime);
		
		/* Second we calculate the cost of writing one MB */
		double costPerMb = ((ssd.getStorageUnitPrice()/1000) * ssd.getCapacity()) 
							/ ssd.getMaxDataWrite();
		
		//Log.printLine(" Unit price "+hdd.getStorageUnitPrice()+" capacity  "+hdd.getCapacity()+ "getMaxDataWrite"+hdd.getMaxDataWrite());
		/* Finally: we calculate the cost of writing this volume */
		exeWOCost = writeVolume * costPerMb;
		
		return exeWOCost;
	}

}
