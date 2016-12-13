package optim_storage_infrastructure;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;


public class IoStorageEnergyModel {

	public double getDatacenterStorageEnergy(List<IoHost> hostList, double time){
		double energy = 0;
		
		// IMPORTANT TODO: Add idle energy consumption when no VMS stored on storage device
		// FOR DVFS, when no VM stored on device, standby energy consumption, and increment start-stop count for HDD
		// Get all storage devices from HostList
		for (IoHost host: hostList) {
			energy += getHostStorageEnergy(host, time);
		}
		
		return energy;
	}
	
	/**
	 * Get the energy consumed by the hdd (I/O workload execution cost)
	 * @param hdd the hard drive
	 * @return the energy consumed
	 */
	double getEnergyFromHdd(IoHarddriveStorage hdd, List<IoVm> vmlist, double time){
		List<String> fileNameList = hdd.getFileNameList();
		double energy = 0;
		//double energySimple = 0;
		
		if (fileNameList.size() <= 0) {
			energy = getIdleEnergy(hdd, time);
			return energy;
		}
		
		// We assume that files' name are the same as VMs' name (ID)
		for (IoVm vm: vmlist) {
			if(fileNameList.contains(vm.getUid())) {
				
				// IMPORTANT: In real case case, usually we have the same power value for all kinds of IO operations (Operating mode power)
				// So to simplify the energy computation: energy = IOprocessingtime X Operating_Mode_Power 
				energy += vm.getLastIoProcessingTime() * Double.max(hdd.getSeqReadPower(), hdd.getRandReadPower());
				//energy += getSingleHddEnergyCost(vm, hdd);
				//Log.printLine("Hamza: getEnergyFromHdd from time "+vm.getLastIoProcessingTime()+" energy "+energy);
			}
		}
		
		return energy;
	}
	
	/**
	 * Get the energy consumed by the ssd (I/O workload execution cost)
	 * @param hdd the hard drive
	 * @return the energy consumed
	 */
	double getEnergyFromSsd(IoSolidStateStorage ssd, List<IoVm> vmlist, double time){
		List<String> fileNameList = ssd.getFileNameList();
		double energy = 0;
		//double energySimple = 0;
		
		// If there is no VM stored on this hard drive
				if (fileNameList.size() <= 0) {
					energy = getIdleEnergy(ssd, time);
					return energy;
				}
		
		// We assume that files' name are the same as VMs' name (ID)
		for (IoVm vm: vmlist) {
			if(fileNameList.contains(vm.getUid())) {
				
				// IMPORTANT: In real case case, usually we have the same power value for all kinds of IO operations (Operating mode power)
				// So to simplify the energy computation: energy = IOprocessingtime X Operating_Mode_Power 
				energy += vm.getLastIoProcessingTime() * Double.max(ssd.getSeqReadPower(), ssd.getRandReadPower());
				//energy += getSingleSsdEnergyCost(vm, ssd);
			}
			
		}
		//Log.printLine("Hamza: getEnergyFromSsd from "+startTime+" to "+stopTime+" energy "+energy);
		return energy;
	}
	
	double getHddToSSdMigrationEnergy(IoHarddriveStorage src,IoSolidStateStorage dst, IoVm vm){
		double transferTime = vm.getStorageMigrationTime(src, dst);
		
		// to simplify the migration cost we assume that:
		// Vm image file is not fragmented, so it is read and written sequentially
		double energy = transferTime * (src.getSeqReadPower() + dst.getSeqWritePower()); 
		
		return energy;
	}

	public double getHostStorageEnergy(IoHost host, double time) {
		double energy = 0.0;
		for (Storage device: host.getStorageDevices()) {
			if(device instanceof IoHarddriveStorage) {
				energy += getEnergyFromHdd((IoHarddriveStorage)device, host.getVmList(), time);
				
			}else if (device instanceof IoSolidStateStorage) {
				energy += getEnergyFromSsd((IoSolidStateStorage)device, host.getVmList(), time);
				
			}
		}
		return energy;
	}
	
	/**
	 * Get the energy consumed in idle mode
	 * @param device storage device
	 * @param startTime
	 * @param stopTime
	 * @return idle energy
	 */
	private double getIdleEnergy(Storage device, double time) {
		
		// If its HDD
		if(device instanceof IoHarddriveStorage) {
			IoHarddriveStorage hdd = (IoHarddriveStorage) device;
			return hdd.getIdlePower() * time;
		// If its SSD
		} else if (device instanceof IoSolidStateStorage) {
			IoSolidStateStorage ssd  = (IoSolidStateStorage) device;
			return ssd.getIdlePower() * time;
		}
		return 0.0;
	}

