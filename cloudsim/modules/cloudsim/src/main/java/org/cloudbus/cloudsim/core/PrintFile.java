package org.cloudbus.cloudsim.core;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Anup
 * 
 */
public class PrintFile {

	public static String file_name = "";

	public static void AddtoFile(String msg) {
		try {
			//java.util.Date d = new java.util.Date();
			if (file_name == "") {
				file_name = "/home/hamza/CloudSimStorage/cloudsim/modules/cloudsim-examples/output/events/events.txt";
			}
			File file = new File(file_name);
			// if file doesnt exists, then create it
			if (!file.exists()) {

				file.createNewFile();
				FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
				fw.write("#EventType;EventTag;Source;Destination;Time;Endwaiting\n");
				fw.close();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			//String text = System.lineSeparator()
			//		+ msg.replace("\n", System.lineSeparator());
			fw.write(msg);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
