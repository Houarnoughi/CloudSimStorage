package optim_storage_infrastructure;

import java.util.List;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerVm;


public class IoVm extends PowerVm {
	
	/** The Model that represents the I/O workload to be executed */
	private IoWorkloadModel ioWorkloadModel;
	
	/** The IOPS requested by the VM */
	private int requestedIops;
	
	/** The throughput requested by the VM */ 
	private int requestedThr;
	
	/** The last I/O processing time */
	private double lastIoProcessingTime;
	
	/** The transaction time. The same as transaction time attribute in File **/
	private double transactionTime;
	
	/** The remaining volume after processing */
	private double remainingVolume;
	
	public <T extends IoWorkloadModel> IoVm(int id, 
					int userId, 
					double mips, 
					int pesNumber, 
					int ram, 
					long bw, 
					long size, 
					int priority,
					String vmm, 
					CloudletScheduler cloudletScheduler, 
					double schedulingInterval,
					// For storage parts
					T ioWorkloadModel,
					int requestedIops,
					int requestedThr
					) {
		super(id, userId, mips, pesNumber, ram, bw, size, priority, vmm, cloudletScheduler, schedulingInterval);
		
		// For storage parts
		setIoWorkloadModel(ioWorkloadModel);
		setRequestedIops(requestedIops);
		setRequestedThr(requestedThr);
		setRemainingVolume(ioWorkloadModel.getVolume(0.0));
		
	}
	
	/**
	 * TODO: this method must implements IO processing and return
	 * @return time to process this IOs (depends on storage device)
	 */
	
	public double updateVmIoProcessing(double currentTime, int iops, double dataTransferTime) {
		
		double ioTime = 0;
		double processedVolume = 0.0;
		IoWorkloadModel model = getIoWorkloadModel();
		
		// Check if there is a data to process
		//if(getLastIoProcessingTime() >= 0 && getLastIoProcessingTime() < currentTime ){
		if(getLastIoProcessingTime() < currentTime ){	
			IoHost host = (IoHost)this.getHost();
			
			//Storage device = EnergyStorageList.getDeviceContainsVm(host.getStorageDevices(), this);
			
			double schedulingTime = getSchedulingInterval();
			
			int avgIoSize = model.getIoSize(currentTime);
				
			Double sched = new Double(schedulingTime);
			int ioNum = model.getArrivalRate(currentTime) * sched.intValue();
			
			int rRead = (int)Math.ceil((model.getRandomRate(currentTime)*
						model.getReadRate(currentTime))*ioNum);
				
			int rWrite = (int)Math.ceil((model.getRandomRate(currentTime)*
							(1 - model.getReadRate(currentTime)))*ioNum);
				
			int sRead = (int)Math.ceil((1 - model.getRandomRate(currentTime))*
						(model.getReadRate(currentTime)) * ioNum);
				
			int sWrite = (int)Math.ceil((1 - model.getRandomRate(currentTime))*
						(1 - model.getReadRate(currentTime))*ioNum);
			
			ioTime = (rRead+rWrite)/iops + (((sRead+sWrite)*avgIoSize)/1048576)/dataTransferTime;
			
			if (ioTime < 0 ){
				ioTime *= -1;
			}
			
			// The processed data volume (in MB)
			processedVolume = (ioNum * avgIoSize) / 1048576;
			setRemainingVolume(getRemainingVolume() - processedVolume);
			
			//Log.printLine("Processed volume for VM "+this.getUid()+" is "+processedVolume);
			//Log.printLine("Remaining volume for VM "+this.getUid()+" is "+getRemainingVolume());
			
			Log.formatLine(
						"%.2f: [Host #%d] IO processing of VM #%d elapsed time is %.5f s",
						CloudSim.clock(),
						host.getId(),
						this.getId(),
						ioTime);
		}
		
		return ioTime;
	}

	/*
	private void setLastIoProcessingTime(double lastIoProcessingTime) {
		this.lastIoProcessingTime = lastIoProcessingTime;		
	}
	*/
	
	public double getLastIoProcessingTime() {
		return lastIoProcessingTime;
	}

	/**
	 * Gets the I/O workload
	 * @return I/O workload model
	 */
	public IoWorkloadModel getIoWorkloadModel() {
		return ioWorkloadModel;
	}
	
	/**
	 * Sets the I/O workload
	 * @param ioWorkloadModel the I/O workload model
	 */
	public void setIoWorkloadModel(IoWorkloadModel ioWorkloadModel) {
		this.ioWorkloadModel = ioWorkloadModel;
	}
	
	/**
	 * Gets the IOPS requested by the VM
	 * @return 
	 */
	public int getRequestedIops() {
		return requestedIops;
	}
	
	/**
	 * Sets the IOPS requested by the VM 
	 * @param requestedIops the requested IOPS
	 */
	public void setRequestedIops(int requestedIops) {
		this.requestedIops = requestedIops;
	}
	
	/**
	 * Gets the throughput requested by the VM
	 * @return requested throughput
	 */
	public int getRequestedThr() {
		return requestedThr;
	}
	
	/**
	 * Sets the threshold requested by the VM
	 * @param requestedThr requested throughput
	 */
	public void setRequestedThr(int requestedTher) {
		this.requestedThr = requestedTher;
	}

	public double getStorageMigrationTime(IoHarddriveStorage src, IoSolidStateStorage dst) {
		// Vm size is in bytes, must be converted
				long imgSizeInMB = this.getSize() / 1048576;
				double time = 0.0;
				// Intra host storage migration
				if (src.getHostId() == dst.getHostId()){
					time = imgSizeInMB / Math.min(src.getAvgTransferRate(),
							dst.getAvgTransferRate());
				// Inter host storage migration. So the migration time depends also on bandwidth
				}else{
					double minDev = Math.min(src.getAvgTransferRate(), dst.getAvgTransferRate());
					time = imgSizeInMB / Math.min((getHost().getBw()/2),minDev);
				}
				return time;
	}

	/**
	 * Gets the transaction time (time to add/remove VM image file)
	 * @return transation time
	 */
	public double getTransactionTime() {
		return transactionTime;
	}
	
	/**
	 * Sets transaction time (time to add/remove VM image)
	 * @param transactionTime
	 */
	public void setTransactionTime(double transactionTime) {
		this.transactionTime = transactionTime;
	}
	
	public double getRemainingVolume() {
		return remainingVolume;
	}

	public void setRemainingVolume(double remainingVolume) {
		this.remainingVolume = remainingVolume;
	}
	
	// To access from the inside
	@Override
	public List<Double> getUtilizationHistory() {
		return super.getUtilizationHistory();
	}
	
}
