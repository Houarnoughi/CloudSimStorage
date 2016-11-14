/**
 * This is an extension of storage parts in CloudSim to adapt it with our cost model
 * @author Hamza Ouarnoughi
 */

package io_storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cloudbus.cloudsim.File;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.ParameterException;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.distributions.ContinuousDistribution;



public class IoSolidStateStorage implements Storage {
	
	/** The storage device Unique ID (identify device in data center)**/
	private String deviceUid;
	
	/** The storage device ID **/
	private int id;
	
	/** The host Id **/
	private int hostId;
	
	/** a list storing the names of all the MyFiles on the harddrive. */
	private List<String> nameList;

	/** a list storing all the MyFiles stored on the harddrive. */
	private List<File> fileList;

	/** the name of the harddrive. */
	private final String name;

	/** a generator required to randomize the seek time. */
	private ContinuousDistribution gen;

	/** the current size of MyFiles on the harddrive. */
	private double currentSize;

	/** the total capacity of the harddrive in MB. */
	private long capacity;

	/** the maximum transfer rate in MB/sec. */
	private double maxTransferRate;

	/** the latency of the harddrive in seconds. */
	private double latency;

	/** the average seek time in seconds. */
	private double avgSeekTime;
	
	/******** Parameters defined in our cost model ********/
	/** the avrage transfer rate in MB/sec. */
	private double avgTransferRate;
	
	/** the maximum IOPS in 4k IOs/s **/
	private int maxIops;
	
	private int avgIops;
	
	/** the power consumed during sequential read IOs in watt**/
	private double seqReadPower;
	
	/** the power consumed during random read IOs in watt **/
	private double randReadPower;
	
	/** the power consumed during sequential write IOs in watt **/
	private double seqWritePower;
	
	/** the power consumed during random write IOs in watt **/
	private double randWritedPower;
	
	/** the power consumed during idle mode in watt**/
	private double idlePower;
	
	/** the power consumed during standby mode in watt**/
	private double stanbyPower;
	
	/** the unit storage price in $/GB **/
	private double storageUnitPrice;
	
	/** the max amount of data to write before the SSD get worn out in GB **/
	private double maxDataWrite;
	
	/** the amount of data written on SSD unti now in GB **/
	private double totalDataWrite;
	
	/** the warranty period defined in the data sheet **/
	private int warrantyPeriod;
	/** The host where the device is attached **/
	//private EnergyHost host;

	/**
	 * Creates a new solid state storage with a given name and capacity.
	 * 
	 * @param name the name of the new ssd
	 * @param capacity the capacity in MByte
	 * @throws MyParameterException when the name and the capacity are not valid
	 */
	public IoSolidStateStorage(String name, 
			/* Cost related attributes */
			long capacity,				// HDD total capacity in GB
			double storageUnitPrice,	// HDD unitary price $/GB  
			int maxStartStop,			// Max number of start-stop cycles
			int warrantyPeriod,			// The warranty period in years
			/* Performance attributes*/
			int maxTransferRate,		// HDD max throughput (for sequential access) 
			int maxIops,				// HDD max 
			/* Power and Energy attributes */
			double seqReadPower,		// Power consumption during sequential read operations
			double randReadPower,		// Power consumption during random read operations
			double seqWritePower,		// Power consumption during sequential write operations
			double randWritedPower,		// Power consumption during random write operations
			double idlePower,			// Power consumption during idle mode
			double stanbyPower,			// Power consumption during standby mode
			double spinupEnergy			// Energy consumed during spi nup
			) {
		this.name = name;
		setCapacity(capacity);
		// Cost model
		setStorageUnitPrice(storageUnitPrice);
		setWarrantyPriod(warrantyPeriod);
		// Performances
		setMaxTransferRate(maxTransferRate);
		setMaxIops(maxIops);
		setLatency(latency);
		// Power
		setSeqReadPower(seqReadPower);
		setRandReadPower(randReadPower);
		setSeqWritePower(seqWritePower);
		setRandWritePower(randWritedPower);
		setIdlePower(idlePower);
		setStandbyPower(stanbyPower);
		
		// Set the current configuration
		fileList = new ArrayList<File>();
		nameList = new ArrayList<String>();
		gen = null;
		setCurrentSize(0);	
	}
	
