package io_workload_example;

import java.io.IOException;

/**
 * A simulation of a heterogeneous power aware data center that applies the Static Threshold (THR)
 * VM allocation policy and Minimum Migration Time (MMT) VM selection policy.
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
public class IopsThrMstrgmt {

	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(String[] args) throws IOException {
		boolean enableOutput = true;
		boolean outputToFile = true;
		String inputFolder = "";
		String outputFolder = "output";
		String workload = "random"; // Random workload
		String ioVmAllocationPolicy = "iopsthr"; // Static Threshold (THR) VM allocation policy
		String ioVmSelectionPolicy = "mstrgmt"; // Minimum Storage Migration Time (MSTRGMT) VM selection policy
		String parameter = "0.9"; // the static utilization threshold

		new IoRandomRunner(
				enableOutput,
				outputToFile,
				inputFolder,
				outputFolder,
				workload,
				ioVmAllocationPolicy,
				ioVmSelectionPolicy,
				parameter);
	}

}
