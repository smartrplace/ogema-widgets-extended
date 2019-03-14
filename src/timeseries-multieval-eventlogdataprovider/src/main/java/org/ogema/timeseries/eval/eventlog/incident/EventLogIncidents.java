package org.ogema.timeseries.eval.eventlog.incident;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.ogema.timeseries.eval.eventlog.incident.IncidentFilter;


/**
 * Keeps track of incidents found in the event log.
 * By default, incident types defined in addDefaultIncidents are searched and accounted for.
 * 
 * One instance is created per EvalCore, i.e. one instance is created for each Gateway and time frame
 * 
 * @author jruckel
 *
 */
public class EventLogIncidents {
	
	private List<EventLogIncidentType> types = new ArrayList<EventLogIncidentType>();
	
	private FileWriter fw;
	
	/**
	 * Example: timestamp of last incident, thus allowing for a cooldown on incident reporting
	 */
	public HashMap<String, Object> flags = new HashMap<>();
	
	public EventLogIncidents() {
		System.out.println("ELI created.");
		this.addDefaultTypes();
	}
	
	
	
	/**
	 * A type of incident e.g. a Homematic Error
	 * @author jruckel
	 *
	 */
	public class EventLogIncidentType {
		
		public String name;
		public String description;
		public String searchString;
		public IncidentFilter filter;
		public boolean reverseFilter;
		
		/** Whether or not do display the type on the KPI page */
		public boolean display;
		
		public IncidentCounter counter = new IncidentCounter();
		
		/**
		 * 
		 * @param name formerly ID
		 * @param description
		 * @param searchString String by which the incident can be found in the logfiles
		 */
		public EventLogIncidentType(String name, String description, String searchString) {
			
			this.name = name;
			this.description = description;
			this.searchString = searchString;
			this.filter = new IncidentFilter.AllPassFilter();
			this.reverseFilter = false;
			this.display = true;

		}
		
		
		/**
		 * Counts occurrences of an incident type per day
		 * @author jruckel
		 *
		 */
		public class IncidentCounter {
			/** Date --> Count */
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
			 * @return sum
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
	
	
	public List<EventLogIncidentType> getTypes() {
		return types;
	}
	
	public EventLogIncidentType getTypeByName(String name) {
		for(EventLogIncidentType t : types) {
			if (t.name.equals(name)) return t;
		}
		return new EventLogIncidentType("NULL", "n/a", "");
	}

	/**
	 * Add an incident type
	 * @param t
	 */
	public void addType(EventLogIncidentType t) {
		types.add(t);
	}
	
	/**
	 * adds the default types. run on construction.
	 * configure/add default incident types here
	 */
	private void addDefaultTypes() {
		
		/*
		 * Simple incidents without filters:
		 */
		types.add(new EventLogIncidentType("UPDSERVER_NOCON_EVENT", "n/a", "Error connecting to update server"));
		types.add(new EventLogIncidentType("TRANSFER_FAIL_HOMEMATIC", "n/a", "PING failed"));
		types.add(new EventLogIncidentType("OLD_BUNDLE", "n/a", "Inactive bundle found"));
		

		/*
		 * incidents with filters:
		 */
		EventLogIncidentType homematicErr = new EventLogIncidentType(
				"HOMEMATIC_ERR", "n/a", "discarding write to");
		homematicErr.filter = new IncidentFilter.CooldownFilter(flags, 60_000);
		types.add(homematicErr);
		
		EventLogIncidentType shutdownDB = new EventLogIncidentType(
				"SHUTDOWN_DB", "n/a", "Closing FendoDB data/slotsdb");
		/** This filter sets a flag to indicate a DB shutdown, but does not count the incident itself */
		shutdownDB.filter = new IncidentFilter.OccurrenceFlagFilter(flags, false);
		shutdownDB.display = false; // since this is only a "helper incident", no need to display on KPI page
		types.add(shutdownDB);
		
		EventLogIncidentType frameworkRestartClean = new EventLogIncidentType(
				"FW_RESTART_CLEAN", "n/a", "Flushing Data every: ");
		/** This filter checks whether the DB was shut down, indicating a clean shutdown */
		frameworkRestartClean.filter = new IncidentFilter.CheckOccurrenceFlagFilter(flags, "SHUTDOWN_DB");
		types.add(frameworkRestartClean);
		
		EventLogIncidentType frameworkRestartUnclean = new EventLogIncidentType(
				"FW_RESTART_UNCLEAN", "n/a", "Flushing Data every: ");
		/** This filter checks whether the DB was shut down, indicating a clean shutdown */
		frameworkRestartUnclean.filter = new IncidentFilter.CheckOccurrenceFlagFilter(flags, "SHUTDOWN_DB");
		frameworkRestartUnclean.reverseFilter = true;
		types.add(frameworkRestartUnclean);
	}
	
	/**
	 * Get total number of incidents across all types of incidents
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
	 * Write header row to CSV
	 * 
	 * TODO: Fix multiple headers being written
	 * writeCSVHeader() is called from EvalCore, which is created for each part (i.e. GW and Interval) of the
	 * Offline Evaluation. Thus, the CSV has duplicate header rows.
	 * Workaround: `sort -ur <File>.csv > <File>.unique.csv`
	 * 
	 * @return false on IOException
	 */
	public boolean writeCSVHeader() {
		
		List<String> cols = new ArrayList<String>();

		cols.add("Gateway");
		cols.add("startTime");
		cols.add("endTime");
		
		for(EventLogIncidentType t : types ) {
			cols.add(t.name);
		}

		return writeCSVCols(cols);
	}

	/**
	 * Write a row of data i.e. the sums of each type of incident for a Gateway (gwId) 
	 * in a time frame (startTime - endTime).
	 * 
	 * @param gwId
	 * @param startTime
	 * @param endTime
	 * @return false on IOException
	 */
	public boolean writeCSVRow(String gwId, long startTime, long endTime) {
		
		List<String> cols = new ArrayList<String>();
		
		cols.add(gwId);
		cols.add(String.valueOf(startTime));
		cols.add(String.valueOf(endTime));
		
		for(EventLogIncidentType t : types ) {
			cols.add(
					String.valueOf(t.counter.getSum())
					);
		}

		return writeCSVCols(cols);

	}
	
	/**
	 * Write columns to the CSV output
	 * @param cols
	 * @return false if an IOException occurred
	 */
	private boolean writeCSVCols(List<String> cols) {
		
		String line = "";
		for(String col : cols) {
			line += col + ",";
		}
		line = line.substring(0, line.length() - 1) + "\n";
		
			
		try {
			createCSVFile();
			fw.append(line);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	
		return true;
	}
	
	/**
	 * Create / open the CSV output file
	 * @throws IOException
	 */
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