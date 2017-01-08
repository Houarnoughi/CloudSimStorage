package optim_storage_infrastructure;

import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * This class implements the first version of VM storage cost model.
 * For more information, please refer to the paper : 
 * "A Cost Model for Virtual Machine Storage in Cloud IaaS Context". 	
 * Hamza Ouarnoughi, Jalil Boukhobza, Frank Singhoff, Stéphane Rubini
 * 
 * @author hamza
 *
 */

public class BcomStorageCostModel implements IoStorageCostModel {
	
	/* The constant costs (Non-recurring cost) */
	private double nonRecurrStorageCost;
	
	/* The unitary price of the elasticity (for energy cost) */
	private double energyPrice;
	
	/* The bill amount (for SLA cost)*/
	private double billAmount;
	
	public BcomStorageCostModel(double costConst, double energyPrice, double bill) {
		setNonRecurrStorageCost(costConst);
		setEnergyPrice(energyPrice);
		setBillAmount(bill);
	}

	/**
	 * The cost of storing this Virtual machine in this storage device
	 */
	@Override
	public <VM extends Vm, SD extends Storage> 
	double getVmStorageCost(VM vm, SD device) {
		double totalCost = 0.0;
		IoVm egyVm = (IoVm) vm; 
		// The Cost of storing a VM on a given storage device is the some of :
		// Constant Costs, I/O workload Execution Cost, and eventual migration cost
		totalCost = getNonRecurrStorageCost() + 
				workloadExecutionCost(egyVm, device) + 
				vmStorageMigration(egyVm, device);
		return totalCost;
	}
	
/************* First Level Costs (Exe + non-rec + Mig) *******************************/
	
	/**
	 * The cost of executing the VM I/O workload on this device 
	 * @param vm the virtual machine
	 * @param device the storage device
	 * @return
	 */
	private double workloadExecutionCost(IoVm vm, Storage device) {
		double costExe = 0.0;
		
		// The execution cost: TCO + Penalty 
		costExe = getExeTcoCost(vm, device) + getExePenaltyCost(vm, device);
		return costExe;
	}
	
	/**
	 * The cost of migrating the virtual machine to the storage device
	 * @param vm the virtual machine
	 * @param device the storage device
	 * @return
	 */
	private double vmStorageMigration(IoVm vm, Storage device) {
		double costMig = 0.0;
		
		if (vm.getHost() != null) {
			IoDataCenter datacenter = (IoDataCenter) vm.getHost().getDatacenter();
			Storage source = IoStorageList.getDeviceContainsVm(datacenter.getAllStorageDevices(), vm);
			// If the VM is in the same device
			if ( device == source ) {
				costMig = 0.0;
			} else {
				// The penalty cost: TCO + Penalty 
				costMig = getMigTcoCost(vm, device);// + getMigPenaltyCost(vm, device);
			}
			
		}
		return costMig;
	}
	
	/**
	 * Get the Non-Recurring cost (constant cost)  
	 * @return the non recurring cost
	 */
	public double getNonRecurrStorageCost() {
		return nonRecurrStorageCost;
	}
	
	/**
	 * Set the Non-Recurring cost (constant cost)
	 * @param nonRecurrStorageCost
	 */
	public void setNonRecurrStorageCost(double nonRecurrStorageCost) {
		this.nonRecurrStorageCost = nonRecurrStorageCost;
	}

/****************** Second Level Costs (TCO + Penalty) ********************************/

	/**
	 * Get the VM I/O workload execution TCO cost
	 * @param vm the virtual machine
	 * @param device the storage device
	 * @return execution tco cost
	 */
	private double getExeTcoCost(IoVm vm, Storage device) {
		double exeTcoCost = 0.0, egyCost = 0.0, wearCost = 0.0;
		
		if(device instanceof IoHarddriveStorage) {
			egyCost = getExecHddEnergyCost(vm, (IoHarddriveStorage) device);
			wearCost = getExeHddWearOutCost(vm, (IoHarddriveStorage) device);
		}else if (device instanceof IoSolidStateStorage) {
			egyCost = getExecSsdEnergyCost(vm, (IoSolidStateStorage) device);
			wearCost = getExeSsdWearOutCost(vm, (IoSolidStateStorage) device);
		}
		
		// The Exe TCO: Energy + Wear-Out
		exeTcoCost = egyCost + wearCost;
		return exeTcoCost;
	}
	
