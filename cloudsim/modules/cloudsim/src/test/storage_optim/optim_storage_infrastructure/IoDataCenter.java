package optim_storage_infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;

public class IoDataCenter extends PowerDatacenter {

	private IoStorageCostModel storageCostModel;
	
	/** The storage system energy model */
	private IoStorageEnergyModel storageEnergyModel;

	/** The storage wear out cost */
	private IoStorageWearOutModel storageWearOutModel;

	/** The SLA model for storage system */
	private IoStorageSlaModel storageSlaModel;

	/** IO to CPU Correlatio model */
	private IoCpuCorrelationModel ioCpuCorrelationModel;

	/** Sotrage System power */
	private double storagePower;

	private double storageWearout;

	private double storageSla;
	
	private boolean localStorageEnabled;
	
	private double costPerKwh;
	
	private double allStorageCost;
	
	/** The List of all storage devices got from hosts **/
	private List<Storage> allStorageDevices;

	public IoDataCenter(String name, 
							DatacenterCharacteristics characteristics,
							VmAllocationPolicy vmAllocationPolicy, 
							List<Storage> storageList, 
							double schedulingInterval,
							// Storage part
							IoStorageEnergyModel storageEnergyModel, 
							IoStorageWearOutModel storageWearoutModel,
							IoStorageSlaModel storageSlaModel, 
							IoCpuCorrelationModel ioToCpuCorrelationModel,
							double cotPerKwh,
							boolean storageEnabled) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		setStorageEnergyModel(storageEnergyModel);
		setStorageWearOutModel(storageWearoutModel);
		setStorageSlaModel(storageSlaModel);
		setIoCpuCorrelationModel(ioToCpuCorrelationModel);
		setStoragePower(0);
		setStorageSla(0);
		setStorageWearout(0);
		setAllStorageCost(0);
		setCostPerKwh(cotPerKwh);
		setLocalStorageEnabled(storageEnabled);
	}

	/**
	 * Denotes an event to process IOs by VMs. This event must be a periodic
	 * event
	 */
	public static final int VM_IO = 1962;

	/**
	 * Denotes an event to process IOs by VMs. This event must be a periodic
	 * event
	 */
	public static final int VM_IO_ACK = 1956;

	@Override
	protected void processOtherEvent(SimEvent ev) {
		if (ev == null) {
			org.cloudbus.cloudsim.Log
					.printLine("Warning: " + CloudSim.clock() + ": " + this.getName() + ": Null event ignored.");
		} else {
			int tag = ev.getTag();
			switch (tag) {
			case VM_IO:
				processVmIo(ev, false);
				break;
			case VM_IO_ACK:
				processVmIo(ev, true);
				break;
			default:
				Log.printLine(
						"Warning: " + CloudSim.clock() + ":" + this.getName() + ": Unknown event ignored. Tag:" + tag);
			}
		}
	}

	/**
	 * Process IOs send by vm. Processing time depends on storage device
	 * 
	 * @param ev
	 * @param ack
	 */
	protected void processVmIo(SimEvent ev, boolean ack) {
		//Log.printLine("Hamza: VM_IO event recieved!!");

		// My code to handle this event
		/*
		 * EnergyVm vm = (EnergyVm) ev.getData();
		 * 
		 * double currentTime = CloudSim.clock();
		 * 
		 * if (vm != null) {
		 * 
		 * if (currentTime < vm.getLastIoProcessingTime()) { Log.printLine(
		 * "[MyIoPowerDatacenter.processVmIo] vm Io processing failed"); }
		 * 
		 * if(ack) { int[] data = new int[3]; data[0] = getId(); data[1] =
		 * vm.getId();
		 * 
		 * if (currentTime >= vm.getLastIoProcessingTime()) { data[2] =
		 * CloudSimTags.TRUE; } else { data[2] = CloudSimTags.FALSE; }
		 * sendNow(ev.getSource(), VM_IO_ACK, data); } }
		 */
	}
	
	/**
	 * Override the VM create method to add storage time
	 */
	@Override
	protected void processVmCreate(SimEvent ev, boolean ack) {
		IoVm vm = (IoVm) ev.getData();
		IoHost host = (IoHost) vm.getHost();

		boolean result = getVmAllocationPolicy().allocateHostForVm(vm);
		double transferTime = CloudSim.getMinTimeBetweenEvents();

		if (ack) {
			int[] data = new int[3];
			data[0] = getId();
			data[1] = vm.getId();
			if (result) {
				data[2] = CloudSimTags.TRUE;
			} else {
				data[2] = CloudSimTags.FALSE;
			}
			
			// VM can be created in data center (by broker) but not yet in a host
			// So we have to be sure that VM have a host
			if (isLocalStorageEnabled()) {
				if (host != null) {
					Storage device = IoStorageList.getDeviceContainsVm(
							host.getStorageDevices(), 
							vm);
					if (device instanceof IoHarddriveStorage) {
						IoHarddriveStorage tmp = (IoHarddriveStorage) device;
						transferTime = tmp.getTransferTime(Math.toIntExact(vm.getSize()));
						} else if (device instanceof IoSolidStateStorage) {
							IoSolidStateStorage tmp = (IoSolidStateStorage) device;
							transferTime = tmp.getTransferTime(Math.toIntExact(vm.getSize()));
						}
					
				}
				send(vm.getUserId(), transferTime, CloudSimTags.VM_CREATE_ACK, data);
			} else {
				send(vm.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_CREATE_ACK, data);
			}
		}

	// Hamza: First VM creation
	if(result)
	{
		getVmList().add(vm);

		if (vm.isBeingInstantiated()) {
			vm.setBeingInstantiated(true);
		}

		vm.updateVmProcessing(CloudSim.clock(),
				getVmAllocationPolicy().getHost(vm).getVmScheduler().getAllocatedMipsForVm(vm));
		}
	}

	// Hamza: firstly i have copied the same code from PowerDatacenter
	// I have added the IO processing after migration, to avoid processing IO in
	// vms that will be migrated
	@Override
	protected void updateCloudletProcessing() {
		if (getCloudletSubmitted() == -1 || getCloudletSubmitted() == CloudSim.clock()) {
			CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
			schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
			return;
		}

		double currentTime = CloudSim.clock();

		// if some time passed since last processing
		if (currentTime > getLastProcessTime()) {

			double minTime = updateCloudetProcessingWithoutSchedulingFutureEventsForce();

			if (!isDisableMigrations()) {
				List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(getVmList());

				if (migrationMap != null) {
					for (Map<String, Object> migrate : migrationMap) {
						IoVm vm = (IoVm) migrate.get("vm");
						IoHost targetHost = (IoHost) migrate.get("host");
						IoHost oldHost = (IoHost) vm.getHost();

						if (oldHost == null) {
							Log.formatLine("%.2f: Migration of VM #%d to Host #%d is started", currentTime, vm.getId(),
									targetHost.getId());
						} else {
							Log.formatLine("%.2f: Migration of VM #%d from Host #%d to Host #%d is started",
									currentTime, vm.getId(), oldHost.getId(), targetHost.getId());
						}

						targetHost.addMigratingInVm(vm);
						incrementMigrationCount();

						/** VM migration delay = RAM / bandwidth **/
						// we use BW / 2 to model BW available for migration
						// purposes, the other
						// half of BW is for VM communication
						// around 16 seconds for 1024 MB using 1 Gbit/s network
						if (isLocalStorageEnabled()) {
							send(getId(),
									vm.getSize() / ((double) targetHost.getBw() / (2 * 8000)), CloudSimTags.VM_MIGRATE,
									migrate);
						} else {
							send(getId(), 
									vm.getRam() / ((double) targetHost.getBw() / (2 * 8000)),
									CloudSimTags.VM_MIGRATE,
									migrate);
						}
					}
				}
			}

			// schedules an event to the next time
			if (minTime != Double.MAX_VALUE) {
				CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
				send(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
			}

			setLastProcessTime(currentTime);
		}
	}

	@Override
	protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
		double currentTime = CloudSim.clock();
		double minTime = Double.MAX_VALUE;
		double timeDiff = currentTime - getLastProcessTime();
		double timeFrameDatacenterEnergy = 0.0;
		double ioTime = 0.0;
		
		double egyPriceKwh = 0.0887; 			// 0.0887 euros / kWh
		double egyPrice =  egyPriceKwh/3600000; // Price per WattSec (Joule)
		double CloudServPrice = 0.5;			// Cloud Service Price / hour
		double bill = CloudServPrice * 30 * 24; // Bill amount / month
		
		// Hamza: Storage energy
		double storageEnergy = 0.0;
		double wearoutCost = 0.0;
		double sla_storage = 0.0;
		
		BcomStorageCostModel costModel = new BcomStorageCostModel(0.0, egyPrice, bill);

		Log.printLine("\n\n--------------------------------------------------------------\n\n");
		Log.formatLine("New resource usage for the time frame starting at %.2f:", currentTime);

		for (IoHost host : this.<IoHost> getHostList()) {
			Log.printLine();

			// Added by Hamza: IO processing before vmProcessing
			
			if (!host.getVmList().isEmpty()) {
				ioTime = host.updateVmsIoProcessing(currentTime);
				send(getId(), ioTime, VM_IO);
				
			}
			
			double time = host.updateVmsProcessing(currentTime); // inform VMs
																	// to update
																	// processing
			if ((time + ioTime) < minTime) {
				minTime = time + ioTime ;
			}

			Log.formatLine("%.2f: [Host #%d] utilization is %.2f%%", currentTime, host.getId(),
					host.getUtilizationOfCpu() * 100);
		}

		if (timeDiff > 0) {
			Log.formatLine("\nEnergy consumption for the last time frame from %.2f to %.2f:", getLastProcessTime(),
					currentTime);

			for (IoHost host : this.<IoHost> getHostList()) {
				double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
				double utilizationOfCpu = host.getUtilizationOfCpu();
				double timeFrameHostEnergy = host.getEnergyLinearInterpolation(previousUtilizationOfCpu,
						utilizationOfCpu, timeDiff);
				timeFrameDatacenterEnergy += timeFrameHostEnergy;

				// Hamza: same thing for storage
				
				if (isLocalStorageEnabled()) {
					double tmpStrEnergy = getStorageEnergyModel().getHostStorageEnergy(
							host, 
							timeDiff);
					storageEnergy += tmpStrEnergy;
					double tmpWearOut = getStorageWearOutModel().getHostStorageWearOut(
							host, 
							timeDiff);
					wearoutCost += tmpWearOut / getHostList().size();
					double tmpSlaStorage = getStorageSlaModel().getHostStorageSla(host, currentTime);
					sla_storage += tmpSlaStorage;
				}
				
				Log.printLine();
				Log.formatLine("%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%", currentTime,
						host.getId(), getLastProcessTime(), previousUtilizationOfCpu * 100, utilizationOfCpu * 100);
				Log.formatLine("%.2f: [Host #%d] energy is %.2f W*sec", currentTime, host.getId(), timeFrameHostEnergy);
			}

			Log.formatLine("\n%.2f: Data center's energy is %.2f W*sec\n", currentTime, timeFrameDatacenterEnergy);
		}

		setPower(getPower() + timeFrameDatacenterEnergy);

		// Hamza: Set storage power also
		
		if (isLocalStorageEnabled()) {
			setStoragePower(getStoragePower() + storageEnergy);
			setStorageWearout(getStorageWearout() + wearoutCost);
			setStorageSla(getStorageSla() + sla_storage);
			for (Host host: this.getHostList()){
				IoHost ioHost = (IoHost) host;
				for (Vm vm: ioHost.getVmList()){
					IoVm ioVm = (IoVm) vm;
					Storage device = IoStorageList.getDeviceContainsVm(ioHost.getStorageDevices(), ioVm);
					double allCost = costModel.getVmStorageCost(vm, device);
					setAllStorageCost(getAllStorageCost()+allCost);
				}
			}
		}
		
		checkCloudletCompletion();

		/** Remove completed VMs **/
		for (PowerHost host : this.<PowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}

		Log.printLine();

		setLastProcessTime(currentTime);
		return minTime;
	}
	

	public IoStorageEnergyModel getStorageEnergyModel() {
		return storageEnergyModel;
	}

	public void setStorageEnergyModel(IoStorageEnergyModel storageEnergyModel) {
		this.storageEnergyModel = storageEnergyModel;
	}

	public IoStorageWearOutModel getStorageWearOutModel() {
		return storageWearOutModel;
	}

	public void setStorageWearOutModel(IoStorageWearOutModel storageWearOutModel) {
		this.storageWearOutModel = storageWearOutModel;
	}

	public IoStorageSlaModel getStorageSlaModel() {
		return storageSlaModel;
	}

	public void setStorageSlaModel(IoStorageSlaModel storageSlaModel) {
		this.storageSlaModel = storageSlaModel;
	}

	public IoCpuCorrelationModel getIoCpuCorrelationModel() {
		return ioCpuCorrelationModel;
	}

	public void setIoCpuCorrelationModel(IoCpuCorrelationModel ioCpuCorrelationModel) {
		this.ioCpuCorrelationModel = ioCpuCorrelationModel;
	}

	public double getStoragePower() {
		return storagePower;
	}

	public void setStoragePower(double storagepower) {
		this.storagePower = storagepower;
	}

	public double getStorageWearout() {
		return storageWearout;
	}

	public void setStorageWearout(double storageWearout) {
		this.storageWearout = storageWearout;
	}

	public double getStorageSla() {
		return storageSla;
	}

	public void setStorageSla(double storageSla) {
		this.storageSla = storageSla;
	}

	public boolean isLocalStorageEnabled() {
		return localStorageEnabled;
	}

	public void setLocalStorageEnabled(boolean storageEnabled) {
		this.localStorageEnabled = storageEnabled;
	}

	public IoStorageCostModel getStorageCostModel() {
		return storageCostModel;
	}

	public void setStorageCostModel(IoStorageCostModel storageCostModel) {
		this.storageCostModel = storageCostModel;
	}

	public List<Storage> getAllStorageDevices() {
		List<Storage> listStorage = new ArrayList<Storage>();
		for (Host host: getHostList()) {
			IoHost egyHost = (IoHost) host;
			listStorage.addAll(egyHost.getStorageDevices());
		}
		
		this.setAllStorageDevices(listStorage);
		return allStorageDevices;
	}

	public void setAllStorageDevices(List<Storage> allStorageDevices) {
		this.allStorageDevices = allStorageDevices;
	}

	public double getCostPerKwh() {
		return costPerKwh;
	}

	public void setCostPerKwh(double costPerKwh) {
		this.costPerKwh = costPerKwh;
	}

	public double getAllStorageCost() {
		return allStorageCost;
	}

	public void setAllStorageCost(double allStorageCost) {
		this.allStorageCost = allStorageCost;
	}

}
