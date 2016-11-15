package io_storage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.cloudbus.cloudsim.Log;


/**
 * This model uses trace files to simulate IO workload utilization
 * This model will be associated to one or multiple VMs
 * @author hamza
 *
 */

public class IoWorkloadModelInMemory extends IoWorkloadModel {
	
	private String ioFilePath;
	private double schedulingInterval;
	private double dataSimple;
	
	/** Trace file fields */
	private final double[] volume;
	private final double[] readRate;
	private final double[] seqRate;
	private final int[] ioSize;
	private final int[] ioPerSec;
	
	public IoWorkloadModelInMemory(String IofilePath, double schedulingInterval, int dataSample)
			throws NumberFormatException,
			IOException {
		
		setIoFilePath(IofilePath);
		setSchedulingInterval(schedulingInterval);
		setDataSimple(dataSample);
		
		// Hamza: create an init array for each field
		volume = new double[dataSample];
		readRate = new double[dataSample];
		seqRate = new double[dataSample];
		ioSize = new int[dataSample];
		ioPerSec = new int[dataSample];
		
		// Hamza : Fill tables
		FileReader fr = new FileReader(IofilePath);
		BufferedReader br = new BufferedReader(fr);
		
		int n = volume.length;
		for (int i = 0; i < n - 1; i++) {
			String[] arrayLine = br.readLine().split("\\,", -1);
			//System.out.println("Hamza: je suis la !!!!");
			// Hamza: we have 5 characteristics of IO workload  
			if (arrayLine.length >= 5) {
				//System.out.println("Hamza: je suis la !!!!");
				volume[i] = Double.parseDouble(arrayLine[0]);
				readRate[i] = Double.parseDouble(arrayLine[1]);
				seqRate[i] = Double.parseDouble(arrayLine[2]);
				ioSize[i] = Integer.parseInt(arrayLine[3]);
				ioPerSec[i] = Integer.parseInt(arrayLine[4]);
			}
		}
		volume[n-1] = volume[n-2];
		readRate[n-1] = readRate[n-2];
		seqRate[n-1] = seqRate[n-2];
		ioSize[n-1] = ioSize[n-2];
		ioPerSec[n-1] = ioPerSec[n-2];
		br.close();
	}
	
	
	@Override
	public double getVolume(double time) {
		double retVolume = 0;
		
		if (time % getSchedulingInterval() == 0) {
			return volume[(int)time / (int)getSchedulingInterval()];
		}
		
		int time1 = (int) (Math.floor(time / getSchedulingInterval())%getDataSimple());
		int time2 = (int) (Math.ceil(time / getSchedulingInterval())%getDataSimple());
		double vol1 = volume[time1];
		double vol2 = volume[time2];
		double delta = (vol2 - vol1) / ((time2 - time1) * getSchedulingInterval());
		retVolume = vol1 + delta * ((time - time1) * getSchedulingInterval());
		
		Log.printLine("Hamza: getVolume() "+ retVolume);
		return retVolume;
	}
	
	@Override
	public double getReadRate(double time) {
		double retreadRate = 0;
		
		if (time % getSchedulingInterval() == 0) {
			return readRate[(int)time / (int)getSchedulingInterval()];
		}
		
		int time1 = (int) (Math.floor(time / getSchedulingInterval())%getDataSimple());
		int time2 = (int) (Math.ceil(time / getSchedulingInterval())%getDataSimple());
		double rrate1 = readRate[time1];
		double rrate2 = readRate[time2];
		double delta = (rrate2 - rrate1) / ((time2 - time1) * getSchedulingInterval());
		retreadRate = rrate1 + delta * ((time2 - time1) * getSchedulingInterval());
		
		return retreadRate;
	}
	
	@Override
	public double getRandomRate(double time) {
		double retRandRate = 0;
		
		if (time % getSchedulingInterval() == 0) {
			return seqRate[(int)time / (int)getSchedulingInterval()];
		}
		
		int time1 = (int) (Math.floor(time / getSchedulingInterval())%getDataSimple());
		int time2 = (int) (Math.ceil(time / getSchedulingInterval())%getDataSimple());
		double seq1 = seqRate[time1];
		double seq2 = seqRate[time2];
		double delta = (seq2 - seq1) / ((time2 - time1) * getSchedulingInterval());
		double seqRate = seq1 + delta * ((time2 - time1) * getSchedulingInterval());
		retRandRate = 1 - seqRate;
		return retRandRate;
	}
	
	@Override
	public int getIoSize(double time) {
		int retIoSize = 0;
		
		if (time % getSchedulingInterval() == 0) {
			return ioSize[(int)time / (int)getSchedulingInterval()];
		}
		
		int time1 = (int) (Math.floor(time / getSchedulingInterval())%getDataSimple());
		int time2 = (int) (Math.ceil(time / getSchedulingInterval())%getDataSimple());
		int io1 = ioSize[time1];
		int io2 = ioSize[time2];
		double delta = (io2 - io1) / ((time2 - time1) * getSchedulingInterval());
		Double d = new Double(Math.ceil(io1 + delta * ((time2 - time1) * getSchedulingInterval())));
		retIoSize = d.intValue() ;
		
		return retIoSize;
	}
	
	@Override
	public int getArrivalRate(double time) {
		int retIoPerSec = 0;
		
		if (time % getSchedulingInterval() == 0) {
			return ioPerSec[(int)time / (int)getSchedulingInterval()];
		}
		
		int time1 = (int) (Math.floor(time / getSchedulingInterval())%getDataSimple());
		int time2 = (int) (Math.ceil(time / getSchedulingInterval())%getDataSimple());
		int ioPer1 = ioPerSec[time1];
		int ioPer2 = ioPerSec[time2];
		double delta = (ioPer2 - ioPer1) / ((time2 - time1) * getSchedulingInterval());
		Double d = new Double(Math.ceil(ioPer1 + delta * (time2 - time1 * getSchedulingInterval())));
		retIoPerSec = d.intValue() ;
		
		
		return retIoPerSec;
	}
	
	public String getIoFilePath() {
		return ioFilePath;
	}

	public void setIoFilePath(String ioFilePath) {
		this.ioFilePath = ioFilePath;
	}


	public double getSchedulingInterval() {
		return schedulingInterval;
	}


	public void setSchedulingInterval(double schedulingInterval) {
		this.schedulingInterval = schedulingInterval;
	}


	public double getDataSimple() {
		return dataSimple;
	}


	public void setDataSimple(double dataSimple) {
		this.dataSimple = dataSimple;
	}

}
