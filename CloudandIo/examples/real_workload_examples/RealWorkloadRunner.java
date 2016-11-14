package real_workload_examples;

import java.util.Calendar;

import org.cloudbus.cloudsim.Log;

import org.cloudbus.cloudsim.core.CloudSim;

import enery_example.Helper;
import enery_example.RunnerAbstract;

/**
 * The example runner for the PlanetLab workload.
 *
 * If you are using any algorithms, policies or workload included in the power
 * package please cite the following paper:
 *
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic
 * Algorithms and Adaptive Heuristics for Energy and Performance Efficient
 * Dynamic Consolidation of Virtual Machines in Cloud Data Centers", Concurrency
 * and Computation: Practice and Experience (CCPE), Volume 24, Issue 13, Pages:
 * 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 *
 * @author Anton Beloglazov
 * @since Jan 5, 2012
 */
public class RealWorkloadRunner extends RunnerAbstract {

	/**
	 * Instantiates a new planet lab runner.
	 *
	 * @param enableOutput
	 *            the enable output
	 * @param outputToFile
	 *            the output to file
	 * @param inputFolder
	 *            the input folder
	 * @param outputFolder
	 *            the output folder
	 * @param workload
	 *            the workload
	 * @param vmAllocationPolicy
	 *            the vm allocation policy
	 * @param vmSelectionPolicy
	 *            the vm selection policy
	 * @param parameter
	 *            the parameter
	 */
	public RealWorkloadRunner(boolean enableOutput, boolean outputToFile,
			String inputFolder, String outputFolder, String workload,
			String vmAllocationPolicy, String vmSelectionPolicy,
			String parameter) {
		super(enableOutput, outputToFile, inputFolder, outputFolder, workload,
				vmAllocationPolicy, vmSelectionPolicy, parameter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudbus.cloudsim.examples.power.RunnerAbstract#init(java.lang.String
	 * )
	 */
	@Override
	protected void init(String inputFolder) {
		try {
			CloudSim.init(1, Calendar.getInstance(), false);
			
			// Hamza: for debug
			//Log.printLine("RealWorkloadRunner "+ inputFolder);
			
			broker = Helper.createBroker();
			int brokerId = broker.getId();

			cloudletList = RealWorkloadHelper.createCloudletListRealWorkload(
					brokerId, inputFolder);
			vmList = Helper.createVmList(brokerId, cloudletList.size());
			hostList = Helper
					.createHostList(RealWorkloadConstants.NUMBER_OF_HOSTS);
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
			System.exit(0);
		}
	}

}
