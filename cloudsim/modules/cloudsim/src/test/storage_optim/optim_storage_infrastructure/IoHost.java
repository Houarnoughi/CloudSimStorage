package optim_storage_infrastructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.File;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.power.PowerHostUtilizationHistory;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.util.MathUtil;

public class IoHost extends PowerHostUtilizationHistory {
	
	/** The connected storage devices */
	private List<? extends Storage> storageDevices;
	
	/** VMs storage devices Map */ 
	private Map<String, String> vmStorageDeviceMap;
	
	/** The available <StorageDeviceUid, freeSpace> map **/
	private Map<String, Long> avilableStorageMap;
	
	/** The last IO processing time */
	private double lastIoProcessingTime;
	
	public IoHost(int id, 
					  RamProvisioner ramProvisioner, 
					  BwProvisioner bwProvisioner, 
					  long storage,
					  List<? extends Pe> peList, 
					  VmScheduler vmScheduler, 
					  PowerModel powerModel,
					  // Storage parts
					  List<? extends Storage> storageList) {
		super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, powerModel);
		setStorageDevices(storageList);
		assignStorage(storageList);
		setVmStorageDeviceMap(new HashMap<String, String>());
		setLastIoProcessingTime(0);
	}
	
	@Override
	public boolean vmCreate(Vm vm) {
		//if (getStorage() < vm.getSize()) {
				if (!allocateStorageForVm((IoVm)vm)) {	
					Log.printLine("[VmScheduler.vmCreate] Allocation of VM #" + vm.getId() + " to Host #" + getId()
							+ " failed by storage");
					return false;
				}

				if (!getRamProvisioner().allocateRamForVm(vm, vm.getCurrentRequestedRam())) {
					Log.printLine("[VmScheduler.vmCreate] Allocation of VM #" + vm.getId() + " to Host #" + getId()
							+ " failed by RAM");
					return false;
				}

				if (!getBwProvisioner().allocateBwForVm(vm, vm.getCurrentRequestedBw())) {
					Log.printLine("[VmScheduler.vmCreate] Allocation of VM #" + vm.getId() + " to Host #" + getId()
							+ " failed by BW");
					getRamProvisioner().deallocateRamForVm(vm);
					return false;
				}

				if (!getVmScheduler().allocatePesForVm(vm, vm.getCurrentRequestedMips())) {
					Log.printLine("[VmScheduler.vmCreate] Allocation of VM #" + vm.getId() + " to Host #" + getId()
							+ " failed by MIPS");
					getRamProvisioner().deallocateRamForVm(vm);
					getBwProvisioner().deallocateBwForVm(vm);
					return false;
				}
				setStorage(getStorage() - vm.getSize());
				getVmList().add(vm);
				vm.setHost(this);
				
				return true;
	}
	
	@Override
	protected void vmDeallocate(Vm vm) {
		// TODO Auto-generated method stub
		super.vmDeallocate(vm);
		IoVm myVm = (IoVm)vm;
		deallocateStorageForVm(myVm);
	}
	
	@Override
	public void vmDestroyAll() {
		
		// Free all storage devices
		for (Vm vm : getVmList()) {
			IoVm myVm = (IoVm) vm;
			deallocateStorageForVm(myVm);
		}
		super.vmDestroyAll();
	}
	
	public double updateVmsIoProcessing(double currentTime) {
		// TODO: I have to implement a scheduler for VmIoexecution 
		// It will be for each storage device
		double ioTime = 0.0;
		//MarsIoCpuCorrelationModel cpuModel = new MarsIoCpuCorrelationModel();
		
		for (Storage device : getStorageDevices()) {
			double devIoTime = 0.0;
			List<IoVm> vmList = IoStorageList.getVmsInDevice(device, getVmList());
			
			if (vmList != null) {
				
				for (IoVm vm : vmList) {
					
					// If VM is already in datacenter
					if 	(getVmList().contains(vm))
					{
						// Update the IO processing
						double tmpioTime = vm.updateVmIoProcessing(currentTime, 
								device.getMaxIops(), 
								device.getMaxTransferRate());
						devIoTime += tmpioTime;
						
						// Update the CPU Utilization
						//double cpuLoad = cpuModel.getCpuLoad(device, vm, currentTime);
						//double utilizationMips = Math.ceil(cpuLoad * getTotalMips());
						//super.setUtilizationMips(utilizationMips);
					}
				}
			}
			
			if (devIoTime > ioTime) {
				ioTime = devIoTime;
			}
		}
		
		// Update io processing time
		setLastIoProcessingTime(getLastIoProcessingTime() + ioTime);
		return ioTime;
	}
	
	/**
	 * Get the list of attached storage devices
	 * @return
	 */
	public List<? extends Storage>  getStorageDevices() {
		//Log.printLine("GetStorageDevices: "+ storageDevices);
		return storageDevices;
	}
	
	/**
	 * Attach a list of storage devices to the data center
	 * @param storageDevices
	 */
	public void setStorageDevices(List<? extends Storage> storageDevices) {
		this.storageDevices = storageDevices;
		setStorageHostId(storageDevices, getId());
	}
	
	/**
	* Allocate a storage to a VM
	* @param vm the vm that requests storage
	* @return
	*/
	public boolean allocateStorageForVm(IoVm vm) {
		
		if (vm.getStorageDevice() != null){
			//System.out.println("VM #"+vm.getUid()+" in device "+vm.getStorageDevice());
			Storage strg = IoStorageList.getDeviceByUid(getStorageDevices(), vm.getStorageDevice());
			if (strg != null){
				//System.out.println("The found storage device "+strg.getUid());
				return storeVmInDevice(vm, strg);
			}
		}
		
		Storage availableDev = IoStorageList.getSuitableStorage(getStorageDevices(), vm);
		if (availableDev == null) {
			Log.printLine("Host #"+getId()+" no available storage device for vm #"+vm.getId());
				return false;
			} else {
				return storeVmInDevice(vm, availableDev);
			}
	}
	
	/**
	 * Deallocate storage occupied by the VM
	 * @param vm the vm
	 * @return true if storage 
	 */
	public boolean deallocateStorageForVm(IoVm vm){
		
		Storage device = IoStorageList.getDeviceContainsVm(getStorageDevices(), vm);
		
		if (device != null) {
			
			File vmImage = device.deleteFile(vm.getUid());
			
			getVmStorageDeviceMap().remove(vm.getUid());
			
			vm.setTransactionTime(vmImage.getTransactionTime());
			
			// Temporary solution 
			vm.setRemainingVolume(0);
			//Log.printLine("Hamza:VM "+vm.getUid()+" has been deallocated");
			return true;
		}else {
			return false;
		}
	}
	
	/**
	 * Gets the virtual machine storage device
	 * @return the map
	 */
	public Map<String, String> getVmStorageDeviceMap() {
		return vmStorageDeviceMap;
		}
	
	/**
	 * 
	 * @param vmStorageDeviceMap
	 */
	public void setVmStorageDeviceMap(Map<String, String> vmStorageDeviceMap) {
		this.vmStorageDeviceMap = vmStorageDeviceMap;
		}
	
	/**
	 * Get the available free storage space of this host 
	 * @return the available storage available map
	 */
	public Map<String, Long> getAvailableStorageMap() {
		return avilableStorageMap;
	}
	
	/**
	 * Sets the available storage map 
	 * @param availableStorageMap
	 */
	public void setAvailableStorageMap(Map<String, Long> availableStorageMap) {
		this.avilableStorageMap = availableStorageMap;
	}
	/**
	 * Set the host ID for all attached storage devices 
	 * @param storageDevices
	 * @param id
	 */
	private void setStorageHostId(List<? extends Storage> storageDevices, int id) {
		
		if (storageDevices != null) {
			for (Storage device: storageDevices) {
				if (device instanceof IoHarddriveStorage) {
					IoHarddriveStorage tmp = (IoHarddriveStorage)device;
					tmp.setHostId(id);
				} else if (device instanceof IoSolidStateStorage) {
					IoSolidStateStorage tmp =  (IoSolidStateStorage)device;
					tmp.setHostId(id);
				}
			}
		}	
	}

	
	@Override
	public double getUtilizationOfCpu() {
		double utilization = super.getUtilizationOfCpu();
		/*
		double ioCpu = getIoCpuUtilz();
		Log.printLine("Hamza : getUtilizationOfCpu Without "+ utilization);
		Log.printLine("Hamza : getUtilizationOfCpu IO "+ ioCpu);
		/*
		if (utilization+ioCpu > 1 ) {
			utilization = 1;
		} else {
			utilization += ioCpu;
		}
		*/
		return utilization;
	}

	public double getLastIoProcessingTime() {
		return lastIoProcessingTime;
	}

	public void setLastIoProcessingTime(double lastIoProcessingTime) {
		this.lastIoProcessingTime = lastIoProcessingTime;
	}
	
	/**
	 * Gets the host utilization history.
	 * 
	 * @return the host utilization history
	 */
	public double[] getUtilizationHistory() {
		double[] utilizationHistory = new double[PowerVm.HISTORY_LENGTH];
		double hostMips = getTotalMips();
		for (IoVm vm : this.<IoVm> getVmList()) {
			for (int i = 0; i < vm.getUtilizationHistory().size(); i++) {
				utilizationHistory[i] += vm.getUtilizationHistory().get(i) * vm.getMips() / hostMips;
			}
		}
		return MathUtil.trimZeroTail(utilizationHistory);
	}
	
	/**
	 * Assign the storage device list to this host
	 */
	public void assignStorage(List<? extends Storage> storageDevices) {
		int id = 0;
		
		for (Storage device : storageDevices) {
			
			if(device instanceof IoHarddriveStorage) {
				IoHarddriveStorage hdd = (IoHarddriveStorage) device;
				hdd.setHost(this);
				hdd.setId(id);
			}else if (device instanceof IoSolidStateStorage) {
				IoSolidStateStorage ssd = (IoSolidStateStorage) device;
				ssd.setHost(this);
				ssd.setId(id);
			}
			
			id++;
		}
	}
	
	/**
	 * Store the VM image in the storage device
	 */
	
	private boolean storeVmInDevice (IoVm vm, Storage device) {
		
		double transactionTime = 0.0;
		
		try {
			
			File vmImage = new File(vm.getUid(),Math.toIntExact(vm.getSize()));
			transactionTime = device.addFile(vmImage);
			
			getVmStorageDeviceMap().put(vm.getUid(), device.getUid());
			
			vm.setTransactionTime(transactionTime);
			
			// Add the energy to add a vm to the Host
			/*
			EnergyDataCenter dataCenter = (EnergyDataCenter) getDatacenter();
			double relatedEnergy = dataCenter.getStorageEnergyModel().getVmStorageAddToHostEnergy(this, vm);
			dataCenter.setStoragePower(dataCenter.getStoragePower() + relatedEnergy);
			*/
			return true;
			
		} catch(Exception e) {
			
			e.printStackTrace();
			return false;
			
		}
		
	}
}
