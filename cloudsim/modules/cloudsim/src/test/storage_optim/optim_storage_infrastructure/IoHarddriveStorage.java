/**
 * This is an extension of storage parts in CloudSim to adapt it with our cost model
 * @author Hamza Ouarnoughi
 */

package optim_storage_infrastructure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cloudbus.cloudsim.File;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.ParameterException;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.distributions.ContinuousDistribution;

/**
 * An implementation of a storage system. It simulates the behavior of a typical hard drive storage.
 * The default values for this storage are those of " To be defined"
 * following parameters:
 * <ul>
 * <li>latency = 4.17 ms
 * <li>avg seek time = 9 ms
 * <li>max transfer rate = 133 MB/sec
 * </ul>
 * 
 * @author Hamza Ouarnoughi
 * @param <T>
 */
public class IoHarddriveStorage implements Storage {
	
	/** The storage device Unique ID (identify device in data center)**/
	private String deviceUid;
	
	/** The storage device ID **/
	private int id;
	
	/** The host Id **/
	private int hostId;

	/** a list storing the names of all the files on the harddrive. */
	private List<String> nameList;

	/** a list storing all the files stored on the harddrive. */
	private List<File> fileList;

	/** the name of the hard drive. */
	private final String name;

	/** a generator required to randomize the seek time. */
	private ContinuousDistribution gen;

	/** the current size of files on the harddrive. */
	private double currentSize;

	/** the total capacity of the harddrive in MB. */
	private long capacity;

	/** the maximum transfer rate in MB/sec. */
	private double maxTransferRate;

	/** the latency of the harddrive in seconds. */
	private double latency;

	/** the average seek time in seconds. */
	private double avgSeekTime;
	
	/** the average read seek time in seconds. */
	private double avgReadSeekTime;
	
	/** the average write seek time in seconds. */
	private double avgWriteSeekTime;
	
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
	
	/** the power consumed during spin-up transition in Joule **/
	private double spinupEnergy;
	
	/** the unit storage price in $/GB **/
	private double storageUnitPrice;
	
	/** the max number of start stop cycles before the HDD been changed **/
	private int maxStartStop;
	
	/** the max amount of data to write before the SSD get worn out in GB **/
	private long maxDataWrite;
	
	/** the total number of start stop cycles before the HDD been changed **/
	private int totalStartStop;
	
	/** the warranty period defined in the data sheet **/
	private int warrantyPeriod;
	
	/** The host where the device is attached **/
	private IoHost host;
	
	/**
	 * Creates a new harddrive storage with a given name and capacity.
	 * 
	 * @param name the name of the new harddrive storage
	 * @param capacity the capacity in MByte
	 * @throws ParameterException when the name and the capacity are not valid
	 */
	public IoHarddriveStorage(String name, 
								int id,
								/* Cost related attributes */
								long capacity,			// HDD total capacity in GB
								double storageUnitPrice,	// HDD unitary price $/GB  
								int maxStartStop,			// Max number of start-stop cycles
								int warrantyPeriod,			// The warranty period in years
								/* Performance attributes*/
								int maxTransferRate,		// HDD max throughput (for sequential access) 
								int maxIops,				// HDD max
								double latency,				// The latency (from the old implementation)
								double avgSeekTime, 		// The average seek time in seconds (from the old implementation)
								double avgReadSeekTime,		// The average read seek time in seconds
								double avgWriteSeekTime,	// The average write seek time in seconds.
								/* Power and Energy attributes */
								double seqReadPower,		// Power consumption during sequential read operations
								double randReadPower,		// Power consumption during random read operations
								double seqWritePower,		// Power consumption during sequential write operations
								double randWritedPower,		// Power consumption during random write operations
								double idlePower,			// Power consumption during idle mode
								double stanbyPower,			// Power consumption during standby mode
								double spinupEnergy			// Energy consumed during spi nup 
								) throws ParameterException {
		
		if (name == null || name.length() == 0) {
			throw new ParameterException("HarddriveStorage(): Error - invalid storage name.");
		}

		if (capacity <= 0) {
			throw new ParameterException("HarddriveStorage(): Error - capacity <= 0.");
		}

		this.name = name;
		setId(id);
		setHostId(hostId);
		setCapacity(capacity);
		// Cost model
		setStorageUnitPrice(storageUnitPrice);
		setMaxStartStop(maxStartStop);
		setWarrantyPriod(warrantyPeriod);
		// Performances
		setMaxTransferRate(maxTransferRate);
		setMaxIops(maxIops);
		setLatency(latency);
		setAvgReadSeekTime(avgReadSeekTime);
		setAvgWriteSeekTime(avgWriteSeekTime);
		// Power
		setSeqReadPower(seqReadPower);
		setRandReadPower(randReadPower);
		setSeqWritePower(seqWritePower);
		setRandWritePower(randWritedPower);
		setIdlePower(idlePower);
		setStandbyPower(stanbyPower);
		setSpinupEnergy(spinupEnergy);
		
		//init();
		
		// Set the current configuration
		fileList = new ArrayList<File>();
		nameList = new ArrayList<String>();
		gen = null;
		setCurrentSize(0);
		
	}

