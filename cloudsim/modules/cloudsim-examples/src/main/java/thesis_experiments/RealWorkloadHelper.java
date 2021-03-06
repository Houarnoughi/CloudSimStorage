package thesis_experiments;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
//import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelNull;

import optim_storage_infrastructure.IoUtilizationModelRealWorkloadInMemory;

/**
 * A helper class for the running examples for the PlanetLab workload.
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
public class RealWorkloadHelper {

	/**
	 * Creates the cloudlet list planet lab.
	 *
	 * @param brokerId
	 *            the broker id
	 * @param inputFolderName
	 *            the input folder name
	 * @return the list
	 * @throws FileNotFoundException
	 *             the file not found exception
	 */
	public static List<Cloudlet> createCloudletListRealWorkload(int brokerId,
			String inputFolderName) throws FileNotFoundException {
		List<Cloudlet> list = new ArrayList<Cloudlet>();

		long fileSize = 300;
		long outputSize = 300;
		UtilizationModel utilizationModelNull = new UtilizationModelNull();
		
		// Hamza : for debug
		File inputFolder = new File(inputFolderName);
		File[] files = inputFolder.listFiles();
		
		//System.out.println("RealWorkloadHelper : "+inputFolderName);
		//for (int i = 0; i < files.length; i++) {
		for (int i = 0; i < IoRandomConstants.NUMBER_OF_VMS; i++) {
			Cloudlet cloudlet = null;
			try {
				cloudlet = new Cloudlet(i, IoConstants.CLOUDLET_LENGTH,
						IoConstants.CLOUDLET_PES, fileSize, outputSize,
						new IoUtilizationModelRealWorkloadInMemory(
								files[i]
								.getAbsolutePath(),
								IoConstants.SCHEDULING_INTERVAL, 10),
						utilizationModelNull, utilizationModelNull);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			cloudlet.setUserId(brokerId);
			cloudlet.setVmId(i);
			list.add(cloudlet);
		}

		return list;
	}

}
