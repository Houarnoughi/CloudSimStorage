package optim_storage_allocation_policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;

import optim_storage_infrastructure.BcomStorageCostModel;
import optim_storage_infrastructure.IoHarddriveStorage;
import optim_storage_infrastructure.IoHost;
import optim_storage_infrastructure.IoSolidStateStorage;
import optim_storage_infrastructure.IoVm;

public class IoSolutionsEnumeration {
	/** The connected storage devices */
	private List<? extends Storage> storageDevices;
	private List<? extends Host> pmList;
	private List<? extends Vm> vmList;
	private BcomStorageCostModel costModel;
	
	int[] plan_array;
	int[] min_plan_placement;
	int nbVm;
	int nbPm;
	int nbSd;
	double minCost = Double.MAX_VALUE;

	public IoSolutionsEnumeration(List<? extends Vm> vmList, List<? extends Host> pmList) {
		double egyPriceKwh = 0.0887; 			// 0.0887 euros / kWh
		double egyPrice =  egyPriceKwh/3600000; // Price per WattSec (Joule)
		double CloudServPrice = 0.5;			// Cloud Service Price / hour
		double bill = CloudServPrice * 30 * 24; // Bill amount / month
		
		this.setPmList(pmList);
		this.setVmList(vmList);
		this.setStorageDevices(getAllStorageDevices());
		
		this.nbVm = vmList.size();
		this.nbPm = pmList.size();
		this.nbSd = getStorageDevices().size();
		plan_array = new int[nbVm];
		min_plan_placement = new int[nbVm];
		costModel = new BcomStorageCostModel(0.0, egyPrice, bill);
	}

	public List<Map<String, Object>> getMinPlacementPlan(int nbVm) {
		List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
		
		/* The trivial case => No more VM to place */
		if (nbVm <= 0) {
			double total_cost = 0.0;
			if (isValid(plan_array)) {
				for (int i=0; i<plan_array.length; i++) {
					/* Calculate the total cost of these placement plan*/
					IoVm vm = (IoVm) getVmList().get(i);
					Storage dev = getAllStorageDevices().get(plan_array[i]);
					total_cost += costModel.getVmStorageCost(vm, dev);
					
					/* Is it the min cost ?*/
					if (total_cost<minCost) {
						this.minCost = total_cost;
						this.min_plan_placement = plan_array;
					}
				}
			}
			migrationMap = constructPlacementPlanFromArray(plan_array);
		/* If there is more VM to place*/
		} else {
			for(int j=0; j<nbSd; j++){
				plan_array[nbVm - 1] = j;
				getMinPlacementPlan(nbVm - 1);
			}
		}
	return migrationMap;
	}
	
	/**
	 * Check if a solution is valid
	 * @return
	 */
	private boolean isValid(int plan_array[]) {
		
		/* Check the available capacity and IOPS */
		for (int i = 0; i < nbSd; i++){
			double available_space = getAllStorageDevices().get(i).getAvailableSpace();
			int available_iops = getAllStorageDevices().get(i).getMaxIops();
			
			for (int j = 0; j < plan_array.length; j++) {
				if (plan_array[j] == i) {
					IoVm vm = (IoVm) getVmList().get(j);
					
					available_space = available_space - vm.getSize();
					available_iops = available_iops - vm.getRequestedIops();
				}
			}
			
			if(available_space <= 0 || available_iops <= 0){
				return false;
			}
		}
		
		/* Check the available CPU and RAM */
		
		for (IoHost pm : this.<IoHost> getPmList()) {
			double pm_avalaible_cpu = pm.getAvailableMips();
			int pm_avalaible_ram = pm.getRam();
			
			for (int i = 0; i < nbSd; i++){
				Storage dev = getAllStorageDevices().get(i);
				IoHost pm_of_dev = getHotOfStorageDeveice(dev);
				
				if (pm.getId() == pm_of_dev.getId()) {
					
					for (int j = 0; j < plan_array.length; j++){
						if (plan_array[j] == i) {
							IoVm vm = (IoVm) getVmList().get(j);
							
							pm_avalaible_cpu = pm_avalaible_cpu - vm.getCurrentRequestedTotalMips();
							pm_avalaible_ram = pm_avalaible_ram - vm.getCurrentRequestedRam();
						}
					}
				}
			}
			
			if (pm_avalaible_cpu <= 0 || pm_avalaible_ram <= 0) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Construct the placement plan from the array plan
	 */
	public List<Map<String, Object>> constructPlacementPlanFromArray (int plan_array[]) {
		List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
		for (int i = 0; i<plan_array.length; i++) {
			IoVm vm = (IoVm) getVmList().get(i);
			int dev_index = plan_array[i];
			
			Storage dev = getAllStorageDevices().get(dev_index);
			IoHost pm = getHotOfStorageDeveice(dev);
			vm.setStorageDevice(dev.getUid());
			pm.vmCreate(vm);
			Log.printLine("VM #" + vm.getId() + " allocated to host #" + pm.getId());
			Map<String, Object> migrate = new HashMap<String, Object>();
			migrate.put("vm", vm);
			migrate.put("host", pm);
			migrationMap.add(migrate);
		}
		
		return migrationMap;
	}
	
	/**
	 * Get the list of all storage devices given the list of PMs
	 * @return
	 */
	public List<? extends Storage> getAllStorageDevices() {
		List<Storage> storageList = new ArrayList<Storage>();
		for (IoHost pm : this.<IoHost> getPmList()){
				storageList.addAll(pm.getStorageDevices());
		}
		
		return storageList;
	}

	@SuppressWarnings("unchecked")
	public <T extends Host> List<T> getPmList() {
		return (List<T>) pmList;
	}

	protected void setPmList(List<? extends Host> pmList) {
		this.pmList = pmList;
	}

	public List<? extends Vm> getVmList() {
		return vmList;
	}

	public void setVmList(List<? extends Vm> vmList) {
		this.vmList = vmList;
	}

	public List<? extends Storage> getStorageDevices() {
		return storageDevices;
	}

	public void setStorageDevices(List<? extends Storage> storageDevices) {
		this.storageDevices = storageDevices;
	}
	
	private IoHost getHotOfStorageDeveice(Storage dev) {
		
		/* Tmp solution */
		if (dev instanceof IoSolidStateStorage){
			IoSolidStateStorage ssd = (IoSolidStateStorage) dev;
			return ssd.getHost(); 
		} else if (dev instanceof IoHarddriveStorage){
			IoHarddriveStorage hdd = (IoHarddriveStorage) dev;
			return hdd.getHost();
		}else{
			return null;
		}
		
	}
}