	/**
	 * Get the VM I/O workload execution penalty cost
	 * @param vm the virtual machine
	 * @param device the storage device
	 * @return execution penalty cost
	 */
	private double getExePenaltyCost(IoVm vm, Storage device) {
		double exePenCost = 0.0, reqPerfs = 0.0, offPerfs = 0.0;
		
		/* The penalty is calculated from the ratio requestedPerfs/offeredPerfs */
		reqPerfs = vm.getRequestedIops();
		
		if(device instanceof IoHarddriveStorage) {
			IoHarddriveStorage hdd = (IoHarddriveStorage) device;
			offPerfs = hdd.getAvgIops();
		}else if (device instanceof IoSolidStateStorage) {
			IoSolidStateStorage ssd = (IoSolidStateStorage) device;
			offPerfs = ssd.getAvgIops();
		}
		
		if ( offPerfs < reqPerfs ) {
			exePenCost = (1- (offPerfs/reqPerfs)) * getBillAmount();
		} else {
			exePenCost = 0.0;
		}
		
		//System.out.println("B-COM Storage Cost Model : "+exePenCost);
		return exePenCost; 
	}
	
	/**
	 * Get the VM migration TCO cost
	 * @param vm the virtual machine
	 * @param device the storage device
	 * @return migration tco cost
	 */
	private double getMigTcoCost(IoVm vm, Storage device) {
		double migTcoCost = 0.0, egyCost = 0.0, wearoutCost = 0.0;
		
		if(device instanceof IoHarddriveStorage) {
			egyCost = getMigHddEnergyCost(vm, (IoHarddriveStorage) device);
			wearoutCost = getHddMigWearOutCost(vm, (IoHarddriveStorage) device);
		}else if (device instanceof IoSolidStateStorage) {
			egyCost = getMigSsdEnergyCost(vm, (IoSolidStateStorage) device);
			wearoutCost = getSsdMigWearOutCost(vm, (IoSolidStateStorage) device);
		}
		
		// The Exe TCO: Energy + Wear-Out
		migTcoCost = egyCost + wearoutCost;
		
		return migTcoCost;
	}
	
	/**
	 * Get the VM migration penalty cost (Delay dependent)
	 * @param vm the virtual machine
	 * @param device the storage device
	 * @return execution penalty cost
	 */
	// private double getMigPenaltyCost(IoVm vm, Storage device) {
		// double exePenCost = 0.0;
		// double secPerMonth = 30 * 24 * 60;
		
		// IoDataCenter datacenter = (IoDataCenter) vm.getHost().getDatacenter();
		// Storage source = IoStorageList.getDeviceContainsVm(datacenter.getAllStorageDevices(), vm);
		
		/* Suppose that are no IOs during migration => (offPerfs = 0 during migTime) */
		
		/* First: we calculate the migration time */
		// double timeMig = vm.getSize() / 
			//				Math.min(device.getMaxTransferRate(), 
				//					source.getMaxTransferRate());
		
		/* The ratio of migrationTime(s)/Month's Bill(s) */
		// exePenCost = ((timeMig * 100)/secPerMonth) * getBillAmount();
		
		// return exePenCost; 
	//}

/****************** Third Level Costs (Energy + WearOut) ********************************/
	
