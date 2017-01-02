package optim_storage_infrastructure;

import java.util.List;

import org.cloudbus.cloudsim.Storage;

/**
 * This is an implementation of proportional SLA calculation ()
 * @author hamza
 *
 */
public class IoStorageSlaModel {
	private double bill;

	public double getDatacenterStorageSla(List<IoHost> hosts, double time) {
		double all_sla = 0.0;
		for (IoHost host: hosts){
			all_sla += getHostStorageSla(host, time);
		}
		return all_sla/hosts.size();
	}

	public double getHostStorageSla(IoHost host, double time) {
		double all_sla = 0.0;
		for (Storage device: host.getStorageDevices()) {
			all_sla += getDeviceStorageSla(device, host.getVmList(), time);
		}
		return all_sla/host.getStorageDevices().size();
	}

	public double getDeviceStorageSla(Storage device, List<IoVm> vmlist, double time) {
		List<String> fileNameList = device.getFileNameList();
		double all_iops = 0.0;
		double prop = 0.0;
		
		if (fileNameList.size() <= 0) {
			return 0;
		}
		
		for (IoVm vm: vmlist) {
			if (fileNameList.contains(vm.getUid())) {
			// Hamza: here the time must to be calculated
				all_iops += vm.getIoWorkloadModel().getArrivalRate(time);
			}
		}
		prop = device.getMaxIops()/Math.ceil(all_iops);
		return (prop<1 && prop>0)?prop:0;
	}

	public double getBill() {
		return bill;
	}

	public void setBill(double bill) {
		this.bill = bill;
	}
}
