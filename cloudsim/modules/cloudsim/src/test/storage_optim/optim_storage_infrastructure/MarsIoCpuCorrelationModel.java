package optim_storage_infrastructure;

import org.cloudbus.cloudsim.Storage;

public class MarsIoCpuCorrelationModel {
	
	public double getCpuLoad(Storage device, IoVm vm, double time) {
		double load = 0.0;
		IoWorkloadModel ioModel = vm.getIoWorkloadModel();
		
		// The variables used in the MARS model
		 double _iops = 1/(ioModel.getArrivalRate(time));
		 double io_size = ioModel.getIoSize(time);
		 double wrt_rate = (1-ioModel.getReadRate(time));
		 int dev_var = (device instanceof IoHarddriveStorage)? 0 : 1; 
		 
		 // The intercept coefficients got from the Mars Earth
		 double intercept = 0.141215;
		 
		 // Calculate the load using the obtained MARS model
		 load = intercept + 
				 (254.358 * h(_iops,0.00027972)) +
				 (9.48678 * h(_iops,0.00384246)) +
				 (-1156.84 * h(_iops,0.0000718817)) +
				 (3536.46 * h(0.0000718817,_iops)) +
				 (0.0231263 * dev_var) +
				 (92.7441 * h(0.000637247,_iops)) +
				 (892.445 * h(_iops,0.000119005)) +
				 (0.0110173 * (io_size/1024)) +
				 (0.00773367 * wrt_rate);
		 
		 return load;
	}
	
	/**
	 * The Hinge Loss function
	 * @param x 
	 * @param t
	 * @return 0 if x<t else x-t
	 */
	private double h(double x, double t) {
		return Math.max(0, (x-t));
	}

}
