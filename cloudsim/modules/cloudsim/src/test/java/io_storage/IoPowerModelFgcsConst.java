package io_storage;

/**
 * Implemented by Hamza Ouarnoughi to get the same results as Anton fgcs paper
 * 
 * This Model will be extended to include the storage cost model
 */

public class IoPowerModelFgcsConst extends IoPowerModelFgcsPower {

	private final double pmax = 90;
	private final double pmin = 50;

	@Override
	protected double getPowerData(int index) {
		/*
		 * P(u) = k · Pmax + (1−k)·Pmax · u
		 *
		 * Pmax -> Power on 100% cpu Pmin -> Power on 0% cpu k = (Pmin X 100)
		 * /Pmax u -> CPU utilization
		 */
		double utilization = index / 10.0;
		double k = pmin / pmax;

		double power = (k * pmax) + (1 - k) * pmax * utilization;
		return power;
	}

}