	/**
	 * Creates a new solid state storage with a given capacity. In this case the name of the storage
	 * is a default name.
	 * 
	 * @param capacity the capacity in MByte
	 * @throws MyParameterException when the capacity is not valid
	 */
	public IoSolidStateStorage(long capacity) throws ParameterException{
		if (capacity <= 0) {
			throw new ParameterException("HarddriveStorage(): Error - capacity <= 0.");
		}
		//Hamza: For debug
		//Log.printLine("Hamza: SSD created");
		name = "SSD";
		setCapacity(capacity);
		defaultConfig();
		//init();
	}
	
	/**
	 * Create a hard drive class with default configuration
	 * We take the specs of seagate ST1000DM003 HDD
	 */
	private void defaultConfig() {
		
		fileList = new ArrayList<File>();
		nameList = new ArrayList<String>();
		gen = null;
		setCurrentSize(0);
		
		// Performance attributes 
	 	setLatency(0.00416);     		// avg latency 4.16 ms in seconds
		setAvgTransferRate(470);		// Average data rate, read/write (MB/s)
		setAvgIops(75000);				// 4k iops
		setMaxIops(75000);				// 4k iops
			
		// From the old implementation
		setAvgSeekTime(0.009);   		// 9 ms
		setMaxTransferRate(550); 		// in MB/sec
		
		// Power attributes
		setIdlePower(0.65);		// Idle power in watts
		setStandbyPower(0.0002);	// Standby power in watts
		setRandReadPower(5); 	// operating mode
		setRandWritePower(5);	// operating mode
		setSeqReadPower(5);	// operating mode
		setSeqWritePower(5);	// operating mode
		
		// Cost related attributes
		setStorageUnitPrice(0.717);	// Samsung SSD 840 PRO Series price 128GB
		//setMaxDataWrite(150000);	// 150TBW (Terabytes Written)	
		setMaxDataWrite(70000000);
		setWarrantyPriod(5);		// Warranty period in years
		
		setMttf(240000);	// Sets the MTTF in hours
	}
	
	/**
	 * Gets the available space on this storage in MB.
	 * 
	 * @return the available space in MB
	 */
	@Override
	public double getAvailableSpace() {
		return getCapacity() - getCurrentSize();
	}

	/**
	 * Checks if the storage is full or not.
	 * 
	 * @return <tt>true</tt> if the storage is full, <tt>false</tt> otherwise
	 */
	@Override
	public boolean isFull() {
		if (Math.abs(getCurrentSize() - getCapacity()) < .0000001) { // currentSize == capacity
			return true;
		}
		return false;
	}

	/**
	 * Gets the number of MyFiles stored on this storage.
	 * 
	 * @return the number of stored MyFiles
	 */
	@Override
	public int getNumStoredFile() {
		return fileList.size();
	}

	/**
	 * Makes a reservation of the space on the storage to store a MyFile.
	 * 
	 * @param MyFileSize the size to be reserved in MB
	 * @return <tt>true</tt> if reservation succeeded, <tt>false</tt> otherwise
	 */
	@Override
	public boolean reserveSpace(int MyFileSize) {
		if (MyFileSize <= 0) {
			return false;
		}

		if (getCurrentSize() + MyFileSize >= getCapacity()) {
			return false;
		}

		setCurrentSize(getCurrentSize() + MyFileSize);
		return true;
	}

	/**
	 * Adds a MyFile for which the space has already been reserved. The time taken (in seconds) for
	 * adding the MyFile can also be found using {@link gridsim.datagrid.MyMyFile#getTransactionTime()}.
	 * 
	 * @param MyFile the MyFile to be added
	 * @return the time (in seconds) required to add the MyFile
	 */
	@Override
	public double addReservedFile(File MyFile) {
		if (MyFile == null) {
			return 0;
		}

		setCurrentSize(getCurrentSize() - MyFile.getSize());
		double result = addFile(MyFile);

		// if add MyFile fails, then set the current size back to its old value
		if (result == 0.0) {
			setCurrentSize(getCurrentSize() + MyFile.getSize());
		}

		return result;
	}

