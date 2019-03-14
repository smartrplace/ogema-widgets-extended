package org.ogema.timeseries.eval.eventlog.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.ogema.timeseries.eval.eventlog.incident.EventLogIncidents;
import org.ogema.timeseries.eval.eventlog.incident.EventLogIncidents.EventLogIncidentType;
import org.ogema.timeseries.eval.eventlog.util.EventLogFileParser;
import org.ogema.tools.resource.util.TimeUtils;
import org.slf4j.Logger;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;


public class EventLogFileParserFirst implements EventLogFileParser {
	
	private EventLogIncidents eli;

	protected final Logger log;
	protected final String gwId;

	private PrintWriter pw;

	
	public EventLogFileParserFirst(Logger logger, String gwId, EventLogIncidents eli) {
		this.log = logger;
		this.gwId = gwId;
		this.eli = eli;
	}
	
	
	@Override
	public List<EventLogResult> parseLogFile(InputStream logFileStream, List<String> eventIds, long dayStart) 
			throws IOException {
		
		List<EventLogIncidentType> incidentTypes = eli.getTypes();
		List<EventLogResult> result = new ArrayList<>();
		BufferedReader br = new BufferedReader(new InputStreamReader(logFileStream));

		File dir = new File("EventLogEvaluationResults");
		dir.mkdirs();
		String fileName = new SimpleDateFormat("yyyy-MM-dd'.txt'").format(new Date());
		File file = new File(dir, "EventLog_"+ fileName);
		file.createNewFile();
		pw = new PrintWriter(new FileWriter(file, true));
		
		while(true) {
			String line = br.readLine();
			if(line == null) break;
			try {
				String trim = line.trim();
				if(trim.startsWith("#")) continue;
				if(trim.isEmpty()) continue;
				for (EventLogIncidentType i : incidentTypes) {
					if (checkEvent(trim, i, true, result, dayStart)) break;
				}

			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		br.close();
		pw.close();
			
		return result;
	}
	
	
	/** List of eventIds known to the parsing provider*/
	@Override
	public List<String> supportedEventIds() {

		List<String> supported = new ArrayList<String>();
		
		eli.getTypes().forEach(t -> {
			supported.add(t.name);
		});
		
		return supported;
		
	}

	
	@Override
	public String id() {
		return this.getClass().getSimpleName();
	}

	
	@Override
	public String label(OgemaLocale locale) {
		return "Initial Event Log File parser, e.g. searching for framework restart events";
	}
	
	/*
	 * *
	 * Check if event is in log file line and perform reporting if so
	 * 
	 * @param trim
	 * @param incidentType
	 * @param doLog
	 * @param result
	 * @param dayStart
	 * @return
	 * @throws IOException
	 */
	public boolean checkEvent(String trim, EventLogIncidentType incidentType, boolean doLog, 
			List<EventLogResult> result, long dayStart) throws IOException {
			
		boolean eventFound = checkEvent(trim, incidentType.searchString, incidentType.name, doLog, 
				result, dayStart, incidentType);
		
		if (eventFound) {
			String date = new SimpleDateFormat("yyyy-MM-dd'.txt'").format(dayStart);
			incidentType.counter.increment(date);
		}
		
		return eventFound;
	
	}

	
	
	/**
	 * 
	 * @param line 
	 * @param searchString string to search for
	 * @param eventName name/identifier of the event
	 * @param doLog
	 * @param result
	 * @param dayStart
	 * @param iType
	 * @return true if an event was found (no further checking for other events required)
	 * @throws IOException
	 */
	public boolean checkEvent(String line, String searchString, String eventName, boolean doLog,  
			List<EventLogResult> result, long dayStart, EventLogIncidentType iType) throws IOException {
		
		if(!line.contains(searchString)) return false;
		
		EventLogResult elr = new EventLogResult();
		if(line.length() > 12) {
			SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			String timeString = line.substring(0, 12);
			try {
			    Date parsed = format.parse(timeString);  
			    elr.eventTime = dayStart + parsed.getTime();
			} catch (ParseException pe) {
			    log.debug("ERROR: Cannot parse \"" + timeString + "\"");
			    return true;
			}
		} else {
			log.debug(" !!!!!!!! No time string in line:"+line);
			return true;
		}
		
		elr.eventId = eventName;
		elr.fullEventString = line;
		elr.gatewayId = gwId;
		elr.eventMessage = gwId+"#"+TimeUtils.getDateAndTimeString(elr.eventTime)+" : "+eventName;
 		
 		
		/**
		 * @return false if the filter has determined not to count the incident
		 */
		if (! iType.filter.exec(elr) ^ iType.reverseFilter) {
			return false;
		}
 		
 		if(elr.eventMessage != null) {
			log.debug(elr.eventMessage);
			pw.append(elr.eventMessage+"\r\n");
 		}
 		
		result.add(elr);
		return true;
 	}
}