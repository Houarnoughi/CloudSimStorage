package io_storage;

/**
 * This Class represents an I/O workload that will be executed by an EnergyVm
 * @author hamza
 *
 */
public class IoWorkloadModel {
	
	/** A workload significant name (i.e. write intensive, random dominant, etc) */
	private String name;
	
	/** Read Operations rate (must be between 0 and 1) */
	private double readRate;
	
	/** Random Operations rate (must be between 0 and 1) */
	private double randomRate;
	
	/** IO request size */
	private int ioSize;
	
	/** The data volume to be executed on storage system (in MB) */
	private double volume;
	
	/** The request arrival rate */
	private int arrivalRate;
	
	/** IO utilization file */
	private String utilizationFileName;
	
	/**
	 * Constructor with given values
	 * @param name Name of experiment
	 * @param readRate The read rate ([0,1])
	 * @param randomRate The random rate ([0,1])
	 * @param ioSize Average IO Size (in bytes)
	 * @param volume The total amount of data (in MB)
	 * @param arrivalRate IO requests per second
	 */
	public IoWorkloadModel(String name,
							double readRate,
							double randomRate,
							int ioSize,
							double volume,
							// Temporary solution, waiting for the implementation of a scheduler (files d'attente)
							int arrivalRate){
		setName(name);
		setReadRate(readRate);
		setRandomRate(randomRate);
		setIoSize(ioSize);
		setVolume(volume);
		setArrivalRate(arrivalRate);
	}
	
	public IoWorkloadModel(String ioUtilizationFile){
		setUtilizationFileName(ioUtilizationFile);
		}
	/**
	 * Constructor with default configuration
	 * @param name  Name of experiment
	 */
	public IoWorkloadModel(){
		defaultConfig();
		}
	
	/**
	 * A default configuration
	 */
	public void defaultConfig() {
		setName("Default Config");
		setReadRate(0.8);
		setRandomRate(0.3);
		setIoSize(4096);
		setVolume(500);
		setArrivalRate(1000);
		
	}
	
	/**
	 * Gets the read rate 
	 * @return read rate
	 */
	public double getReadRate(double time) {
		return readRate;
	}
	
	/**
	 * Sets the read rate 
	 * @param readRate
	 * @return true if rate between 0 and 1
	 */
	public boolean setReadRate(double readRate) {
		if (readRate <= 1 && readRate >= 0) {
			this.readRate = readRate;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Gets the random rate 
	 * @return random rate
	 */
	public double getRandomRate(double time) {
		return randomRate;
	}
	
	/**
	 * Sets the random rate
	 * @param randomRate
	 * @return true if rate between 0 and 1
	 */
	public boolean setRandomRate(double randomRate) {
		if (randomRate >= 0 && randomRate <= 1) {
			this.randomRate = randomRate;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Gets the IO size
	 * @return io size in bytes
	 */
	public int getIoSize(double time) {
		return ioSize;
	}
	
	/**
	 * Sets the IO size
	 * @param ioSize
	 * @return true if IO size greater tan 0
	 */
	public boolean setIoSize(int ioSize) {
		if (ioSize > 0) {
			this.ioSize = ioSize;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Gets the volume of data to be executed on storage system 
	 * @return the data volume in MB
	 */
	public double getVolume(double time) {
		return volume;
	}
	
	/**
	 * Sets the volume of data to be executed on storage system 
	 * @param volume the data volume in MB
	 * @return true if volume greater than 0
	 */
	public boolean setVolume(double volume) {
		
		if (volume >= 0) {
			this.volume = volume;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * The request arrival rate
	 * @return
	 */
	public int getArrivalRate(double time) {
		return arrivalRate;
	}

	public void setArrivalRate(int arrivalRate) {
		this.arrivalRate = arrivalRate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUtilizationFileName() {
		return utilizationFileName;
	}

	public void setUtilizationFileName(String utilizationFileName) {
		this.utilizationFileName = utilizationFileName;
	}

}