	/**
	 * Checks whether there is enough space on the storage for a certain MyFile.
	 * 
	 * @param MyFileSize a MyFileAttribute object to compare to
	 * @return <tt>true</tt> if enough space available, <tt>false</tt> otherwise
	 */
	@Override
	public boolean hasPotentialAvailableSpace(int MyFileSize) {
		if (MyFileSize <= 0) {
			return false;
		}

		// check if enough space left
		if (getAvailableSpace() > MyFileSize) {
			return true;
		}

		Iterator<File> it = fileList.iterator();
		File MyFile = null;
		int deletedMyFileSize = 0;

		// if not enough space, then if want to clear/delete some MyFiles
		// then check whether it still have space or not
		boolean result = false;
		while (it.hasNext()) {
			MyFile = it.next();
			if (!MyFile.isReadOnly()) {
				deletedMyFileSize += MyFile.getSize();
			}

			if (deletedMyFileSize > MyFileSize) {
				result = true;
				break;
			}
		}

		return result;
	}

	/**
	 * Gets the total capacity of the storage in MB.
	 * 
	 * @return the capacity of the storage in MB
	 */
	@Override
	public double getCapacity() {
		return capacity;
	}

	/**
	 * Gets the current size of the stored MyFiles in MB.
	 * 
	 * @return the current size of the stored MyFiles in MB
	 */
	@Override
	public double getCurrentSize() {
		return currentSize;
	}

	/**
	 * Gets the name of the storage.
	 * 
	 * @return the name of this storage
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the latency of this harddrive in seconds.
	 * 
	 * @param latency the new latency in seconds
	 * @return <tt>true</tt> if the setting succeeded, <tt>false</tt> otherwise
	 */
	public boolean setLatency(double latency) {
		if (latency < 0) {
			return false;
		}

		this.latency = latency;
		return true;
	}

	/**
	 * Gets the latency of this harddrive in seconds.
	 * 
	 * @return the latency in seconds
	 */
	public double getLatency() {
		return latency;
	}

	/**
	 * Sets the maximum transfer rate of this storage system in MB/sec.
	 * 
	 * @param rate the maximum transfer rate in MB/sec
	 * @return <tt>true</tt> if the setting succeeded, <tt>false</tt> otherwise
	 */
	@Override
	public boolean setMaxTransferRate(int rate) {
		if (rate > 0){
			maxTransferRate = rate;
			return true;
		}else{
			return false;
		}
	}

	/**
	 * Gets the maximum transfer rate of the storage in MB/sec.
	 * 
	 * @return the maximum transfer rate in MB/sec
	 */
	@Override
	public double getMaxTransferRate() {
		return maxTransferRate;
	}

	/**
	 * Sets the average seek time of the storage in seconds.
	 * 
	 * @param seekTime the average seek time in seconds
	 * @return <tt>true</tt> if the setting succeeded, <tt>false</tt> otherwise
	 */
	public boolean setAvgSeekTime(double seekTime) {
		return setAvgSeekTime(seekTime, null);
	}

	/**
	 * Sets the average seek time and a new generator of seek times in seconds. The generator
	 * determines a randomized seek time.
	 * 
	 * @param seekTime the average seek time in seconds
	 * @param gen the ContinuousGenerator which generates seek times
	 * @return <tt>true</tt> if the setting succeeded, <tt>false</tt> otherwise
	 */
	public boolean setAvgSeekTime(double seekTime, ContinuousDistribution gen) {
		if (seekTime <= 0.0) {
			return false;
		}

		avgSeekTime = seekTime;
		this.gen = gen;
		return true;
	}

	/**
	 * Gets the average seek time of the harddrive in seconds.
	 * 
	 * @return the average seek time in seconds
	 */
	public double getAvgSeekTime() {
		return avgSeekTime;
	}

	/**
	 * Gets the MyFile with the specified name. The time taken (in seconds) for getting the MyFile can
	 * also be found using {@link gridsim.datagrid.MyMyFile#getTransactionTime()}.
	 * 
	 * @param MyFileName the name of the needed MyFile
	 * @return the MyFile with the specified MyFilename
	 */
	@Override
	public File getFile(String MyFileName) {
		// check first whether MyFile name is valid or not
		File obj = null;
		if (MyFileName == null || MyFileName.length() == 0) {
			Log.printLine(name + ".getMyFile(): Warning - invalid " + "MyFile name.");
			return obj;
		}

		Iterator<File> it = fileList.iterator();
		int size = 0;
		int index = 0;
		boolean found = false;
		File tempMyFile = null;

		// find the MyFile in the disk
		while (it.hasNext()) {
			tempMyFile = it.next();
			size += tempMyFile.getSize();
			if (tempMyFile.getName().equals(MyFileName)) {
				found = true;
				obj = tempMyFile;
				break;
			}

			index++;
		}

		// if the MyFile is found, then determine the time taken to get it
		if (found) {
			obj = fileList.get(index);
			double seekTime = getSeekTime(size);
			double transferTime = getTransferTime(obj.getSize());

			// total time for this operation
			obj.setTransactionTime(seekTime + transferTime);
		}

		return obj;
	}