	public double getVmStorageAddToHostEnergy(IoHost host, IoVm vm) {
		Storage device = IoStorageList.getDeviceContainsVm(host.getStorageDevices(), vm);
		// If its HDD
		if(device instanceof IoHarddriveStorage) {
			IoHarddriveStorage hdd = (IoHarddriveStorage) device;
			return (vm.getTransactionTime() * hdd.getSeqWritePower());
		// If its SSD
		} else if (device instanceof IoSolidStateStorage) {
			IoSolidStateStorage ssd  = (IoSolidStateStorage) device;
			return (vm.getTransactionTime() * ssd.getSeqWritePower());
		}
		
		return 0.0;
	}

	public double getVmStorageRemoveFromHost(IoHost host, IoVm vm) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/**
	 * Get the execution energy cost from HDD
	 * @param vm
	 * @param device
	 * @return 
	 */
	private double getSingleHddEnergyCost(IoVm vm, IoHarddriveStorage hdd) {
		double egy = 0.0, cpuPower = 0.0;
		int megaBytesToBytes = 1024 *1024;
		double bytesToMegaBytes = 0.000000954;
		IoWorkloadModel model = vm.getIoWorkloadModel();
		MarsIoCpuCorrelationModel marsModel = new MarsIoCpuCorrelationModel();
		
		// First: we compute the total number of IOs based on IO size and total volume */
		int ioSize = model.getIoSize(CloudSim.clock()); // In bytes
		double volume = model.getVolume(CloudSim.clock());
		int ioNum = (int)Math.ceil((volume * megaBytesToBytes)/ioSize);
		
		// Second: we compute the number of IOs for each type (seq and rnd)*/
		int rndIoNum = (int)Math.ceil(ioNum * model.getRandomRate(CloudSim.clock())/8);
		int seqIoNum = ioNum - rndIoNum;
		
		// Third: we compute the time to execute IOs with full device performances */
		double rndIoTime = (rndIoNum / hdd.getMaxIops());
		double seqIoTime = (seqIoNum*(ioSize/bytesToMegaBytes)) / hdd.getMaxTransferRate();
		egy = rndIoTime * Math.max(hdd.getRandReadPower(), hdd.getRandWritePower()) +
				seqIoTime * Math.max(hdd.getSeqReadPower(), hdd.getSeqWritePower());
		
		// Fourth: get the energy from using the CPU usage */
		double utilization = marsModel.getCpuLoad(hdd, vm, CloudSim.clock());
		IoHost egyHost = hdd.getHost();
		if (egyHost != null) {
			cpuPower = egyHost.getPowerModel().getPower(utilization);
		}
		
		// Update the energy */
		egy += (rndIoTime + seqIoTime) * cpuPower;
		
		/* Fifth: we compute the energy cost */
		//egyExeHddCost = egy * getEnergyPrice();
		Log.printLine("Hamza getSingleHddEnergyCost HDD for vm "+vm.getId()+" is : "+egy);
		return egy;
	}
	
	/**
	 * Get the execution energy cost from SSD
	 * @param vm
	 * @param device
	 * @return 
	 */
	private double getSingleSsdEnergyCost(IoVm vm, IoSolidStateStorage ssd) {
		double egy = 0.0, cpuPower = 0.0; 
		int megaBytesToBytes = 1024 * 1024;
		double bytesToMegaBytes = 0.000000954;
		IoWorkloadModel model = vm.getIoWorkloadModel();
		MarsIoCpuCorrelationModel marsModel = new MarsIoCpuCorrelationModel();
		
		/* First: we compute the total number of IOs based on IO size and total volume */
		int ioSize = model.getIoSize(CloudSim.clock()); // In bytes
		double volume = model.getVolume(CloudSim.clock());
		int ioNum = (int)Math.ceil((volume * megaBytesToBytes)/ioSize);
		
		/* Second: we compute the number of IOs for each type (seq and rnd)*/
		int rndIoNum = (int)Math.ceil(ioNum * model.getRandomRate(CloudSim.clock()));
		int seqIoNum = 1 - rndIoNum;
	
		/* Third: we compute the time to execute IOs with full device performances */
		double rndIoTime = (rndIoNum / ssd.getMaxIops());
		double seqIoTime = (seqIoNum*(ioSize/bytesToMegaBytes)) / ssd.getMaxTransferRate();
		egy = rndIoTime * Math.max(ssd.getRandReadPower(), ssd.getRandWritePower()) +
				seqIoTime * Math.max(ssd.getSeqReadPower(), ssd.getSeqWritePower());
		
		/* Fourth: get the energy from using the CPU usage */
		double utilization = marsModel.getCpuLoad(ssd, vm, CloudSim.clock());
		IoHost egyHost = ssd.getHost();
		//double cpuPower = egyHost.getPowerModel().getPower(utilization);
		if (egyHost != null) {
			cpuPower = egyHost.getPowerModel().getPower(utilization);
		}
		
		/* Update the energy */
		egy += Math.abs((rndIoTime + seqIoTime) * cpuPower);
		
		/* Fifth: we compute the energy cost */
		// egyExeHddCost = egy * getEnergyPrice();
		Log.printLine("Hamza getSingleSsdEnergyCost SSD for vm "+vm.getId()+" is : "+egy);
		return egy;
	}

}
