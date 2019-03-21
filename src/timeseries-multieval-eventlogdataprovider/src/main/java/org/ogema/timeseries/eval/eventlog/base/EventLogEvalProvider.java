package org.ogema.timeseries.eval.eventlog.base;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.timeseries.eval.eventlog.util.EventLogFileParser.EventLogResult;
import org.ogema.timeseries.eval.eventlog.incident.EventLogIncidents;
import org.ogema.timeseries.eval.eventlog.incident.EventLogIncidents.EventLogIncidentType;
import org.ogema.timeseries.eval.eventlog.util.EventLogParserUtil;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.timeseries.iterator.api.SampledValueDataPoint;
import org.ogema.util.kpieval.KPIEvalUtil;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.iwes.timeseries.eval.api.EvaluationInput;
import de.iwes.timeseries.eval.api.EvaluationInstance.EvaluationListener;
import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.SingleEvaluationResult;
import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.base.provider.utils.SingleValueResultImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoEvaluationCore;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoResultType;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoSingleEvalProviderPreEval;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

/**
 * Evaluate basic time series qualities per gateway from logfiles
 */
@Component(service = EvaluationProvider.class)
public class EventLogEvalProvider extends GenericGaRoSingleEvalProviderPreEval {
	
	private final String basePathStr = System.getProperty("org.smartrplace.analysis.backup.parser.basepath");
	private final Path basePath = Paths.get(basePathStr);

	public static final long MINUTE_MILLIS = 60000;
	public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
	public static final long DAY_MILLIS = 24 * HOUR_MILLIS;
		
	/* Provider Information */
    public final static String ID = "basic-eventlog_eval_provider";
    public final static String LABEL = "Basic EventLog: Incident/Error detection";
    public final static String DESCRIPTION = "Basic EventLog: Provides critical event evaluation";
    
    /** Additional information about this evaluation. Sent with each message */
    public final static String MSG_EVAL_INFO = ""
    		+ "Data Overview: "
    		+ "https://sema.iee.fraunhofer.de:8443/com/example/app/evaluationofflinecontrol/eventLogEval.html\r\n"
    		+ "Data Overview by incident type: "
    		+ "https://sema.iee.fraunhofer.de:8443/com/example/app/evaluationofflinecontrol/eventLogEvalIndi.html\r\n"
    		+ "Evaluation Overview: "
    		+ "https://www.ogema-source.net/wiki/display/SEMA/Wettbewerb+und+Feldtest+ab+Mitte+2018\r\n";
    
    protected static final Logger logger = LoggerFactory.getLogger(EventLogEvalProvider.class);
    
    public EventLogEvalProvider() {
        super(ID, LABEL, DESCRIPTION);
    }

	/* Provide your data types here*/
	@Override
	public GaRoDataType[] getGaRoInputTypes() {
		/*
		 * Even though we're not processing any GaRoInputTypes, this function may
		 * not return an empty array or an IllegalArgumentException will be thrown
		 * and evaluation aborted. FIXME!
		 */
		return new GaRoDataType[] {GaRoDataType.OncePerGateway}; 
		//return new GaRoDataType[] {GaRoDataType.TemperatureSetpointFeedback}; 
	}
 	
    @Override
    public int[] getRoomTypes() {
    	return new int[] {-1};
    }

    public class EvalCore extends GenericGaRoEvaluationCore {
    	
    	
    	EventLogIncidents eli = new EventLogIncidents();
    	
		EventLogFileParserFirst fileParser = new EventLogFileParserFirst(logger, currentGwId, eli);
		int eventNum = 0;
		
		int incidentCount;
		float incidentsPerDay;
		
    	final int size;
 		
 		final long totalTime;
       	final long startTime;
       	final long endTime;
   	
    	/** Application specific state variables, see also documentation of the util classes used*/

