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

import org.ogema.timeseries.eval.eventlog.base.EventLogIncidents.EventLogIncidentType;
import org.ogema.timeseries.eval.eventlog.util.EventLogFileParser;
import org.ogema.tools.resource.util.TimeUtils;
import org.slf4j.Logger;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;


public class EventLogFileParserFirst implements EventLogFileParser {
	
	private EventLogIncidents eli;

	public static final String RESTART_EVENT = "FrameworkRestart";
	public static final String UPDSERVER_NOCON_EVENT = "UpdateServerNoConnection";
	public static final String HOMEMATIC = "HomematicFehler";
	public static final String TRANSFER_FAIL_HOMEMATIC = "NoHomematicData";
	public static final String OLD_BUNDLE = "inactiveBundle";
	public static final String SHUTDOWN_DB = "device itself shutdowned and restarted";
//	public static final String STOP_CHANNEL_MAN = "device itself but not shutdowned before";
//	public static final String CONNECTION_FAILED_FWRESTART = "device itself but not shutdowned before";//"ServerConnector stopped"; 
//	public static final String BUNDLEAPP_INIT = "device itself but not shutdowned before";//"RemoteConnector stopped"; 
	protected final Logger log;
	protected final String gwId;
	private long prevDt;
	private String shutdown = null;
	private PrintWriter pw ;

	
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
	
	/**
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
			
		boolean eventFound = checkEvent(trim, incidentType.searchString, incidentType.name, doLog, result, dayStart, incidentType);
		
		if (eventFound) {
			String date = new SimpleDateFormat("yyyy-MM-dd'.txt'").format(dayStart);
			incidentType.counter.increment(date);
		}
		
		return eventFound;
	
	}

	/** 
	 * Check if event is in log file line and perform reporting if so
	 * FIXME update docstring
	 * TODO cleanup
	 * @param line
	 * @param searchString string to search for
	 * @param eventName
	 * @param doLog
	 * @param result
	 * @param dayStart
	 * @return true if event is found (no checking for other events required), otherwise false
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
			    System.out.println("ERROR: Cannot parse \"" + timeString + "\"");
			    return true;
			}
		} else {
			System.out.println(" !!!!!!!! No time string in line:"+line);
			return true;
		}
		
		
		/**
		 * @return false if additional processing has decided not to count the incident
		 */
		if (! iType.filter.exec(elr)) {
			return false;
		}
		
		elr.eventId = eventName;
		//elr.eventTime = getTimeFromLogLine(line);
		elr.fullEventString = line;

 		switch (eventName) {
 		
 		/*case HOMEMATIC:
// 			long startOfHour = AbsoluteTimeHelper.getIntervalStart(elr.eventTime, AbsoluteTiming.HOUR);
 			if(elr.eventTime - prevDt < EventLogEvalProvider.HOUR_MILLIS) return true;
 			else {
 				prevDt = elr.eventTime; 
 				elr.eventMessage = gwId+"#"+TimeUtils.getDateAndTimeString(elr.eventTime)+" : "+eventName;
 			}
		 	break;*/
 		case RESTART_EVENT:											
 			if (shutdown==null)
 				elr.eventMessage = gwId+"#"+TimeUtils.getDateAndTimeString(elr.eventTime)+" : "+eventName + " - "+"possibly device removed by user or itself restarted without shutdown";
 			else 
 				elr.eventMessage = gwId+"#"+TimeUtils.getDateAndTimeString(elr.eventTime)+" : "+eventName +" - "+shutdown;
 				shutdown = null;
		   	break;
 		case SHUTDOWN_DB:
 			shutdown = eventName;
 			break;
 		default:
 			elr.eventMessage = gwId+"#"+TimeUtils.getDateAndTimeString(elr.eventTime)+" : "+eventName;
			break;
 		}
 		elr.gatewayId = gwId;
 		
 		if(elr.eventMessage != null) {
			System.out.println(elr.eventMessage);
			pw.append(elr.eventMessage+"\r\n");
 		}
 		
 		if(eventName != SHUTDOWN_DB) {
 			log.info(gwId+"#"+TimeUtils.getDateAndTimeString(elr.eventTime)+" : "+eventName);
 		}
 		
 		
 		
		result.add(elr);
		return true;
 	}
}