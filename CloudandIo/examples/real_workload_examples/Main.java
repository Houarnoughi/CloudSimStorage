package real_workload_examples;

import java.io.IOException;

public class Main {
	
	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static void main(String[] args) throws IOException {
		boolean enableOutput = true;
		boolean outputToFile = true;
		//String inputFolder = NonPowerAware.class.getClassLoader()
			//	.getResource("workload/real_1").getPath();
		
		String inputFolder = "/home/hamza/workspace/vmStorageOptim/examples/workload/io/cpu";
		//String inputFolder = NonPowerAware.class.getClassLoader().getResource("/workload/io/cpu").getPath();
		//System.out.println("hamza: "+inputFolder);
		String outputFolder = "output";
		//String inputFolder = "";
		String workload = "2vm_per_host"; 		// Real workload
		String vmAllocationPolicy = "thr"; 	// Static Threshold (THR) VM
											// allocation policy
		String vmSelectionPolicy = "rs"; 	// Random Selection (RS) VM selection
											// policy
		String parameter = "1"; // the static utilization threshold

		new RealWorkloadRunner(enableOutput, 
				outputToFile, 
				inputFolder,
				outputFolder, 
				workload, 
				vmAllocationPolicy, 
				vmSelectionPolicy,
				parameter);
		
	}

}
