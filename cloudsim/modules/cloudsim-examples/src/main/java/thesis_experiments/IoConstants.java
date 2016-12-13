package thesis_experiments;

import org.cloudbus.cloudsim.power.models.PowerModel;

import optim_storage_infrastructure.IoCpuCorrelationModel;
import optim_storage_infrastructure.IoPowerModelFgcsConst;
import optim_storage_infrastructure.IoStorageEnergyModel;
import optim_storage_infrastructure.IoStorageSlaModel;
import optim_storage_infrastructure.IoStorageWearOutModel;
import optim_storage_infrastructure.IoWorkloadModel;


/**
 * If you are using any algorithms, policies or workload included in the power
 * package, please cite the following paper:
 *
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic
 * Algorithms and Adaptive Heuristics for Energy and Performance Efficient
 * Dynamic Consolidation of Virtual Machines in Cloud Data Centers", Concurrency
 * and Computation: Practice and Experience (CCPE), Volume 24, Issue 13, Pages:
 * 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 *
 * @author Anton Beloglazov
 * @since Jan 6, 2012
 */
public class IoConstants {
	
	/** Hamza */
	
	public final static boolean ENABLE_OUTPUT = true;
	public final static boolean OUTPUT_CSV = false;

	public final static double SCHEDULING_INTERVAL = 300;
	public final static double SIMULATION_LIMIT = 24 * 60 * 60; // Not used in
																// my
																// experiments

	public final static int CLOUDLET_LENGTH = 600000;	// 600000 / 1000 = 600 seconds =10 Min simulation
	public final static int CLOUDLET_PES = 1;

	/*
	 * VM instance types: Presented in the paper fgcs
	 */
	/*
	public final static int VM_TYPES = 4;
	public final static int[] VM_MIPS = { 250, 250, 250, 250 };
	public final static int[] VM_PES = { 1, 1, 1, 1 };
	public final static int[] VM_RAM = { 1024, 1024, 1024, 1024 };
	public final static int VM_BW = 100000; // 100 Mbit/s
	public final static int [] VM_SIZE = {10000, 10000, 10000, 10000}; // 1 GB
	*/
	/*
	 * VM instance types: Presented in the paper fgcs
	 */
	public final static int VM_TYPES = 4;
	public final static int[] VM_MIPS = { 2000, 2000, 2000, 2000 };
	public final static int[] VM_PES = { 1, 1, 1, 1 };
	public final static int[] VM_RAM = { 1024, 1024, 2048, 2048 };
	public final static int [] VM_BW = {100000, 100000, 200000, 200000}; // 100 Mbit/s
	public final static int [] VM_SIZE = {10000, 10000, 10000, 10000}; // 10GB, 20GB
	
	// VM storage
	public final static int [] VM_IOPS = {100, 100, 200, 200};
	public final static int [] VM_THR = {100, 100, 200, 200};
	public final static boolean STORAGE_ENABLED = true;
	public final static IoWorkloadModel ioWorkload1 = new IoWorkloadModel("seq_read_dominant", 0.9, 0.1, 1024, 1000, 32300);
	public final static boolean REAL_IO = true;
	public final static String INPUT_IO_WORKLOAD_STORAGE_DIR = "/home/hamza/CloudSimStorage/cloudsim/modules/cloudsim-examples/src/main/resources/workload/io/hdd/";
	public final static String INPUT_IO_WORKLOAD_CPU_DIR = "/home/hamza/CloudSimStorage/cloudsim/modules/cloudsim-examples/src/main/resources/workload/io/cpu/";
	
	/*
	 * Host types: Presented in the paper fgcs
	 */
	/*
	public final static int HOST_TYPES = 3;
	public final static int[] HOST_MIPS = { 1250, 1250, 1250 };
	public final static int[] HOST_PES = { 1, 1, 1 };
	public final static int[] HOST_RAM = { 16384, 16384, 16384 };
	public final static int HOST_BW = 1000000; // 1 Gbit/s
	public final static int HOST_STORAGE = 1000000; // 1 TB
	*/
	
	public final static int HOST_TYPES = 3;
	public final static int[] HOST_MIPS = { 16000, 16000, 16000 };
	public final static int[] HOST_PES = { 1, 1, 1 };
	public final static int[] HOST_RAM = { 16384, 16384, 16384 };
	public final static int HOST_BW = 1000000; // 1 Gbit/s
	public final static int HOST_STORAGE = 1000000; // 1 TB
	/*
	 * Here we implement the power model preseneted in the paper fgcs: Power
	 * consumption: using the model described in Section 3.2 0% CPU -> 175 W
	 * 100% CPU-> 250 W
	 */
	public final static PowerModel HOST_POWER = new IoPowerModelFgcsConst();
	
	/**
	 * Storage system models
	 */
	public final static IoStorageEnergyModel STRG_EGY = new IoStorageEnergyModel();
	public final static IoStorageWearOutModel STRG_WO = new IoStorageWearOutModel();
	public final static IoStorageSlaModel STRG_SLA = new IoStorageSlaModel();
	public final static IoCpuCorrelationModel STRG_CPU = new IoCpuCorrelationModel();
	public final static double COST_PER_KWH = 0.1763; /// Prix en californie

}