	/**
	 * Get the execution energy cost from HDD
	 * @param vm
	 * @param device
	 * @return 
	 */
	private double getExecHddEnergyCost(IoVm vm, IoHarddriveStorage hdd) {
		double egyExeHddCost = 0.0, egy = 0.0, cpuPower = 0.0;
		// int megaBytesToBytes = 1024 *1024;
		// IoWorkloadModel model = vm.getIoWorkloadModel();
		MarsIoCpuCorrelationModel marsModel = new MarsIoCpuCorrelationModel();
		
		/* First: we compute the total number of IOs based on IO size and total volume */
		/*
		int ioSize = model.getIoSize(CloudSim.clock()); // In bytes
		double volume = model.getVolume(CloudSim.clock());
		int ioNum = (int)Math.ceil((volume * megaBytesToBytes)/ioSize);
		*/
		/* Second: we compute the number of IOs for each type (seq and rnd)*/
		/*
		int rndIoNum = (int)Math.ceil(ioNum * model.getRandomRate(CloudSim.clock()));
		int seqIoNum = ioNum - rndIoNum;
		*/
		
		/* Third: we compute the time to execute IOs with full device performances */
		/*
		double rndIoTime = (rndIoNum / hdd.getMaxIops());
		double seqIoTime = (seqIoNum*ioSize) / hdd.getMaxTransferRate();
		egy = rndIoTime * Math.max(hdd.getRandReadPower(), hdd.getRandWritePower()) +
				seqIoTime * Math.max(hdd.getSeqReadPower(), hdd.getSeqWritePower());
		*/
		egy = vm.getLastIoProcessingTime() * Double.max(hdd.getSeqReadPower(), hdd.getRandReadPower());
		
		/* Fourth: get the energy from using the CPU usage */
		double utilization = marsModel.getCpuLoad(hdd, vm, CloudSim.clock());
		IoHost egyHost = hdd.getHost();
		if (egyHost != null) {
			cpuPower = egyHost.getPowerModel().getPower(utilization);
		}
		
		/* Update the energy */
		// egy += (rndIoTime + seqIoTime) * cpuPower;
		egy += vm.getLastIoProcessingTime() * cpuPower;
		
		/* Fifth: we compute the energy cost */
		egyExeHddCost = egy * getEnergyPrice(); 
		return egyExeHddCost;
	}
	
	/**
	 * Get the execution energy cost from SSD
	 * @param vm
	 * @param device
	 * @return 
	 */
	private double getExecSsdEnergyCost(IoVm vm, IoSolidStateStorage ssd) {
		double egyExeHddCost = 0.0, egy = 0.0, cpuPower = 0.0; 
		// int megaBytesToBytes = 1048576;
		// IoWorkloadModel model = vm.getIoWorkloadModel();
		MarsIoCpuCorrelationModel marsModel = new MarsIoCpuCorrelationModel();
		
		/* First: we compute the total number of IOs based on IO size and total volume */
		/*
		int ioSize = model.getIoSize(CloudSim.clock()); // In bytes
		double volume = model.getVolume(CloudSim.clock());
		int ioNum = (int)Math.ceil((volume * megaBytesToBytes)/ioSize);
		*/
		
		/* Second: we compute the number of IOs for each type (seq and rnd)*/
		/*
		int rndIoNum = (int)Math.ceil(ioNum * model.getRandomRate(CloudSim.clock()));
		int seqIoNum = ioNum - rndIoNum;
		*/
		
		/* Third: we compute the time to execute IOs with full device performances */
		/*
		double rndIoTime = (rndIoNum / ssd.getMaxIops());
		double seqIoTime = (seqIoNum*ioSize) / ssd.getMaxTransferRate();
		egy = rndIoTime * Math.max(ssd.getRandReadPower(), ssd.getRandWritePower()) +
				seqIoTime * Math.max(ssd.getSeqReadPower(), ssd.getSeqWritePower());
		*/
		egy = vm.getLastIoProcessingTime() * Double.max(ssd.getSeqReadPower(), ssd.getRandReadPower());
		
		/* Fourth: get the energy from using the CPU usage */
		double utilization = marsModel.getCpuLoad(ssd, vm, CloudSim.clock());
		IoHost egyHost = ssd.getHost();
		//double cpuPower = egyHost.getPowerModel().getPower(utilization);
		if (egyHost != null) {
			cpuPower = egyHost.getPowerModel().getPower(utilization);
		}
		
		/* Update the energy */
		egy += vm.getLastIoProcessingTime() * cpuPower;
		
		/* Fifth: we compute the energy cost */
		egyExeHddCost = egy * getEnergyPrice(); 
		return egyExeHddCost;
	}
	
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
		/*The Io Size is in KB. We have to convert it to MB*/
		double writeVolume = (writeIos * model.getIoSize(currentTime))/1024;
		
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
	
