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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.ogema.timeseries.eval.eventlog.util.EventLogFileParser;
import org.ogema.tools.resource.util.TimeUtils;
import org.slf4j.Logger;

import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class EventLogFileParserFirst implements EventLogFileParser {

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

	
	public EventLogFileParserFirst(Logger logger, String gwId) {
		this.log = logger;
		this.gwId = gwId;
	}
	
	@Override
	public List<EventLogResult> parseLogFile(InputStream logFileStream, List<String> eventIds, long dayStart) throws IOException{
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

				if(checkEvent(trim, "Flushing Data every: ", RESTART_EVENT, true, result, dayStart)) continue;
				if(checkEvent(trim, "Error connecting to update server", UPDSERVER_NOCON_EVENT, true, result, dayStart)) continue;
				if(checkEvent(trim, "discarding write to", HOMEMATIC, true, result, dayStart)) continue;
				if(checkEvent(trim, "PING failed", TRANSFER_FAIL_HOMEMATIC, true, result, dayStart)) continue;
				if(checkEvent(trim, "Inactive bundle found", OLD_BUNDLE, true, result, dayStart)) continue;
				checkEvent(trim, "Closing FendoDB data/slotsdb", SHUTDOWN_DB, true, result, dayStart);
				
			} catch(Exception e) {
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
		return Arrays.asList(new String[] {RESTART_EVENT, UPDSERVER_NOCON_EVENT, HOMEMATIC,TRANSFER_FAIL_HOMEMATIC});
	}

	@Override
	public String id() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Initial Event Log File parser, e.g. searching for framework restart events";
	}

	/** Check if event is in log file line and perform reporting if so
	 * 
	 * @param trim
	 * @param eventText
	 * @param eventId
	 * @param doLog
	 * @param result
	 * @param dayStart
	 * @return true if event is found (no checking for other events required), otherwise false
	 * @throws IOException
	 */
	public boolean checkEvent(String trim, String eventText, String eventId, boolean doLog,  
			List<EventLogResult> result, long dayStart) throws IOException {
		if(!trim.contains(eventText)) return false;
		
		EventLogResult elr = new EventLogResult();
		if(trim.length() > 12) {
			SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			String timeString = trim.substring(0, 12);
			try {
			    Date parsed = format.parse(timeString);  
			    elr.eventTime = dayStart + parsed.getTime();
			} catch (ParseException pe) {
			    System.out.println("ERROR: Cannot parse \"" + timeString + "\"");
			    return true;
			}
		} else {
			System.out.println(" !!!!!!!! No time string in line:"+trim);
			return true;
		}
		elr.eventId = eventId;
		//elr.eventTime = getTimeFromLogLine(line);
		elr.fullEventString = trim;

 		switch (eventId) {
 		
 		case HOMEMATIC:
// 			long startOfHour = AbsoluteTimeHelper.getIntervalStart(elr.eventTime, AbsoluteTiming.HOUR);
 			if(elr.eventTime - prevDt < EventLogEvalProvider.HOUR_MILLIS) return true;
 			else {
 				prevDt = elr.eventTime; 
 				elr.eventMessage = gwId+"#"+TimeUtils.getDateAndTimeString(elr.eventTime)+" : "+eventId;
 			}
		 	break;
 		case RESTART_EVENT:											
 			if (shutdown==null)
 				elr.eventMessage = gwId+"#"+TimeUtils.getDateAndTimeString(elr.eventTime)+" : "+eventId + " - "+"possibly device removed by user or itself restarted without shutdown";
 			else 
 				elr.eventMessage = gwId+"#"+TimeUtils.getDateAndTimeString(elr.eventTime)+" : "+eventId +" - "+shutdown;
 				shutdown = null;
		   	break;
 		case SHUTDOWN_DB:
 			shutdown = eventId;
 			break;
 		default:
 			elr.eventMessage = gwId+"#"+TimeUtils.getDateAndTimeString(elr.eventTime)+" : "+eventId;
			break;
 		}
 		elr.gatewayId = gwId;
 		
 		if(elr.eventMessage != null) {
			System.out.println(elr.eventMessage);
			pw.append(elr.eventMessage+"\r\n");
 		}
 		
 		if(eventId != SHUTDOWN_DB) {
 			log.info(gwId+"#"+TimeUtils.getDateAndTimeString(elr.eventTime)+" : "+eventId);
 		}
		result.add(elr);
		return true;
 	}
}