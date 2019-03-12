package org.ogema.timeseries.eval.eventlog.base;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.timeseries.eval.eventlog.util.EventLogFileParser.EventLogResult;
import org.ogema.timeseries.eval.eventlog.util.EventLogParserUtil;
import org.ogema.timeseries.eval.eventlog.base.EventLogIncidents;
import org.ogema.tools.timeseries.iterator.api.SampledValueDataPoint;
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
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoEvaluationCore;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoResultType;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoSingleEvalProviderPreEval;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

/**
 * Evaluate basic time series qualities per gateway including gap evaluation
 */
@Service(EvaluationProvider.class)
@Component
public class EventLogEvalProvider extends GenericGaRoSingleEvalProviderPreEval {
	
	private final String basePathStr = System.getProperty("org.smartrplace.analysis.backup.parser.basepath");
	private final Path basePath = Paths.get(basePathStr);

	public static final long MINUTE_MILLIS = 60000;
	public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
	public static final long DAY_MILLIS = 24 * HOUR_MILLIS;
		
	/** Adapt these values to your provider*/
    public final static String ID = "basic-eventlog_eval_provider";
    public final static String LABEL = "Basic EventLog: Startup events";
    public final static String DESCRIPTION = "Basic EventLog: Provides critical event evaluation";
    
    protected static final Logger logger = LoggerFactory.getLogger(EventLogEvalProvider.class);
    
    public EventLogEvalProvider() {
        super(ID, LABEL, DESCRIPTION);
    }

	/** Provide your data types here*/
	@Override
	public GaRoDataType[] getGaRoInputTypes() {
		return new GaRoDataType[] {GaRoDataType.TemperatureSetpointFeedback};
	}
 	
    @Override
    public int[] getRoomTypes() {
    	return new int[] {-1};
    }

    public class EvalCore extends GenericGaRoEvaluationCore {
    	
    	EventLogIncidents e = new EventLogIncidents();
    	
    	
    	
		EventLogFileParserFirst fileParser = new EventLogFileParserFirst(logger, currentGwId, e);
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
     		
    		try {
				e.writeCSVHeader();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		
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
				
				Map<String, List<EventLogResult>> fileResult = EventLogParserUtil.processNewGzLogFile(gzFile , fileParser, null, dayStartFile);
				eventNum += fileResult.size();
			}
			System.out.println("Finished event log data evaluation in "+this.getClass().getName());

			incidentCount = e.getTotalIncidents();
			incidentsPerDay = (float)incidentCount / ( (float)totalTime / DAY_MILLIS );
			
			try {
				e.writeCSVRow(currentGwId, startTime, endTime);
			} catch(Exception e) {
				e.printStackTrace();
			}
			
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
    
    public final static GenericGaRoResultType INCIDENT_FREQ = new GenericGaRoResultType("INCIDENT_FREQ",
    		"Number of incidents per day", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Float>(rt, cec.incidentsPerDay, inputData);
		}
    };
 
    private static final List<GenericGaRoResultType> RESULTS = Arrays.asList(
    		BOXSTART_NUM, 
    		INCIDENT_COUNT,
    		INCIDENT_FREQ
    		);
    
	@Override
	protected List<GenericGaRoResultType> resultTypesGaRo() {
		return RESULTS;
	}

	@Override
	protected GenericGaRoEvaluationCore initEval(List<EvaluationInput> input, List<ResultType> requestedResults,
			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time, int size,
			int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
		return new EvalCore(input, requestedResults, configurations, listener, time, size, nrInput, idxSumOfPrevious, startEnd);
	}

	@Override
	public List<PreEvaluationRequested> preEvaluationsRequested() {
		return null;
	}
}