	/**
	 * Get the migration energy cost from HDD
	 * @param vm
	 * @param device
	 * @return 
	 */
	private double getMigHddEnergyCost(IoVm vm, IoHarddriveStorage hdd) {
		double egyMigHddCost = 0.0, readTime = 0.0, readPow = 0.0, writeTime = 0.0, writePow = 0.0;
		IoDataCenter datacenter = (IoDataCenter) vm.getHost().getDatacenter();
		Storage source = IoStorageList.getDeviceContainsVm(datacenter.getAllStorageDevices(), vm);
		
		if (source instanceof IoHarddriveStorage) {
			IoHarddriveStorage src = (IoHarddriveStorage) source;
			readPow = src.getSeqReadPower();
		} else if (source instanceof IoSolidStateStorage) {
			IoSolidStateStorage src = (IoSolidStateStorage) source;
			readPow = src.getSeqReadPower();
		}
		
		/* First: we calculate the time to copy VM disk image  
		 * Note that the copy is supposed to be sequential
		 * Sequential read from the source device, then sequential write to the destination
		 * * */
		if (source != null) {
			readTime = vm.getSize() / source.getMaxTransferRate();
		}
		writeTime = vm.getSize() / hdd.getMaxTransferRate();
		writePow = hdd.getSeqWritePower();
		
		/* Second: we calculate the amount of consumed energy */
		double totalEgy = (readTime * readPow) + (writeTime * writePow); 
		
		/* Finally: we calculate the migration energy cost */
		egyMigHddCost = totalEgy * getEnergyPrice();
		
		return egyMigHddCost;
	}
	
	/**
	 * Get the migration energy cost from SSD
	 * @param vm
	 * @param device
	 * @return 
	 */
	private double getMigSsdEnergyCost(IoVm vm, IoSolidStateStorage ssd) {
		double egyMigHddCost = 0.0, readTime = 0.0, readPow = 0.0, writeTime = 0.0, writePow = 0.0;
		IoDataCenter datacenter = (IoDataCenter) vm.getHost().getDatacenter();
		Storage source = IoStorageList.getDeviceContainsVm(datacenter.getAllStorageDevices(), vm);
		
		if (source instanceof IoHarddriveStorage) {
			IoHarddriveStorage src = (IoHarddriveStorage) source;
			readPow = src.getSeqReadPower();
		} else if (source instanceof IoSolidStateStorage) {
			IoSolidStateStorage src = (IoSolidStateStorage) source;
			readPow = src.getSeqReadPower();
		}
		
		/* First: we calculate the time to copy VM disk image  
		 * Note that the copy is supposed to be sequential
		 * Sequential read from the source device, then sequential write to the destination
		 * * */
		if (source != null) {
			readTime = vm.getSize() / source.getMaxTransferRate();
		}
		writeTime = vm.getSize() / ssd.getMaxTransferRate();
		writePow = ssd.getSeqWritePower();
		
		/* Second: we calculate the amount of consumed energy */
		double totalEgy = (readTime * readPow) + (writeTime * writePow); 
		
		/* Finally: we calculate the migration energy cost */
		egyMigHddCost = totalEgy * getEnergyPrice();
		
		return egyMigHddCost;
	}
	
	/**
	 * Get the migration Wear Out Cost
	 * @param vm the virtual machine
	 * @param device storage device
	 * @return
	 */
	private double getHddMigWearOutCost(IoVm vm, IoHarddriveStorage hdd) {
		double wearoutMigCost = 0.0;
		
		/* First: calculate the total amount of data to be written */
		double totalData = vm.getSize();
		
		/* Second: calculate the cost per MB written */
		double costPerMb = (hdd.getStorageUnitPrice() * hdd.getCapacity()) 
							/ hdd.getMaxDataWrite();
		
		/* Finally: the migration wear-out cost */
		wearoutMigCost = totalData * costPerMb;
		return wearoutMigCost;
	}
	
	/**
	 * Get the migration Wear Out Cost
	 * @param vm the virtual machine
	 * @param device storage device
	 * @return
	 */
	private double getSsdMigWearOutCost(IoVm vm, IoSolidStateStorage ssd) {
		double wearoutMigCost = 0.0;
		
		/* First: calculate the total amount of data to be written */
		double totalData = vm.getSize();
		
		/* Second: calculate the cost per MB written */
		double costPerMb = (ssd.getStorageUnitPrice() * ssd.getCapacity()) 
							/ ssd.getMaxDataWrite();
		
		/* Finally: the migration wear-out cost */
		wearoutMigCost = totalData * costPerMb;
		return wearoutMigCost;
	}

	public double getEnergyPrice() {
		return energyPrice;
	}

	public void setEnergyPrice(double energyPrice) {
		this.energyPrice = energyPrice;
	}

	public double getBillAmount() {
		return billAmount;
	}

	public void setBillAmount(double billAmount) {
		this.billAmount = billAmount;
	}

}