    	long durationTime = 0;
    	
    	
    	public EvalCore(List<EvaluationInput> input, List<ResultType> requestedResults,
    			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time,
    			int size, int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
     		
    		logger.info("Starting new Eventlog EvalCore");
    		
			eli.writeCSVHeader();
    		
    		this.size = size;
    		
    		totalTime = startEnd[1] - startEnd[0];
    		startTime = startEnd[0];
    		endTime = startEnd[1];
    		
			//File[] directories = basePath.toFile().listFiles(File::isDirectory);
			//for(File dir: directories) {
    		Path gwdir = basePath.resolve(currentGwId);
			Path logDir = gwdir.resolve("logs");
			File[] logFiles = logDir.toFile().listFiles();
			//long lastFileTime = -1;
			boolean allFiles = Boolean.getBoolean("org.ogema.timeseries.eval.eventlog.base.allfiles");
			
			if(logFiles != null) for(File lgz: logFiles) {
				//long fileTime = lgz.lastModified();
				Path gzFile = lgz.toPath();
				Long dayStartFile = EventLogParserUtil.getDayStartOfLogFile(gzFile);
				if(dayStartFile == null) {
					continue;
				}
				long dayEndFile = AbsoluteTimeHelper.getNextStepTime(dayStartFile, AbsoluteTiming.DAY);
				if((dayEndFile <= startTime || dayStartFile >= endTime)&&(!allFiles)) continue;
				//lastFileTime = fileTime;
				
				Map<String, List<EventLogResult>> fileResult = EventLogParserUtil.processNewGzLogFile(gzFile, 
						fileParser, null, dayStartFile);
				
				eventNum += fileResult.size();
			}
			
			System.out.println("Finished event log data evaluation in "+this.getClass().getName());

			incidentCount = eli.getTotalIncidents();
			incidentsPerDay = (float)incidentCount / ( (float)totalTime / DAY_MILLIS );
			
			
			eli.writeCSVRow(currentGwId, startTime, endTime);
	
			
       	}
      	
    	/** In processValue the core data processing takes place. This method is called for each input
    	 * value of any input time series.*/
    	@Override
    	protected void processValue(int idxOfRequestedInput, int idxOfEvaluationInput,
    			int totalInputIdx, long timeStamp,
    			SampledValue sv, SampledValueDataPoint dataPoint, long duration) {
    		//throw new IllegalStateException("No input specified, should not receive any values");
    	}
    }
 	
