package thesis_experiments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import optim_storage_allocation_policy.IoVmAllocationPolicy;
import optim_storage_allocation_policy.IoVmAllocationPolicyGreedyMinStorageCost;
import optim_storage_allocation_policy.IoVmAllocationPolicyMigrationAbstract;
import optim_storage_allocation_policy.IoVmAllocationPolicyMigrationInterQuartileRange;
import optim_storage_allocation_policy.IoVmAllocationPolicyMigrationLocalRegression;
import optim_storage_allocation_policy.IoVmAllocationPolicyMigrationLocalRegressionRobust;
import optim_storage_allocation_policy.IoVmAllocationPolicyMigrationMedianAbsoluteDeviation;
import optim_storage_allocation_policy.IoVmAllocationPolicyMigrationStaticIopsThreshold;
import optim_storage_allocation_policy.IoVmAllocationPolicyMigrationStaticThreshold;
import optim_storage_allocation_policy.IoVmAllocationPolicyMinCostSolutionEnum;
import optim_storage_allocation_policy.IoVmAllocationPolicySimple;
import optim_storage_infrastructure.IoDataCenter;
import optim_storage_infrastructure.IoHost;
import optim_storage_infrastructure.IoVm;
import optim_storage_selection_policy.IoVmSelectionPolicy;
import optim_storage_selection_policy.IoVmSelectionPolicyMaximumCorrelation;
import optim_storage_selection_policy.IoVmSelectionPolicyMinimumCpuUtilization;
import optim_storage_selection_policy.IoVmSelectionPolicyMinimumIopsUtilization;
import optim_storage_selection_policy.IoVmSelectionPolicyMinimumMigrationTime;
import optim_storage_selection_policy.IoVmSelectionPolicyMinimumStorageMigrationTime;
import optim_storage_selection_policy.IoVmSelectionPolicyRandomSelection;

/**
 * The Class RunnerAbstract.
 *
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
 */
public abstract class IoRunnerAbstract {

	/** The enable output. */
	private static boolean enableOutput;

	/** The broker. */
	protected static DatacenterBroker broker;

	/** The cloudlet list. */
	protected static List<Cloudlet> cloudletList;

	/** The vm list. */
	protected static List<IoVm> vmList;

	/** The host list. */
	protected static List<IoHost> hostList;

