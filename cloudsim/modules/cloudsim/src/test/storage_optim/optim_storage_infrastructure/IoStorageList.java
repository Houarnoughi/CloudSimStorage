package optim_storage_infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;

public class IoStorageList {
	
	/**
	 * Get a storage device giving it UID
	 * @param storageList the list of storage devices
	 * @param uid uid of storage device
	 * @return a storage device
	 */
	public static <T extends Storage> Storage getDeviceByUid(List<T> storageList, String uid){
		for (Storage device: storageList) {
			if (device.getUid() == uid) {
				return device;
			}
		}
		return null;
	}
	
	/**
	 * Gets a Map containing storage devices and corresponding free space   
	 * @param storageList
	 * @return storage map
	 */
	public static <T extends Storage> Map<String, Double> getAvailableStorageMap(List<T> storageList){
		Map<String, Double> available = new HashMap<String, Double>();
		for (Storage device: storageList) {
			available.put(device.getUid(), device.getAvailableSpace());
		}
		return available;
	}
	
	/**
	 * Gets a suitable storage device for a VM considering its size
	 * @param storageList storage device list
	 * @param vm VM
	 * @return storage device
	 */
	public static <T extends Storage> Storage getSuitableStorage(List<T> storageList, IoVm vm) {
		IoVm tmpVm = vm;
		for (Storage device: storageList) {
			if (device.getAvailableSpace() >= vm.getSize() && device.getMaxIops() >= tmpVm.getRequestedIops()) {
				return device;
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the storage device containing a VM
	 * @param storageList storage device list
	 * @param vm vm
	 * @return storage device
	 */
	public static <T extends Storage> Storage getDeviceContainsVm(List<T> storageList, IoVm vm) {
		String vmUid = vm.getUid();
		for (Storage device: storageList) {
			List<String> fileNameList = device.getFileNameList();
			if (fileNameList.contains(vmUid)) {
				return device;
			}
		}
		Log.printLine("[MyStorageList.getDeviceContainsVm]: there is no device containing the vm "+vm.getUid());
		return null;
	}
	
	/**
	 * Gets VMs stored in this storage device
	 * @param device storage device
	 * @param vmList the list of vms
	 * @return list
	 */
	public static List<IoVm> getVmsInDevice(Storage device, List<? extends Vm> vmList) {
		List<IoVm> list = new ArrayList<>();
		List<String> fileNameList = device.getFileNameList();
		for (Vm vm : vmList) {
			if (fileNameList.contains(vm.getUid())) {
				list.add((IoVm)vm);
			}
		}
		return list;
	}

}
