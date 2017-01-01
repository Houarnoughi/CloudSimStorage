package optim_storage_infrastructure;

import org.cloudbus.cloudsim.power.models.PowerModel;

/**
 * Implemented by Hamza Ouarnoughi to get the same results as Anton fgcs paper
 */


public abstract class IoPowerModelFgcsPower implements PowerModel {

	@Override
	public double getPower(double utilization) throws IllegalArgumentException {

		// Utilization must be between 0 and 1
		//System.out.println("Utilization "+utilization);
		if (utilization < 0 || utilization > 1) {
			throw new IllegalArgumentException(
					"Utilization value must be between 0 and 1");
		}
		if (utilization % 0.1 == 0) {
			return getPowerData((int) (utilization * 10));
		}
		int utilization1 = (int) Math.floor(utilization * 10);
		int utilization2 = (int) Math.ceil(utilization * 10);
		double power1 = getPowerData(utilization1);
		double power2 = getPowerData(utilization2);
		double delta = (power2 - power1) / 10;
		double power = power1 + delta
				* (utilization - (double) utilization1 / 10) * 100;
		return power;
	}

	/**
	 * Gets the power data.
	 *
	 * @param index
	 *            the index
	 * @return the power data
	 */
	protected abstract double getPowerData(int index);

}