	/**
	 * Run.
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
	public IoRunnerAbstract(boolean enableOutput, boolean outputToFile,
			String inputFolder, String outputFolder, String workload,
			String ioVmAllocationPolicy, String ioVmSelectionPolicy,
			String parameter,
			String maxThe,
			String minThr) {
		try {
			initLogOutput(enableOutput, outputToFile, outputFolder, workload,
					ioVmAllocationPolicy, ioVmSelectionPolicy, //parameter
					maxThe,
					minThr);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		init(inputFolder, workload);
		start(getExperimentName(workload, ioVmAllocationPolicy,
				ioVmSelectionPolicy, //parameter
				maxThe,
				minThr),
				outputFolder,
				getVmAllocationPolicy(ioVmAllocationPolicy, ioVmSelectionPolicy,
						parameter,
						maxThe,
						minThr));
	}

	/**
	 * Gets the experiment name.
	 *
	 * @param args
	 *            the args
	 * @return the experiment name
	 */
	protected String getExperimentName(String... args) {
		StringBuilder experimentName = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			if (args[i].isEmpty()) {
				continue;
			}
			if (i != 0) {
				experimentName.append("_");
			}
			experimentName.append(args[i]);
		}
		return experimentName.toString();
	}

	/**
	 * Gets the vm allocation policy.
	 *
	 * @param vmAllocationPolicyName
	 *            the vm allocation policy name
	 * @param vmSelectionPolicyName
	 *            the vm selection policy name
	 * @param parameterName
	 *            the parameter name
	 * @return the vm allocation policy
	 */
	protected IoVmAllocationPolicy getVmAllocationPolicy(
			String ioVmAllocationPolicyName, String ioVmSelectionPolicyName,
			String parameterName,
			String maxThr, 
			String minThr) {
		IoVmAllocationPolicy ioVmAllocationPolicy = null;
		IoVmSelectionPolicy ioVmSelectionPolicy = null;
		if (!ioVmSelectionPolicyName.isEmpty()) {
			ioVmSelectionPolicy = getVmSelectionPolicy(ioVmSelectionPolicyName);
		}
		
		double parameter = 0.0;
		if (!parameterName.isEmpty()) {
			parameter = Double.valueOf(parameterName);
		}
		
		double max = 0;
		if (!maxThr.isEmpty()) {
			max = Double.valueOf(maxThr);
		}
		
		double min = 0;
		if (!minThr.isEmpty()) {
			min = Double.valueOf(minThr);
		}
		
		if (ioVmAllocationPolicyName.equals("iqr")) {
			IoVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy = new IoVmAllocationPolicyMigrationStaticThreshold(
					hostList, ioVmSelectionPolicy, 
					max,
					min,
					0.7);
			ioVmAllocationPolicy = new IoVmAllocationPolicyMigrationInterQuartileRange(
					hostList, ioVmSelectionPolicy, 
					parameter,
					max,
					min,
					fallbackVmSelectionPolicy);
		} else if (ioVmAllocationPolicyName.equals("mad")) {
			IoVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy = new IoVmAllocationPolicyMigrationStaticThreshold(
					hostList, ioVmSelectionPolicy,
					max,
					min,
					0.7);
			ioVmAllocationPolicy = new IoVmAllocationPolicyMigrationMedianAbsoluteDeviation(
					hostList, ioVmSelectionPolicy, parameter,
					max,
					min,
					fallbackVmSelectionPolicy);
		} else if (ioVmAllocationPolicyName.equals("lr")) {
			IoVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy = new IoVmAllocationPolicyMigrationStaticThreshold(
					hostList, ioVmSelectionPolicy, 
					max,
					min,
					0.7);
			ioVmAllocationPolicy = new IoVmAllocationPolicyMigrationLocalRegression(
					hostList, ioVmSelectionPolicy, parameter,
					IoConstants.SCHEDULING_INTERVAL, fallbackVmSelectionPolicy);
		} else if (ioVmAllocationPolicyName.equals("lrr")) {
			IoVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy = new IoVmAllocationPolicyMigrationStaticThreshold(
					hostList, ioVmSelectionPolicy, 
					max,
					min,
					0.7);
			ioVmAllocationPolicy = new IoVmAllocationPolicyMigrationLocalRegressionRobust(
					hostList, ioVmSelectionPolicy, parameter,
					IoConstants.SCHEDULING_INTERVAL, fallbackVmSelectionPolicy);
		} else if (ioVmAllocationPolicyName.equals("thr")) {
			ioVmAllocationPolicy = new IoVmAllocationPolicyMigrationStaticThreshold(
					hostList, ioVmSelectionPolicy, 
					max,
					min,
					parameter);
		} else if (ioVmAllocationPolicyName.equals("dvfs")) {
			ioVmAllocationPolicy = new IoVmAllocationPolicySimple(hostList);
		//////// Storage Allocation optimization Policies //////////////
		} else if (ioVmAllocationPolicyName.equals("iopsthr")) {
			ioVmAllocationPolicy = new IoVmAllocationPolicyMigrationStaticIopsThreshold(
					hostList, ioVmSelectionPolicy, parameter);
		} else if (ioVmAllocationPolicyName.equals("greedyMinStrgCost")) {
			ioVmAllocationPolicy = new IoVmAllocationPolicyGreedyMinStorageCost(hostList, 
					ioVmSelectionPolicy,
					max,
					min,
					parameter);
		} else if (ioVmAllocationPolicyName.equals("BFMC")) {
			ioVmAllocationPolicy = new IoVmAllocationPolicyMinCostSolutionEnum(hostList, 
					ioVmSelectionPolicy,
					max,
					min,
					parameter);
		} else {
			System.out.println("Unknown VM allocation policy: "
					+ ioVmAllocationPolicyName);
			System.exit(0);
		}
		return ioVmAllocationPolicy;
	}

	/**
	 * Gets the vm selection policy.
	 *
	 * @param vmSelectionPolicyName
	 *            the vm selection policy name
	 * @return the vm selection policy
	 */
	protected IoVmSelectionPolicy getVmSelectionPolicy(
			String ioVmSelectionPolicyName) {
		IoVmSelectionPolicy vmSelectionPolicy = null;
		if (ioVmSelectionPolicyName.equals("mc")) {
			vmSelectionPolicy = new IoVmSelectionPolicyMaximumCorrelation(
					new IoVmSelectionPolicyMinimumMigrationTime());
		} else if (ioVmSelectionPolicyName.equals("mmt")) {
			vmSelectionPolicy = new IoVmSelectionPolicyMinimumMigrationTime();
		} else if (ioVmSelectionPolicyName.equals("mu")) {
			vmSelectionPolicy = new IoVmSelectionPolicyMinimumCpuUtilization();
		} else if (ioVmSelectionPolicyName.equals("rs")) {
			vmSelectionPolicy = new IoVmSelectionPolicyRandomSelection();
		////////////// Storage Selection optimization Policies //////////////
		} else if (ioVmSelectionPolicyName.equals("miopsu")) {
			vmSelectionPolicy = new IoVmSelectionPolicyMinimumIopsUtilization();
		} else if (ioVmSelectionPolicyName.equals("mstrgmt")) {
			vmSelectionPolicy = new IoVmSelectionPolicyMinimumStorageMigrationTime();
		} else {
			System.out.println("Unknown VM selection policy: "
					+ ioVmSelectionPolicyName);
			System.exit(0);
		}
		return vmSelectionPolicy;
	}

	/**
	 * Inits the simulation.
	 *
	 * @param inputFolder
	 *            the input folder
	 */
	protected abstract void init(String inputFolder, String workload);

	/**
	 * Inits the log output.
	 *
	 * @param enableOutput
	 *            the enable output
	 * @param outputToFile
	 *            the output to file
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
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws FileNotFoundException
	 *             the file not found exception
	 */
	protected void initLogOutput(boolean enableOutput, boolean outputToFile,
			String outputFolder, String workload, String vmAllocationPolicy,
			String vmSelectionPolicy,
			// String parameter
			String maxThr, String minThr) throws IOException,
			FileNotFoundException {
		setEnableOutput(enableOutput);
		Log.setDisabled(!isEnableOutput());
		if (isEnableOutput() && outputToFile) {
			File folder = new File(outputFolder);
			if (!folder.exists()) {
				folder.mkdir();
			}

			File folder2 = new File(outputFolder + "/log");
			if (!folder2.exists()) {
				folder2.mkdir();
			}

			File file = new File(outputFolder
					+ "/log/"
					+ getExperimentName(workload, vmAllocationPolicy,
							vmSelectionPolicy, //parameter
							maxThr, 
							minThr) + ".txt");
			file.createNewFile();
			Log.setOutput(new FileOutputStream(file));
		}
	}

	/**
	 * Checks if is enable output.
	 *
	 * @return true, if is enable output
	 */
	public boolean isEnableOutput() {
		return enableOutput;
	}

	/**
	 * Sets the enable output.
	 *
	 * @param enableOutput
	 *            the new enable output
	 */
	public void setEnableOutput(boolean enableOutput) {
		IoRunnerAbstract.enableOutput = enableOutput;
	}

	/**
	 * Starts the simulation.
	 *
	 * @param experimentName
	 *            the experiment name
	 * @param outputFolder
	 *            the output folder
	 * @param vmAllocationPolicy
	 *            the vm allocation policy
	 */
	protected void start(String experimentName, String outputFolder,
			IoVmAllocationPolicy vmAllocationPolicy) {
		System.out.println("Starting " + experimentName);

		try {
			IoDataCenter datacenter = (IoDataCenter) IoHelper
					.createDatacenter("Datacenter", IoDataCenter.class,
							hostList, vmAllocationPolicy);

			datacenter.setDisableMigrations(false);

			broker.submitVmList(vmList);
			broker.submitCloudletList(cloudletList);

			//CloudSim.terminateSimulation(Constants.SIMULATION_LIMIT);
			CloudSim.terminateSimulation();
			double lastClock = CloudSim.startSimulation();

			List<Cloudlet> newList = broker.getCloudletReceivedList();
			Log.printLine("Received " + newList.size() + " cloudlets");

			CloudSim.stopSimulation();

			IoHelper.printResults(datacenter, vmList, lastClock, experimentName,
					IoConstants.OUTPUT_CSV, outputFolder);

		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
			System.exit(0);
		}

		Log.printLine("Finished " + experimentName);
	}

}