	/**
	 * Gets the list of MyFile names located on this storage.
	 * 
	 * @return a List of MyFile names
	 */
	@Override
	public List<String> getFileNameList() {
		return nameList;
	}

	/**
	 * Get the seek time for a MyFile with the defined size. Given a MyFile size in MB, this method
	 * returns a seek time for the MyFile in seconds.
	 * 
	 * @param MyFileSize the size of a MyFile in MB
	 * @return the seek time in seconds
	 */
	private double getSeekTime(int MyFileSize) {
		double result = 0;

		if (gen != null) {
			result += gen.sample();
		}

		if (MyFileSize > 0 && getCapacity() != 0) {
			result += (MyFileSize / getCapacity());
		}
		return result;
	}

	/**
	 * Gets the transfer time of a given MyFile.
	 * 
	 * @param MyFileSize the size of the transferred MyFile
	 * @return the transfer time in seconds
	 */
	public double getTransferTime(int MyFileSize) {
		double result = 0;
		if (MyFileSize > 0) {
			result = (MyFileSize / getAvgTransferRate());
		}
		return result;
	}

	/**
	 * Check if the MyFile is valid or not. This method checks whether the given MyFile or the MyFile name
	 * of the MyFile is valid. The method name parameter is used for debugging purposes, to output in
	 * which method an error has occured.
	 * 
	 * @param MyFile the MyFile to be checked for validity
	 * @param methodName the name of the method in which we check for validity of the MyFile
	 * @return <tt>true</tt> if the MyFile is valid, <tt>false</tt> otherwise
	 */
	private boolean isFileValid(File MyFile, String methodName) {

		if (MyFile == null) {
			Log.printLine(name + "." + methodName + ": Warning - the given MyFile is null.");
			return false;
		}

		String MyFileName = MyFile.getName();
		if (MyFileName == null || MyFileName.length() == 0) {
			Log.printLine(name + "." + methodName + ": Warning - invalid MyFile name.");
			return false;
		}

		return true;
	}

	/**
	 * Adds a MyFile to the storage. First, the method checks if there is enough space on the storage,
	 * then it checks if the MyFile with the same name is already taken to avoid duplicate MyFilenames. <br>
	 * The time taken (in seconds) for adding the MyFile can also be found using
	 * {@link gridsim.datagrid.MyMyFile#getTransactionTime()}.
	 * 
	 * @param MyFile the MyFile to be added
	 * @return the time taken (in seconds) for adding the specified MyFile
	 */
	@Override
	public double addFile(File MyFile) {
		double result = 0.0;
		// check if the MyFile is valid or not
		if (!isFileValid(MyFile, "addMyFile()")) {
			return result;
		}

		// check the capacity
		if (MyFile.getSize() + getCurrentSize() > getCapacity()) {
			Log.printLine(name + ".addMyFile(): Warning - not enough space" + " to store " + MyFile.getName());
			return result;
		}

		// check if the same MyFile name is alredy taken
		if (!contains(MyFile.getName())) {
			double seekTime = getSeekTime(MyFile.getSize());
			double transferTime = getTransferTime(MyFile.getSize());

			fileList.add(MyFile);               					// add the MyFile into the HD
			nameList.add(MyFile.getName());     					// add the name to the name list
			setCurrentSize(getCurrentSize() + MyFile.getSize());	// increment the current HD size
			result = seekTime + transferTime;  						// add total time
		} else {
			//Log.printLine("WARNING: file "+MyFile.getName()+" already exists on device "+this.getDeviceUid());
			result = 0.0;
		}
		MyFile.setTransactionTime(result);
		return result;
	}

