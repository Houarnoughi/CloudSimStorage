package miopsu_experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import thesis_experiments.IoConstants;
import thesis_experiments.IoRandomConstants;
import thesis_experiments.IoRandomRunner;

/**
 * A simulation of a heterogeneous power aware data center that applies the Static Threshold (THR)
 * VM allocation policy and Minimum Utilization (MU) VM selection policy.
 * 
 * The remaining configuration parameters are in the Constants and RandomConstants classes.
 * 
 * If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since Jan 5, 2012
 */
public class BFMinStorageCostMiopsU {

	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(String[] args) throws IOException {
		
		
		String outputDir = "/home/hamza/git/CloudSimStorage/cloudsim/thesis_results/output_"+IoConstants.STORAGE_DEVICE_TYPE+"/BFM/";
		StringBuilder builder = new StringBuilder();
		
		/*===================================*/
		
		boolean enableOutput = false;
		boolean outputToFile = true;
		String inputFolder = IoConstants.INPUT_IO_WORKLOAD_CPU_DIR;
		String outputFolder = "output";
		String workload = "mix_vm"; // Real Workload
		String ioVmAllocationPolicy = "BFMC"; // Brute Force Min Cost
		String ioVmSelectionPolicy = "miopsu"; // Random selection
		String parameter = "0.7"; // the static utilization threshold
		String maxThr = "0.7";
		String minThr = "0.1";
		/*==================================*/
		
		builder.append(outputDir+"/"+ioVmAllocationPolicy+"_"+ioVmSelectionPolicy+"_"+IoRandomConstants.NUMBER_OF_HOSTS+"_"+IoRandomConstants.NUMBER_OF_VMS+".csv");
		String filePath = builder.toString();
		File file = new File(filePath);
		if (!file.exists()) {
			file.createNewFile();
		}
		
		BufferedWriter br = new BufferedWriter(new FileWriter(file));
		br.write("#NB_PM;NB_VM;sim_time;cpu_energy;nb_mig;all_sla;vm_realloc_mean;"
				+ "exe_time_mean;strg_egy;egy_cost;wo_cost;sla_cost;all_cost;all_time\n");
		
		for(int i = 0 ; i < 5; i++) {
			/*============================*/
			long start = System.nanoTime();
			
		new IoRandomRunner(
				enableOutput,
				outputToFile,
				inputFolder,
				outputFolder,
				workload,
				ioVmAllocationPolicy,
				ioVmSelectionPolicy,
				parameter,
				maxThr,
				minThr);
		
		long stop = System.nanoTime();
		
		double seconds = (double) (stop - start) / 1000000000.0;
		System.out.println("Total elapsed time in sec "+ seconds);
		/*============================*/
		
		Map<String, Double> paramters = new HashMap<String, Double>();
		paramters = thesis_experiments.IoHelper.getStats();
		StringBuilder data = new StringBuilder();
		String delimiter = ";";
		
		data.append(paramters.get("#NB_PM")+delimiter);
		data.append(paramters.get("NB_VM")+delimiter);
		data.append(paramters.get("sim_time")+delimiter);
		data.append(paramters.get("cpu_energy")+delimiter);
		data.append(paramters.get("nb_mig")+delimiter);
		data.append(paramters.get("all_sla")+delimiter);
		data.append(paramters.get("vm_realloc_mean")+delimiter);
		data.append(paramters.get("exe_time_mean")+delimiter);
		data.append(paramters.get("strg_egy")+delimiter);
		data.append(paramters.get("egy_cost")+delimiter);
		data.append(paramters.get("wo_cost")+delimiter);
		data.append(paramters.get("sla_cost")+delimiter);
		data.append(paramters.get("all_cost")+delimiter);
		data.append(seconds);
		data.append("\n");
		try {
			br.write(data.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
			}
		}

		br.close();
	}
}
