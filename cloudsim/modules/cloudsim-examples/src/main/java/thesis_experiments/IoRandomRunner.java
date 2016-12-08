package thesis_experiments;

import java.util.Calendar;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * The example runner for the random workload.
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
public class IoRandomRunner extends IoRunnerAbstract {

	/**
	 * @param enableOutput
	 * @param outputToFile
	 * @param inputFolder
	 * @param outputFolder
	 * @param workload
	 * @param vmAllocationPolicy
	 * @param vmSelectionPolicy
	 * @param parameter
	 */
	private static String workload;
	
	public IoRandomRunner(
			boolean enableOutput,
			boolean outputToFile,
			String inputFolder,
			String outputFolder,
			String workload,
			String ioVmAllocationPolicy,
			String ioVmSelectionPolicy,
			String parameter) {
		super(
				enableOutput,
				outputToFile,
				inputFolder,
				outputFolder,
				workload,
				ioVmAllocationPolicy,
				ioVmSelectionPolicy,
				parameter);
		
		// Set the workload
		setWorkload(workload);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudbus.cloudsim.examples.power.RunnerAbstract#init(java.lang.String)
	 */
	@Override
	protected void init(String inputFolder, String workload) {
		try {
			CloudSim.init(1, Calendar.getInstance(), false);

			broker = IoHelper.createBroker();
			int brokerId = broker.getId();

			cloudletList = RealWorkloadHelper.createCloudletListRealWorkload(
					brokerId, inputFolder + "/" + workload);
			vmList = IoHelper.createVmList(brokerId, cloudletList.size(), workload);
			hostList = IoHelper.createHostList(IoRandomConstants.NUMBER_OF_HOSTS);
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
			System.exit(0);
		}
	}

	public static String getWorkload() {
		return workload;
	}

	public static void setWorkload(String workload) {
		IoRandomRunner.workload = workload;
	}

}