	/**
	 * Adds a set of MyFiles to the storage. Runs through the list of MyFiles and save all of them. The
	 * time taken (in seconds) for adding each MyFile can also be found using
	 * {@link gridsim.datagrid.MyMyFile#getTransactionTime()}.
	 * 
	 * @param list the MyFiles to be added
	 * @return the time taken (in seconds) for adding the specified MyFiles
	 */
	@Override
	public double addFile(List<File> list) {
		double result = 0.0;
		if (list == null || list.isEmpty()) {
			Log.printLine(name + ".addMyFile(): Warning - list is empty.");
			return result;
		}

		Iterator<File> it = list.iterator();
		File MyFile = null;
		while (it.hasNext()) {
			MyFile = it.next();
			result += addFile(MyFile);    // add each MyFile in the list
		}
		return result;
	}

	/**
	 * Removes a MyFile from the storage. The time taken (in seconds) for deleting the MyFile can also
	 * be found using {@link gridsim.datagrid.MyMyFile#getTransactionTime()}.
	 * 
	 * @param MyFileName the name of the MyFile to be removed
	 * @return the deleted MyFile
	 */
	@Override
	public File deleteFile(String MyFileName) {
		if (MyFileName == null || MyFileName.length() == 0) {
			return null;
		}

		Iterator<File> it = fileList.iterator();
		File MyFile = null;
		while (it.hasNext()) {
			MyFile = it.next();
			String name = MyFile.getName();

			// if a MyFile is found then delete
			if (MyFileName.equals(name)) {
				double result = deleteFile(MyFile);
				MyFile.setTransactionTime(result);
				break;
			} else {
				MyFile = null;
			}
		}
		return MyFile;
	}

	/**
	 * Removes a MyFile from the storage. The time taken (in seconds) for deleting the MyFile can also
	 * be found using {@link gridsim.datagrid.MyMyFile#getTransactionTime()}.
	 * 
	 * @param MyFileName the name of the MyFile to be removed
	 * @param MyFile the MyFile which is removed from the storage is returned through this parameter
	 * @return the time taken (in seconds) for deleting the specified MyFile
	 */
	@Override
	public double deleteFile(String MyFileName, File MyFile) {
		return deleteFile(MyFile);
	}

	/**
	 * Removes a MyFile from the storage. The time taken (in seconds) for deleting the MyFile can also
	 * be found using {@link gridsim.datagrid.MyMyFile#getTransactionTime()}.
	 * 
	 * @param MyFile the MyFile which is removed from the storage is returned through this parameter
	 * @return the time taken (in seconds) for deleting the specified MyFile
	 */
	@Override
	public double deleteFile(File MyFile) {
		double result = 0.0;
		// check if the MyFile is valid or not
		if (!isFileValid(MyFile, "deleteMyFile()")) {
			return result;
		}
		double seekTime = getSeekTime(MyFile.getSize());
		double transferTime = getTransferTime(MyFile.getSize());

		// check if the MyFile is in the storage
		if (contains(MyFile)) {
			fileList.remove(MyFile);            // remove the MyFile HD
			nameList.remove(MyFile.getName());  // remove the name from name list
			setCurrentSize(getCurrentSize() - MyFile.getSize());
			// currentSize -= MyFile.getSize();    // decrement the current HD space
			result = seekTime + transferTime;  // total time
			MyFile.setTransactionTime(result);
		}
		return result;
	}

	/**
	 * Checks whether a certain MyFile is on the storage or not.
	 * 
	 * @param MyFileName the name of the MyFile we are looking for
	 * @return <tt>true</tt> if the MyFile is in the storage, <tt>false</tt> otherwise
	 */
	@Override
	public boolean contains(String MyFileName) {
		boolean result = false;
		if (MyFileName == null || MyFileName.length() == 0) {
			Log.printLine(name + ".contains(): Warning - invalid MyFile name");
			return result;
		}
		// check each MyFile in the list
		Iterator<String> it = nameList.iterator();
		while (it.hasNext()) {
			String name = it.next();
			if (name.equals(MyFileName)) {
				result = true;
				break;
			}
		}
		return result;
	}

	/**
	 * Checks whether a certain MyFile is on the storage or not.
	 * 
	 * @param MyFile the MyFile we are looking for
	 * @return <tt>true</tt> if the MyFile is in the storage, <tt>false</tt> otherwise
	 */
	@Override
	public boolean contains(File MyFile) {
		boolean result = false;
		if (!isFileValid(MyFile, "contains()")) {
			return result;
		}

		result = contains(MyFile.getName());
		return result;
	}