 	/**
 	* Define the results of the evaluation here including the final calculation
 	*/
    public final static GenericGaRoResultType BOXSTART_NUM = new GenericGaRoResultType("BOXSTART_NUM",
    		"Number of restarts", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Integer>(rt, cec.eventNum, inputData);
		}
    };
    
    public final static GenericGaRoResultType INCIDENT_COUNT = new GenericGaRoResultType("INCIDENT_COUNT",
    		"Number of incidents", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Integer>(rt, cec.incidentCount, inputData);
		}
    };
    
    
    public final static GenericGaRoResultType LINES_PARSED = new GenericGaRoResultType("LINES_PARSED",
    		"Number of logfile lines parsed", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Integer>(rt, cec.eli.linesParsedCount, inputData);
		}
    };
    
    /** In this list, a KPI for each of the configured incident types is held */
    private List<GenericGaRoResultType> incidentResults = new ArrayList<GenericGaRoResultType>();
	private List<String> incidentResultNamesToDisplay = new ArrayList<String>();
	private boolean incidentResultsFilled = false; // Ensure that result types are only added once
    
    public void addIncidentResults() {
    	
    	if (incidentResultsFilled) return;
    	
    	// Since we don't have access to an EvalCore yet, we're creating a new ELI to get the list of types
    	List<EventLogIncidentType> types = new EventLogIncidents().getTypes();
    	
    	for( EventLogIncidentType t : types ) {
    		GenericGaRoResultType res = new  GenericGaRoResultType(t.name,
    	    		t.description, IntegerResource.class, null) {
    			@Override
    			public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
    					List<TimeSeriesData> inputData) {
    				EvalCore cec = ((EvalCore)ec);
    				EventLogIncidentType iType = cec.eli.getTypeByName(t.name);
    				int incidentCount = iType.counter.getSum();
    				
    				return new SingleValueResultImpl<Integer>(rt, incidentCount, inputData);
    			}
    	    };
    	    incidentResults.add(res);
    	    if (t.display) incidentResultNamesToDisplay.add(t.name);
    	}
    	
    	incidentResultNamesToDisplay.add("timeOfCalculation");
    	incidentResultsFilled = true;

    }
    
    private static final List<GenericGaRoResultType> RESULTS = Arrays.asList(
//   		BOXSTART_NUM, 
    		INCIDENT_COUNT,
    		LINES_PARSED
    		);
    
    
	@Override
	protected List<GenericGaRoResultType> resultTypesGaRo() {
		addIncidentResults();
		List<GenericGaRoResultType> allResults = new ArrayList<GenericGaRoResultType>(RESULTS);
		allResults.addAll(incidentResults);
		return allResults;
	}
	
	public final static String[] kpiResults = new String[]{
			"INCIDENT_COUNT",
			"LINES_PARSED",
			"timeOfCalculation"
			};	
	
	/**
	 * KPI Page(s)
	 */
	@Override
	public List<KPIPageDefinition> getPageDefinitionsOffered() {
		
		List<KPIPageDefinition> result = new ArrayList<>();

		KPIPageDefinition def = new KPIPageDefinition();
		def.resultIds.add(kpiResults);
		def.providerId = Arrays.asList(new String[] {ID});
		def.configName = ID + ": Total Incident Count";
		def.urlAlias = "eventLogEval";
		def.messageProvider = "eventLogMsgProv";
		def.specialIntervalsPerColumn.put("timeOfCalculation", 1);
		result.add(def);
		
		addIncidentResults();
		String[] incidentResultNamesArr = new String[incidentResultNamesToDisplay.size()];
		incidentResultNamesArr = incidentResultNamesToDisplay.toArray(incidentResultNamesArr);
		
		def = new KPIPageDefinition();
		def.resultIds.add(incidentResultNamesArr);
		def.providerId = Arrays.asList(new String[] {ID});
		def.configName = ID + " Incident Count Per Type";
		def.urlAlias = "eventLogEvalIndi";
		def.defaultIntervalsPerColumnType = 1;
		def.specialIntervalsPerColumn.put("timeOfCalculation", 1);
		result.add(def);
		
		return result;
	}

	// Message generation
	// TODO: Don't send a message when no KPI Jumps/Changes detected
	public static final KPIMessageDefinitionProvider eventLogMsgProv = new KPIMessageDefinitionProvider() {

		@Override
		public MessageDefinition getMessage(Collection<KPIStatisticsManagementI> kpis, long currentTime) {
			return new MessageDefinition() {
				@Override
				public String getTitle() {
					return "SEMA Eventlog Evaluation";
				}

				@Override
				public String getMessage() {
					String mes = "Time of message creation: "
							+ TimeUtils.getDateAndTimeString(currentTime) + "\r\n"
							+ detectKPIChanges(kpis, currentTime, kpiResults) + "\r\n\r\n"
							+ MSG_EVAL_INFO + "\r\n";
					return mes;
				}
				
			};
		}
	};
	
	@Override
	public KPIMessageDefinitionProvider getMessageProvider(String messageProviderId) {
		if(messageProviderId.equals("eventLogMsgProv")) return eventLogMsgProv;
		return null;
	}
	
	/**
	 * Detect/Search for significant changes in the given KPIs
	 * @param kpis
	 * @param currentTime
	 * @param kpiResults
	 * @return
	 */
	protected static String detectKPIChanges(Collection<KPIStatisticsManagementI> kpis, long currentTime, 
			String[] kpiResults) {
		
		String[] resultIds = kpiResults;
		
		float downThreshold = 0.75f; // TODO: find good default value; make threshold configurable
		float upThreshold = 1.5f; // TODO: find good default value; make threshold configurable
		List<String> idsToCheckAlways = Arrays.asList(kpiResults);
		
		return KPIEvalUtil.detectKPIChanges(kpis, currentTime, resultIds, 
				downThreshold, upThreshold, idsToCheckAlways);
	}
	
	
	@Override
	protected GenericGaRoEvaluationCore initEval(List<EvaluationInput> input, List<ResultType> requestedResults,
			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time, int size,
			int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
		
		return new EvalCore(input, requestedResults, configurations, listener, time, size, nrInput, 
				idxSumOfPrevious, startEnd);
	}

	@Override
	public List<PreEvaluationRequested> preEvaluationsRequested() {
		return null;
	}
}