	/**
	 * Creates a new harddrive storage with a given capacity. In this case the name of the storage
	 * is a default name.
	 * 
	 * @param capacity the capacity in MByte
	 * @throws ParameterException when the capacity is not valid
	 */
	public IoHarddriveStorage(long capacity) throws ParameterException {
		
		if (capacity <= 0) {
			throw new ParameterException("HarddriveStorage(): Error - capacity <= 0.");
		}
		
		//Hamza: For debug
		//Log.printLine("Hamza: HDD created");
		name = "HDD";
		setCapacity(capacity);
		defaultConfig();
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
		latency = 0.00416;     		// avg latency 4.16 ms in seconds
		setAvgReadSeekTime(0.0085);	// avg read seek time
		setAvgWriteSeekTime(0.0095);// avg write seek time
		setAvgTransferRate(150);	// Average data rate, read/write (MB/s)
		setMaxIops(35000);			// 4k iops
		setAvgIops(35000);
		
		// From the old implementation
		avgSeekTime = 0.009;   // 9 ms
		maxTransferRate = 133; // in MB/sec
		
		// Power attributes
		setIdlePower(3.36);		// Idle power in watts
		setStandbyPower(0.63);	// Standby power in watts
		setRandReadPower(5.9); 	// operating mode
		setRandWritePower(5.9);	// operating mode
		setSeqReadPower(5.9);	// operating mode
		setSeqWritePower(5.9);	// operating mode
		setSpinupEnergy(0);		// Energy in W/s NOT yet defined
		
		// Cost related attributes
		setStorageUnitPrice(0.075);	// The hdd unitary price $/GB
		setMaxStartStop(50000);		// Max star-stop cycles
		setTotalStartStop(0);		// Initialize current star-stop cycles
		setWarrantyPriod(2);		// Warranty period in years
		
		setMttf(1000000);	// Sets the MTTF in hours
		setMaxDataWrite(2500000000L); // https://www.micron.com/about/blogs/2016/february/the-myth-of-hdd-endurance
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
		if (Math.abs(currentSize - capacity) < .0000001) { // currentSize == capacity
			return true;
		}
		return false;
	}

	/**
	 * Gets the number of files stored on this storage.
	 * 
	 * @return the number of stored files
	 */
	@Override
	public int getNumStoredFile() {
		return fileList.size();
	}

	/**
	 * Makes a reservation of the space on the storage to store a file.
	 * 
	 * @param fileSize the size to be reserved in MB
	 * @return <tt>true</tt> if reservation succeeded, <tt>false</tt> otherwise
	 */
	@Override
	public boolean reserveSpace(int fileSize) {
		if (fileSize <= 0) {
			return false;
		}

		if (getCurrentSize() + fileSize >= getCapacity()) {
			return false;
		}

		setCurrentSize(getCurrentSize() + fileSize);
		return true;
	}

	/**
	 * Adds a file for which the space has already been reserved. The time taken (in seconds) for
	 * adding the file can also be found using {@link gridsim.datagrid.MyFile#getTransactionTime()}.
	 * 
	 * @param file the file to be added
	 * @return the time (in seconds) required to add the file
	 */
	@Override
	public double addReservedFile(File file) {
		if (file == null) {
			return 0;
		}

		setCurrentSize(getCurrentSize() - file.getSize());
		double result = addFile(file);

		// if add file fails, then set the current size back to its old value
		if (result == 0.0) {
			setCurrentSize(getCurrentSize() + file.getSize());
		}

		return result;
	}