	/**
	 * Renames a MyFile on the storage. The time taken (in seconds) for renaming the MyFile can also be
	 * found using {@link gridsim.datagrid.MyMyFile#getTransactionTime()}.
	 * 
	 * @param MyFile the MyFile we would like to rename
	 * @param newName the new name of the MyFile
	 * @return <tt>true</tt> if the renaming succeeded, <tt>false</tt> otherwise
	 */
	@Override
	public boolean renameFile(File MyFile, String newName) {
		// check whether the new MyFilename is conflicting with existing ones
		// or not
		boolean result = false;
		if (contains(newName)) {
			return result;
		}

		// replace the MyFile name in the MyFile (physical) list
		File obj = getFile(MyFile.getName());
		if (obj == null) {
			return result;
		} else {
			obj.setName(newName);
		}

		// replace the MyFile name in the name list
		Iterator<String> it = nameList.iterator();
		while (it.hasNext()) {
			String name = it.next();
			if (name.equals(MyFile.getName())) {
				MyFile.setTransactionTime(0);
				nameList.remove(name);
				nameList.add(newName);
				result = true;
				break;
			}
		}

		return result;
	}

	@Override
	public int getMaxIops() {
		return maxIops;
	}

	public void setMaxIops(int iops) {
		this.maxIops = iops;
	}

	public double getSeqReadPower() {
		return seqReadPower;
	}

	public void setSeqReadPower(double power) {
		this.seqReadPower = power;
	}

	public double getSeqWritePower() {
		return seqWritePower;
	}

	public void setSeqWritePower(double power) {
		this.seqWritePower = power;
	}

	public double getRandReadPower() {
		return randReadPower;
	}

	public void setRandReadPower(double power) {
		this.randReadPower = power;
	}

	public double getRandWritePower() {
		return randWritedPower;
	}

	public void setRandWritePower(double power) {
		this.randWritedPower = power;
	}

	public double getStandbyPower() {
		return stanbyPower;
	}

	public void setStandbyPower(double power) {
		this.stanbyPower = power;
	}

	public double getIdlePower() {
		return idlePower;
	}

	public void setIdlePower(double power) {
		this.idlePower = power;
	}

	public double getStorageUnitPrice() {
		return storageUnitPrice;
	}

	public void setStorageUnitPrice(double price) {
		this.storageUnitPrice = price;
	}

	public double getMaxDataWrite() {
		return maxDataWrite;
	}

	public boolean setMaxDataWrite(double max) {
		if (max <= 0) {
			return false;
		}
		this.maxDataWrite = max;
		return true;
	}

	public double getTotalWrittenData() {
		return totalDataWrite;
	}

	public boolean setTotalWrittenData(double data) {
		if (data <= 0) {
			return false;
		}
		this.totalDataWrite = data;
		return true;
	}

	public int getWarrantyPriod() {
		return warrantyPeriod;
	}

	public void setWarrantyPriod(int period) {
		this.warrantyPeriod = period;
	}

	public double getAvgTransferRate() {
		return avgTransferRate;
	}

	public void setAvgTransferRate(double avgTransferRate) {
		this.avgTransferRate = avgTransferRate;
	}

	public void setCapacity(long capacity) {
		this.capacity = capacity;
		
	}

	public void setCurrentSize(double size) {
		this.currentSize = size;
		
	}

	public int getHostId() {
		
		return hostId;
	}

	public void setHostId(int id) {
		hostId = id;
		
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
		
	}

	@Override
	public String getUid() {
		setUid(getName()+"_"+Integer.toString(getId())+"_"+Integer.toString(getHostId()));
		return deviceUid;
	}

	@Override
	public void setUid(String deviceUid) {
		this.deviceUid = deviceUid;
	}


	public void setAvgIops(int iops) {
		this.avgIops = iops;
	}

	public int getAvgIops() {
		return avgIops;
	}

	public IoHost getHost() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setHost(IoHost host) {
		// TODO Auto-generated method stub
		
	}

	public void setMttf(long mttf) {
		// TODO Auto-generated method stub
		
	}

	public long getMttf() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
