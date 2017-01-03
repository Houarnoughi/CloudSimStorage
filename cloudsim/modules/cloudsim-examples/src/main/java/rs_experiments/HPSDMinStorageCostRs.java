package rs_experiments;

import java.io.IOException;

import thesis_experiments.IoConstants;
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
public class HPSDMinStorageCostRs {

	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(String[] args) throws IOException {
		boolean enableOutput = false;
		boolean outputToFile = true;
		String inputFolder = IoConstants.INPUT_IO_WORKLOAD_CPU_DIR;
		String outputFolder = "output";
		String workload = "mix_vm"; // Real Workload
		String ioVmAllocationPolicy = "HPSD"; // Heuristic Min Cost
		String ioVmSelectionPolicy = "rs"; // Random selection
		String parameter = "1"; // the static utilization threshold
		String maxThr = "0.8";
		String minThr = "0.1";

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
	}

}