	/**
	 * Checks whether there is enough space on the storage for a certain file.
	 * 
	 * @param fileSize a FileAttribute object to compare to
	 * @return <tt>true</tt> if enough space available, <tt>false</tt> otherwise
	 */
	@Override
	public boolean hasPotentialAvailableSpace(int fileSize) {
		if (fileSize <= 0) {
			return false;
		}

		// check if enough space left
		if (getAvailableSpace() > fileSize) {
			return true;
		}

		Iterator<File> it = fileList.iterator();
		File file = null;
		int deletedFileSize = 0;

		// if not enough space, then if want to clear/delete some files
		// then check whether it still have space or not
		boolean result = false;
		while (it.hasNext()) {
			file = it.next();
			if (!file.isReadOnly()) {
				deletedFileSize += file.getSize();
			}

			if (deletedFileSize > fileSize) {
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
	 * Gets the current size of the stored files in MB.
	 * 
	 * @return the current size of the stored files in MB
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
	 * Gets the file with the specified name. The time taken (in seconds) for getting the file can
	 * also be found using {@link gridsim.datagrid.MyFile#getTransactionTime()}.
	 * 
	 * @param fileName the name of the needed file
	 * @return the file with the specified filename
	 */
	@Override
	public File getFile(String fileName) {
		// check first whether file name is valid or not
		File obj = null;
		if (fileName == null || fileName.length() == 0) {
			Log.printLine(name + ".getFile(): Warning - invalid " + "file name.");
			return obj;
		}

		Iterator<File> it = fileList.iterator();
		int size = 0;
		int index = 0;
		boolean found = false;
		File tempFile = null;

		// find the file in the disk
		while (it.hasNext()) {
			tempFile = it.next();
			size += tempFile.getSize();
			if (tempFile.getName().equals(fileName)) {
				found = true;
				obj = tempFile;
				break;
			}

			index++;
		}

		// if the file is found, then determine the time taken to get it
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
	 * Gets the list of file names located on this storage.
	 * 
	 * @return a List of file names
	 */
	@Override
	public List<String> getFileNameList() {
		return nameList;
	}

	/**
	 * Get the seek time for a file with the defined size. Given a file size in MB, this method
	 * returns a seek time for the file in seconds.
	 * 
	 * @param fileSize the size of a file in MB
	 * @return the seek time in seconds
	 */
	private double getSeekTime(int fileSize) {
		double result = 0;

		if (gen != null) {
			result += gen.sample();
		}

		if (fileSize > 0 && getCapacity() != 0) {
			result += (fileSize / getCapacity());
		}

		return result;
	}

	/**
	 * Gets the transfer time of a given file.
	 * 
	 * @param fileSize the size of the transferred file
	 * @return the transfer time in seconds
	 */
	//@Override
	public double getTransferTime(int fileSize) {
		double result = 0;
		if (fileSize > 0 ) {
			result = (fileSize / getAvgTransferRate());
		}

		return result;
	}

	/**
	 * Check if the file is valid or not. This method checks whether the given file or the file name
	 * of the file is valid. The method name parameter is used for debugging purposes, to output in
	 * which method an error has occured.
	 * 
	 * @param file the file to be checked for validity
	 * @param methodName the name of the method in which we check for validity of the file
	 * @return <tt>true</tt> if the file is valid, <tt>false</tt> otherwise
	 */
	private boolean isFileValid(File file, String methodName) {

		if (file == null) {
			Log.printLine(name + "." + methodName + ": Warning - the given file is null.");
			return false;
		}

		String fileName = file.getName();
		if (fileName == null || fileName.length() == 0) {
			Log.printLine(name + "." + methodName + ": Warning - invalid file name.");
			return false;
		}

		return true;
	}

	/**
	 * Adds a file to the storage. First, the method checks if there is enough space on the storage,
	 * then it checks if the file with the same name is already taken to avoid duplicate filenames. <br>
	 * The time taken (in seconds) for adding the file can also be found using
	 * {@link gridsim.datagrid.MyFile#getTransactionTime()}.
	 * 
	 * @param file the file to be added
	 * @return the time taken (in seconds) for adding the specified file
	 */
	@Override
	public double addFile(File file) {
		double result = 0.0;
		// check if the file is valid or not
		if (!isFileValid(file, "addFile()")) {
			return result;
		}

		// check the capacity
		if (file.getSize() + currentSize > capacity) {
			Log.printLine(name + ".addFile(): Warning - not enough space" + " to store " + file.getName());
			return result;
		}

		// check if the same file name is alredy taken
		if (!contains(file.getName())) {
			double seekTime = getSeekTime(file.getSize());
			double transferTime = getTransferTime(file.getSize());

			fileList.add(file);               // add the file into the HD
			nameList.add(file.getName());     // add the name to the name list
			setCurrentSize(getCurrentSize() + file.getSize());    // increment the current HD size
			result = seekTime + transferTime;  // add total time
		}else {
			//Log.printLine("WARNING: file "+file.getName()+" already exists on device "+this.getDeviceUid());
			result = 0.0;
		}
		file.setTransactionTime(result);
		return result;
	}

	/**
	 * Adds a set of files to the storage. Runs through the list of files and save all of them. The
	 * time taken (in seconds) for adding each file can also be found using
	 * {@link gridsim.datagrid.MyFile#getTransactionTime()}.
	 * 
	 * @param list the files to be added
	 * @return the time taken (in seconds) for adding the specified files
	 */
	@Override
	public double addFile(List<File> list) {
		double result = 0.0;
		if (list == null || list.isEmpty()) {
			Log.printLine(name + ".addFile(): Warning - list is empty.");
			return result;
		}

		Iterator<File> it = list.iterator();
		File file = null;
		while (it.hasNext()) {
			file = it.next();
			result += addFile(file);    // add each file in the list
		}
		return result;
	}

	/**
	 * Removes a file from the storage. The time taken (in seconds) for deleting the file can also
	 * be found using {@link gridsim.datagrid.MyFile#getTransactionTime()}.
	 * 
	 * @param fileName the name of the file to be removed
	 * @return the deleted file
	 */
	@Override
	public File deleteFile(String fileName) {
		if (fileName == null || fileName.length() == 0) {
			return null;
		}

		Iterator<File> it = fileList.iterator();
		File file = null;
		while (it.hasNext()) {
			file = it.next();
			String name = file.getName();

			// if a file is found then delete
			if (fileName.equals(name)) {
				double result = deleteFile(file);
				file.setTransactionTime(result);
				break;
			} else {
				file = null;
			}
		}
		return file;
	}

	/**
	 * Removes a file from the storage. The time taken (in seconds) for deleting the file can also
	 * be found using {@link gridsim.datagrid.MyFile#getTransactionTime()}.
	 * 
	 * @param fileName the name of the file to be removed
	 * @param file the file which is removed from the storage is returned through this parameter
	 * @return the time taken (in seconds) for deleting the specified file
	 */
	@Override
	public double deleteFile(String fileName, File file) {
		return deleteFile(file);
	}

	/**
	 * Removes a file from the storage. The time taken (in seconds) for deleting the file can also
	 * be found using {@link gridsim.datagrid.MyFile#getTransactionTime()}.
	 * 
	 * @param file the file which is removed from the storage is returned through this parameter
	 * @return the time taken (in seconds) for deleting the specified file
	 */
	@Override
	public double deleteFile(File file) {
		double result = 0.0;
		// check if the file is valid or not
		if (!isFileValid(file, "deleteFile()")) {
			return result;
		}
		double seekTime = getSeekTime(file.getSize());
		double transferTime = getTransferTime(file.getSize());

		// check if the file is in the storage
		if (contains(file)) {
			fileList.remove(file);            // remove the file HD
			nameList.remove(file.getName());  // remove the name from name list
			setCurrentSize(getCurrentSize() - file.getSize());
			//currentSize -= file.getSize();    // decrement the current HD space
			result = seekTime + transferTime;  // total time
			file.setTransactionTime(result);
		}
		return result;
	}

	/**
	 * Checks whether a certain file is on the storage or not.
	 * 
	 * @param fileName the name of the file we are looking for
	 * @return <tt>true</tt> if the file is in the storage, <tt>false</tt> otherwise
	 */
	@Override
	public boolean contains(String fileName) {
		boolean result = false;
		if (fileName == null || fileName.length() == 0) {
			Log.printLine(name + ".contains(): Warning - invalid file name");
			return result;
		}
		// check each file in the list
		Iterator<String> it = nameList.iterator();
		while (it.hasNext()) {
			String name = it.next();
			if (name.equals(fileName)) {
				result = true;
				break;
			}
		}
		return result;
	}

	/**
	 * Checks whether a certain file is on the storage or not.
	 * 
	 * @param file the file we are looking for
	 * @return <tt>true</tt> if the file is in the storage, <tt>false</tt> otherwise
	 */
	@Override
	public boolean contains(File file) {
		boolean result = false;
		if (!isFileValid(file, "contains()")) {
			return result;
		}

		result = contains(file.getName());
		return result;
	}

	/**
	 * Renames a file on the storage. The time taken (in seconds) for renaming the file can also be
	 * found using {@link gridsim.datagrid.MyFile#getTransactionTime()}.
	 * 
	 * @param file the file we would like to rename
	 * @param newName the new name of the file
	 * @return <tt>true</tt> if the renaming succeeded, <tt>false</tt> otherwise
	 */
	@Override
	public boolean renameFile(File file, String newName) {
		// check whether the new filename is conflicting with existing ones
		// or not
		boolean result = false;
		if (contains(newName)) {
			return result;
		}

		// replace the file name in the file (physical) list
		File obj = getFile(file.getName());
		if (obj == null) {
			return result;
		} else {
			obj.setName(newName);
		}

		// replace the file name in the name list
		Iterator<String> it = nameList.iterator();
		while (it.hasNext()) {
			String name = it.next();
			if (name.equals(file.getName())) {
				file.setTransactionTime(0);
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

	public double getSpinupEnergy() {
		return spinupEnergy;
	}

	public void setSpinupEnergy(double energy) {
		this.spinupEnergy = energy;
	}

	public double getStorageUnitPrice() {
		return storageUnitPrice;
	}

	public void setStorageUnitPrice(double price) {
		this.storageUnitPrice = price;
	}

	public double getTotalWrittenData() {
		// It is a hard drive
		return 0;
	}

	public boolean setTotalWrittenData(double data) {
		// It is a hard drive
		return false;
	}

	public int getMaxStartStop() {
		return maxStartStop;
	}

	public void setMaxStartStop(int max) {
		this.maxStartStop = max;
	}

	public int getTotalStartStop() {
		return totalStartStop;
	}

	public void setTotalStartStop(int total) {
		this.totalStartStop = total;
	}

	public int getWarrantyPriod() {
		return warrantyPeriod;
	}

	public void setWarrantyPriod(int period) {
		this.warrantyPeriod = period;
	}

	public double getAvgReadSeekTime() {
		return avgReadSeekTime;
	}

	public void setAvgReadSeekTime(double avgReadSeekTime) {
		this.avgReadSeekTime = avgReadSeekTime;
	}

	public double getAvgWriteSeekTime() {
		return avgWriteSeekTime;
	}

	public void setAvgWriteSeekTime(double avgWriteSeekTime) {
		this.avgWriteSeekTime = avgWriteSeekTime;
	}

	public double getAvgTransferRate() {
		return avgTransferRate;
	}

	public void setAvgTransferRate(double avgTransferRate) {
		this.avgTransferRate = avgTransferRate;
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
	
	/*
	public String getDeviceUid() {
		setUid(getName()+"_"+Integer.toString(getId())+"_"+Integer.toString(getHostId()));
		return deviceUid;
	}

	public void setDeviceUid(String deviceUid) {
		this.deviceUid = deviceUid;
	}
	*/
	
	public void setCapacity(long capacity) {
		this.capacity = capacity;
	}

	public void setCurrentSize(double size) {
		this.currentSize = size;
		
	}

	public void setAvgIops(int iops) {
		this.avgIops = iops;
		
	}

	public int getAvgIops() {
		return avgIops;
	}

	public IoHost getHost() {
		return host;
	}

	public void setHost(IoHost host) {
		this.host = host;
		
	}

	public void setMttf(long mttf) {
		// TODO Auto-generated method stub
		
	}

	public long getMttf() {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getMaxDataWrite() {
		return maxDataWrite;
	}

	public void setMaxDataWrite(long maxDataWrite) {
		this.maxDataWrite = maxDataWrite;
	}
}
