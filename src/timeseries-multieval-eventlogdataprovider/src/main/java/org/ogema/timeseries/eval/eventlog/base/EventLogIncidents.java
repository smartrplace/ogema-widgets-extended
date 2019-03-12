package org.ogema.timeseries.eval.eventlog.base;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


/**
 * Keeps track of incidents found in the event log.
 * By default, incidents defined in addDefaultIncidents are searched and accounted for.
 * 
 * One instance per GW.
 * 
 * @author jruckel
 *
 */
public class EventLogIncidents {
	
	private List<EventLogIncidentType> types = new ArrayList<EventLogIncidentType>();
	
	private FileWriter fw;
	
	public EventLogIncidents() {
		System.out.println("ELI created.");
		this.addDefaultTypes();
		
		try {
			this.createCSVFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * A type of incident e. g. a Homematic Error
	 * @author jruckel
	 *
	 */
	public class EventLogIncidentType {
		
		public String name;
		public String description;
		public String searchString;
		
		public IncidentCounter counter = new IncidentCounter();
		
		/**
		 * 
		 * @param name
		 * @param description
		 * @param searchString String by which the incident can be found in the logfiles
		 */
		public EventLogIncidentType(String name, String description, String searchString) {
			this.name = name;
			this.description = description;
			this.searchString = searchString;

		}
		
		
		/**
		 * Counts occurences of an incident type per day
		 * @author jruckel
		 *
		 */
		public class IncidentCounter {
			// Date --> Count
			private HashMap<String, Integer> count = new HashMap<String, Integer>();
			
			/**
			 * Increment incident count for a given date
			 * @param date
			 */
			public void increment(String date) {
				if (! count.containsKey(date)) {
					count.put(date, 1);
				}
				else {
					Integer prev = count.get(date);
					count.put(date, prev + 1);
				}
			}
			
			/**
			 * Get number of incidents across all dates processed
			 * @return
			 */
			public int getSum() {
				int sum = 0;
				for (Integer i : count.values()) {
					sum += i;
				}
				return sum;
			}
			
		}
		
	}
	
	/**
	 * 
	 * @return
	 */
	public List<EventLogIncidentType> getTypes() {
		return types;
	}

	/**
	 * Add an incident type
	 * @param t
	 */
	public void addType(EventLogIncidentType t) {
		types.add(t);
	}
	
	private void addDefaultTypes() {
		types.add(new EventLogIncidentType("HomematicFehler", "n/a", "discarding write to"));
		types.add(new EventLogIncidentType("FrameworkRestart", "n/a", "Flushing Data every: "));
		types.add(new EventLogIncidentType("UPDSERVER_NOCON_EVENT", "n/a", "Error connecting to update server"));
		types.add(new EventLogIncidentType("TRANSFER_FAIL_HOMEMATIC", "n/a", "PING failed"));
		types.add(new EventLogIncidentType("OLD_BUNDLE", "n/a", "Inactive bundle found"));
		types.add(new EventLogIncidentType("SHUTDOWN_DB", "n/a", "Closing FendoDB data/slotsdb"));

		
	}
	
	/**
	 * Get number of incidents across all processed days and types of incident
	 * @return
	 */
	public int getTotalIncidents() {
		int sum = 0;
		for (EventLogIncidentType t : types) {
			sum += t.counter.getSum();
		}
		return sum;
	}
	
	/**
	 * Dump some basic stats to console
	 */
	public void dumpStats() {
		System.out.println("Dumping EventLog stats:");
		for(EventLogIncidentType t : types) {
			System.out.println(t.name + " has occured a total of " + t.counter.getSum() + " times.");
		}
	}

	
	public void writeCSVHeader() throws IOException {
		
		createCSVFile();
		
		fw.append("Gateway,");
		for(EventLogIncidentType t : types ) {
			fw.append(t.name + ",");
		}
		fw.append("\n");
		
		fw.close();
	}

	public void writeCSVRow(String gwId) throws IOException {
		
		createCSVFile();
		
		fw.append(gwId + ",");
		for(EventLogIncidentType t : types ) {
			fw.append(t.counter.getSum() + ",");
		}
		fw.append("\n");
		
		fw.close();
	}
	
	public void createCSVFile() throws IOException {
		File dir = new File("EventLogEvaluationResults");
		dir.mkdirs();
		String fileName = new SimpleDateFormat("yyyy-MM-dd'.csv'").format(new Date());
		File file = new File(dir, "EventLog_"+ fileName);
		file.createNewFile();
		fw = new FileWriter(file, true);
		
	}
	public void closeCSVFile() throws IOException {
		fw.close();
	}
	
	
}