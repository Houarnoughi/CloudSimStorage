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
import optim_storage_infrastructure.IoDataCenter;
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
		/****** Initialization *******/
		// cost model parameters
		double egyPrice =  0;		// Price per WattSec (Joule)
		double bill = 0;			// Bill amount / month
		if (!pmList.isEmpty())
		{
			IoDataCenter dc = (IoDataCenter) pmList.get(0).getDatacenter();
			egyPrice = dc.getCostPerWattSec();
			bill = dc.getBill();
		}
		
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

	public void getMinPlacementPlan(int nb_Vm) {
		
		/* The trivial case => No more VM to place */
		if (nb_Vm <= 0) {
			double total_cost = 0.0;
			if (isValid(plan_array)) {
				for (int i=0; i<this.nbVm; i++) {
					/* Calculate the total cost of these placement plan*/
					IoVm vm = (IoVm) getVmList().get(i);
					Storage dev = getAllStorageDevices().get(plan_array[i]);
					//IoHost pm = null;
					//if (dev instanceof IoHarddriveStorage) {
					//	IoHarddriveStorage hdd = (IoHarddriveStorage) dev;
					//	pm = hdd.getHost();
					//} else if (dev instanceof IoSolidStateStorage) {
					//	IoSolidStateStorage ssd = (IoSolidStateStorage) dev;
					//	pm = ssd.getHost();
					//}
					//double currentPower = pm.getPower();
					//double newPower = getPowerAfterAllocation(pm, vm);
					
					total_cost += costModel.getVmStorageCost(vm, dev);
					//System.out.println("Total cost "+total_cost);
					/* Is it the min cost ?*/
					if (total_cost<minCost) {
						minCost = total_cost;
						min_plan_placement = plan_array;
						//printPlacementPlan(min_plan_placement);
					}
				}
			}
			
		/* If there is more VM to place*/
		} else {
			for(int j=0; j<nbSd; j++){
				plan_array[nb_Vm - 1] = j;
				getMinPlacementPlan(nb_Vm - 1);
			}
		}
		//System.out.println("Min cost "+minCost);
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
			
			if (available_space < 0 || available_iops < 0) {
				return false;
			}
		}
		
		/* Check the available CPU and RAM */
		
		for (IoHost pm : this.<IoHost> getPmList()) {
			double pm_pe_cap = pm.getVmScheduler().getPeCapacity();
			double pm_mips_cap = pm.getVmScheduler().getAvailableMips();
			int pm_avalaible_ram = pm.getRamProvisioner().getAvailableRam();
			long pm_bw_capacity = pm.getBwProvisioner().getAvailableBw();
			
			for (int i = 0; i < nbSd; i++){
				Storage dev = getAllStorageDevices().get(i);
				IoHost pm_of_dev = getHotOfStorageDeveice(dev);
				
				if (pm.getId() == pm_of_dev.getId()) {
					
					for (int j = 0; j < plan_array.length; j++){
						if (plan_array[j] == i) {
							IoVm vm = (IoVm) getVmList().get(j);
							
							pm_pe_cap = pm_pe_cap - vm.getCurrentRequestedMaxMips();
							pm_mips_cap = pm_mips_cap - vm.getCurrentRequestedTotalMips();
							pm_avalaible_ram = pm_avalaible_ram - vm.getCurrentRequestedRam();
							pm_bw_capacity = pm_bw_capacity - vm.getCurrentRequestedBw();
						}
					}
				}
			}
			
			if (pm_pe_cap < 0 || pm_mips_cap < 0 || pm_avalaible_ram < 0 || pm_bw_capacity < 0) {
				return false;
			}
		}
		//printPlacementPlan(plan_array);
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
	
	/**
	 * Gets the power after allocation.
	 * 
	 * @param host the host
	 * @param vm the vm
	 * 
	 * @return the power after allocation
	 */
	protected double getPowerAfterAllocation(IoHost host, IoVm vm) {
		double power = 0;
		try {
			double max_utilization = getMaxUtilizationAfterAllocation(host, vm);
			if (max_utilization > 1) {
				return Double.MAX_VALUE;
			}
			//System.out.println("Max utilization "+max_utilization);
			power = host.getPowerModel().getPower(max_utilization);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return power;
	}
	
	/**
	 * Gets the power after allocation. We assume that load is balanced between PEs. The only
	 * restriction is: VM's max MIPS < PE's MIPS
	 * 
	 * @param host the host
	 * @param vm the vm
	 * 
	 * @return the power after allocation
	 */
	private double getMaxUtilizationAfterAllocation(IoHost host, IoVm vm) {
		double requestedTotalMips = vm.getCurrentRequestedTotalMips();
		double hostUtilizationMips = getUtilizationOfCpuMips(host);
		double hostPotentialUtilizationMips = hostUtilizationMips + requestedTotalMips;
		double pePotentialUtilization = hostPotentialUtilizationMips / host.getTotalMips();
		return pePotentialUtilization;
	}
	
	/**
	 * Gets the utilization of the CPU in MIPS for the current potentially allocated VMs.
	 *
	 * @param host the host
	 *
	 * @return the utilization of the CPU in MIPS
	 */
	protected double getUtilizationOfCpuMips(IoHost host) {
		double hostUtilizationMips = 0;
		for (Vm vm2 : host.getVmList()) {
			if (host.getVmsMigratingIn().contains(vm2)) {
				// calculate additional potential CPU usage of a migrating in VM
				hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2) * 0.9 / 0.1;
			}
			hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2);
		}
		return hostUtilizationMips;
	}
	
	/*
	private void printPlacementPlan(int plan_array[]) {
		System.out.println("The optimal placement plan is the below : ");
		for (int i = 0; i < plan_array.length; i++) {
			IoVm vm = (IoVm) getVmList().get(i);
			Storage dev = getAllStorageDevices().get(plan_array[i]);
			IoHost pm_of_dev = getHotOfStorageDeveice(dev);
			System.out.print("VM #"+vm.getId()+" in PM #"+pm_of_dev.getId()+" in SD #"+dev.getUid()+" ");
		}
	System.out.println();
	}
	*/
	
	public List<Map<String, Object>> getNewPlacementPlan() {
		List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
		getMinPlacementPlan(this.nbVm);
		
		//printPlacementPlan(min_plan_placement);
		if (isValid(min_plan_placement)) {
			migrationMap = constructPlacementPlanFromArray (min_plan_placement);
		}
		
		return migrationMap;
	}
}
