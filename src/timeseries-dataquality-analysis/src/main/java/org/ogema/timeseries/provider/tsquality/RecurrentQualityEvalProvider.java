/**
 * ﻿Copyright 2014-2018 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ogema.timeseries.provider.tsquality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.tools.resource.util.TimeUtils;
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
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeParam;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoEvaluationCore;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoResultType;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoSingleEvalProviderPreEval;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoSingleEvaluation;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

/**
 * Evaluate basic time series qualities per gateway including gap evaluation
 */
@Service(EvaluationProvider.class)
@Component
public class RecurrentQualityEvalProvider extends GenericGaRoSingleEvalProviderPreEval {
	public static final long MINUTE_MILLIS = 60000;
	public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
	public static final long DAY_MILLIS = 24 * HOUR_MILLIS;
	public static final long MAX_NONGAP_TIME_STD = 30* MINUTE_MILLIS;
	public static final float MAX_GAP_FOR_GOOD_SERIES_SHARE = 0.1f;
	public static final long MAX_OVERALL_NONGAP_TIME_STD = 45* MINUTE_MILLIS;
	//Markers over MultiEvaluation
	//private int roomEvalCount = 0;
   	//private int eventId = 100;
		
	/** Adapt these values to your provider*/
    public final static String ID = "basic-recurrent-quality_eval_provider";
    public final static String LABEL = "Basic Recurrent Quality: Gap evaluation provider";
    public final static String DESCRIPTION = "Basic Recurrent Quality: Provides gap evaluation, additional information in log file";
    
    private static final Logger logger = LoggerFactory.getLogger(RecurrentQualityEvalProvider.class);
    
    public RecurrentQualityEvalProvider() {
        super(ID, LABEL, DESCRIPTION);
    }

    public static final GaRoDataTypeParam powerType = new GaRoDataTypeParam(GaRoDataType.PowerMeter, false);
    public static final GaRoDataTypeParam winType = new GaRoDataTypeParam(GaRoDataType.WindowOpen, false);
    
	@Override
	/** Provide your data types here*/
	public GaRoDataType[] getGaRoInputTypes() {
		return new GaRoDataType[] {
	        	new GaRoDataTypeParam(GaRoDataType.MotionDetection, false),
	        	new GaRoDataTypeParam(GaRoDataType.HumidityMeasurement, false),
	        	new GaRoDataTypeParam(GaRoDataType.TemperatureMeasurementRoomSensor, false),
	        	new GaRoDataTypeParam(GaRoDataType.TemperatureMeasurementThermostat, false),
	        	new GaRoDataTypeParam(GaRoDataType.TemperatureSetpoint, false), //does not exist in GaRoEvalHelper !
	        	new GaRoDataTypeParam(GaRoDataType.TemperatureSetpointFeedback, false),
	        	new GaRoDataTypeParam(GaRoDataType.TemperatureSetpointSet, false), //setpoint sent to thermostat
	        	new GaRoDataTypeParam(GaRoDataType.ValvePosition, false),
	        	winType,
	        	powerType,
	        	new GaRoDataTypeParam(GaRoDataType.ChargeSensor, false)
		};
	}
	
	@Override
	public int[] getRoomTypes() {
		return new int[] {-1};
	}
	
	public static final long[] MAX_GAPTIMES_INTERNAL = new long[] {24*HOUR_MILLIS,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
			24*HOUR_MILLIS,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //Valve
			3*HOUR_MILLIS, //Window
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //Power
			6*HOUR_MILLIS}; //Charge
	
	@Override
	protected long[] getMaximumGapTimes() {
		return new long[] {2*HOUR_MILLIS, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL};
	}
	/** It is recommended to define the indices of your input here.*/
	public static final int MOTION_IDX = 0;
	public static final int HUMIDITY_IDX = 1;
	public static final int TEMPSENS_IDX = 2;
	public static final int SETP_IDX_NONUSED = 4;
	public static final int SETP_FB_IDX = 5;
	public static final int SETP_IDX = 6;
	public static final int VALVE_IDX = 7;
	public static final int WINDOW_IDX = 8;
	public static final int POWER_IDX = 9;
	public static final int CHARGE_IDX = 10;
    public static final int TYPE_NUM = 11;
	
 	public class EvalCore extends GenericGaRoEvaluationCore {
    	final int[] idxSumOfPrevious;
    	final int size;
 		
 		final long totalTime;
       	final long startTime;
       	final long endTime;
   	
    	/** Application specific state variables, see also documentation of the util classes used*/

    	private long durationTime = 0;
      	
    	int tsNum;
    	int powerNum;
     	
    	final int[] countPoints;
    	final long[] lastTimes ;
    	final long[] durationTimes;
    	final int[] countGaps;
    	//gap times are not really calculated as a time series may not get any call to processValue
       	//long[] gapTimes;
    	long lastTimeStampOverall;
    	public long overallGapTime;
 	
    	int winNum;
    	final int[] countWindowEvents;
    	long[] openEventStarted;
    	
    	final int tsNumLast;
    	
    	public EvalCore(List<EvaluationInput> input, List<ResultType> requestedResults,
    			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time,
    			int size, int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
    		this.idxSumOfPrevious = idxSumOfPrevious;
    		this.size = size;
    		
    		totalTime = startEnd[1] - startEnd[0];
    		startTime = startEnd[0];
    		endTime = startEnd[1];
    		
    		tsNum = 0;
    		for(int nr: nrInput) tsNum += nr;
    		powerNum = nrInput[POWER_IDX];
    		tsNum -= powerNum;
    		
    		countPoints = new int[size];
    	    lastTimes = new long[size];
    	    durationTimes = new long[size];
    	    countGaps = new int[size]; 
    	    //gapTimes = new long[size];
    	    winNum = input.get(WINDOW_IDX).getInputData().size();
    	    countWindowEvents = new int[winNum]; 
    		openEventStarted = new long[winNum];
    	    
    	    lastTimeStampOverall = startTime;
    	    
    		//List<TimeSeriesDataImpl> powerData = powerType.inputInfo;
    		//if(powerData != null) System.out.println("Power series data num:"+powerData.size()+" gwId:"+currentGwId);
    	   
    	    //TODO: Get pre-eval or own data
    	    //TODO: use real base interval size - or is this fixed?
    	    long dayBefore = AbsoluteTimeHelper.addIntervalsFromAlignedTime(time, -1, AbsoluteTiming.DAY);
    	    Float fval = getPreEvalRoomValue(ID, "STATUS_CHANGE", dayBefore, currentGwId, currentRoomId, currentSuperResult);
    	    if(fval == null) tsNumLast = -1;
    	    else tsNumLast = (int)(float)fval;
      	}
      	
    	/** In processValue the core data processing takes place. This method is called for each input
    	 * value of any input time series.*/
    	@Override
    	protected void processValue(int idxOfRequestedInput, int idxOfEvaluationInput,
    			int totalInputIdx, long timeStamp,
    			SampledValue sv, SampledValueDataPoint dataPoint, long duration) {
    		durationTime += duration;
    		
    		processCallforTS(totalInputIdx, idxOfRequestedInput, timeStamp, false);
    		
    		switch(idxOfRequestedInput) {
    		case WINDOW_IDX:
    			boolean state = (sv.getValue().getFloatValue() > 0.5f);
    			if(openEventStarted[idxOfEvaluationInput] > 0) {
    				if(!state) openEventStarted[idxOfEvaluationInput] = -1;
    			} else {
    				if(state) {
    					openEventStarted[idxOfEvaluationInput] = timeStamp;
    					(countWindowEvents[idxOfEvaluationInput])++;
    				}
    			}
    			break;
    		}
    		//System.out.println("Timestamp:"+timeStamp+" F:"+TimeUtils.getDateAndTimeString(timeStamp)+" DurT:"+durationTime+" Dif:"+(timeStamp-startTime)+ "gap:"+gapTime);
    	}
    	
    	void processCallforTS(int totalInputIdx, int idxOfRequestedInput, long timeStamp, boolean isVirtual) {
    		if(!isVirtual)
    			(countPoints[totalInputIdx])++;
    		final long durationLoc;
    		if(lastTimes[totalInputIdx] > 0)
    			durationLoc = timeStamp - lastTimes[totalInputIdx];
    		else {
    			if(isVirtual) durationLoc = 0;
    			else durationLoc = timeStamp - startTime;
    		}
    		if(durationLoc <= MAX_GAPTIMES_INTERNAL[idxOfRequestedInput]) {
    			durationTimes[totalInputIdx] += durationLoc;
    		} else {
    			durationTimes[totalInputIdx] += MAX_GAPTIMES_INTERNAL[idxOfRequestedInput];
    			(countGaps[totalInputIdx])++;
       			//gapTimes[totalInputIdx] += (durationLoc - MAX_GAPTIMES_INTERNAL[idxOfRequestedInput]);
    		}
    		
    		lastTimes[totalInputIdx] = timeStamp;
    		
    		long durationOverall = timeStamp - lastTimeStampOverall;
    		if(durationOverall > MAX_OVERALL_NONGAP_TIME_STD) {
    			overallGapTime += durationOverall;
    		}
    		lastTimeStampOverall = timeStamp;
    	}
    	
        private FinalResult result = null;
        public String roomId;
        public FinalResult getFinalResult() {
			if(result != null) return result;
        	result = new FinalResult();
			
        	roomId = currentRoomId;
        	
        	long minNonGapTime = (long) (totalTime * (1.0f-MAX_GAP_FOR_GOOD_SERIES_SHARE));
			for(int idx = 0; idx < size; idx++) {
				if(lastTimes[idx] > 0 && lastTimes[idx] < (endTime-1)) {
					int idxOfRequestedInput = getRequiredInputIdx(idx);
					processCallforTS(idx, idxOfRequestedInput, endTime-1, true);
				}
			}
			int thisReqIdx = 0;
			int countWithData = 0;
			int countGood = 0;
			int countTotal = 0;
			int cntGaps = 0;
			GaRoDataType[] inputs = getGaRoInputTypes();
			for(int idx = 0; idx < size; idx++) {
				if((idx >= idxSumOfPrevious[POWER_IDX]) &&
						(idx < idxSumOfPrevious[POWER_IDX+1])) continue;
				if(durationTimes[idx] >= minNonGapTime) {
					if(thisReqIdx != SETP_IDX)
						result.goodNum++;
					countGood++;
				}
				if(countPoints[idx] > 0) {
					if(thisReqIdx != SETP_IDX)
						result.withDataNum++;
					countWithData++;
				}
				countTotal++;
				cntGaps += countGaps[idx];
				int nextReqInput;
				if(idx == (size-1)) nextReqInput = Integer.MAX_VALUE;
				else nextReqInput = getRequiredInputIdx(idx+1);
				if(nextReqInput != thisReqIdx) {
					logger.info("Gw:"+currentGwId+" For "+inputs[thisReqIdx].label(null)+" withData:"+countWithData+" good:"+countGood+" total:"+countTotal+" #Gaps:"+cntGaps);
					countWithData = 0;
					countGood = 0;
					countTotal = 0;
					cntGaps = 0;
				}
				thisReqIdx = nextReqInput;
			}
			if(tsNumLast < 0) result.status_ch = 2; //no data before
			else if(tsNum == 0 && tsNumLast > 0)
				result.status_ch = 1;
			else if(tsNum > 0 && tsNumLast == 0)
				result.status_ch = -1;
			else result.status_ch = 0;
			logger.info("Start:"+TimeUtils.getDateString(startTime)+" Gw:"+currentGwId+" Total withData:"+result.withDataNum+" good:"+result.goodNum+" total:"+tsNum+" stat.ch:"+result.status_ch);
			return result;
        }
    }
    
    public static class FinalResult {
    	int goodNum = 0;
       	int withDataNum = 0;
       	int status_ch;
    }
	/**
 	 * Define the results of the evaluation here including the final calculation
 	*/
    public final static GenericGaRoResultType WIN_OPEN_PERDAY = new GenericGaRoResultType("WIN_OPEN_PERDAY",
    		"Average windows openings per day", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			cec.getFinalResult();
			//int count = 0;
			long sum = 0;
			for(int i=0; i<cec.countWindowEvents.length; i++) {
				//count++;
				sum += cec.countWindowEvents[i];
				//TimeSeriesDataImpl ts = winType.inputInfo.get(i);
				//logger.info("In Room: "+ts.id()+" Window Openings:"+cec.countWindowEvents[i]);
			}
			double dayShare = (double)cec.totalTime/DAY_MILLIS;
			return new SingleValueResultImpl<Float>(rt, (float) ((double)sum/dayShare), inputData);
		}
    };

    public final static GenericGaRoResultType TS_TOTAL = new GenericGaRoResultType("TS_TOTAL",
    		"Number of standard time series Homematic standard", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Integer>(rt, cec.tsNum, inputData);
		}
    };
    public final static GenericGaRoResultType TS_GOOD = new GenericGaRoResultType("TS_GOOD",
    		"Number of standard time series Homematic standard with good data", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			FinalResult res = cec.getFinalResult();
			return new SingleValueResultImpl<Integer>(rt, res.goodNum, inputData);
		}
    };
    public final static GenericGaRoResultType TS_WITH_DATA = new GenericGaRoResultType("TS_WITH_DATA",
    		"Number of standard time series Homematic standard with any data", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			FinalResult res = cec.getFinalResult();
			return new SingleValueResultImpl<Integer>(rt, res.withDataNum, inputData);
		}
    };

    public final static GenericGaRoResultType POWER_NUM = new GenericGaRoResultType("POWER_NUM",
    		"Number of rexometer power series", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Integer>(rt, cec.powerNum, inputData);
		}
    };
    public final static GenericGaRoResultType POWER_TIME_REL = new GenericGaRoResultType("POWER_TIME_REL",
    		"Time share of valid rexometer power values", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			cec.getFinalResult();
			int count = 0;
			long sum = 0;
			for(int i=cec.idxSumOfPrevious[POWER_IDX]; i<cec.idxSumOfPrevious[POWER_IDX+1]; i++) {
				count++;
				sum += cec.durationTimes[i];
			}
			return new SingleValueResultImpl<Float>(rt, (float) ((double)sum/(count*cec.totalTime)), inputData);
		}
    };
    public final static GenericGaRoResultType GAP_TIME_REL = new GenericGaRoResultType("GAP_TIME_REL",
    		"Time share in gaps between existing data", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Float>(rt, (float) ((double)ec.gapTime/cec.totalTime), inputData);
		}
    };
    public final static GenericGaRoResultType OVERALL_GAP_REL = new GenericGaRoResultType("OVERALL_GAP_REL",
    		"Time share in gaps with no data on entire gateway", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			//Finalize evaluation
			cec.getFinalResult();
			return new SingleValueResultImpl<Float>(rt, (float) ((double)cec.overallGapTime/cec.totalTime), inputData);
		}
    };
    public final static GenericGaRoResultType ONLY_GAP_REL = new GenericGaRoResultType("ONLY_GAP_REL",
    		"Time share in evals without any data",
    		FloatResource.class, ID) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			if(cec.durationTime == 0 && cec.gapTime == 0)
				return new SingleValueResultImpl<Float>(rt, 1.0f, inputData);
			else
				return new SingleValueResultImpl<Float>(rt, 0.0f, inputData);
		}
    };
    public final static GenericGaRoResultType TOTAL_TIME = new GenericGaRoResultType("TOTAL_HOURS", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Float>(rt, (float) ((double)cec.totalTime / HOUR_MILLIS), inputData);
		}
    };
    public final static GenericGaRoResultType DURATION_TIME = new GenericGaRoResultType("DURATION_HOURS", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Float>(rt, (float) ((double)cec.durationTime / HOUR_MILLIS), inputData);
		}
    };
    public final static GenericGaRoResultType STATUS_CHANGE = new GenericGaRoResultType("STATUS_CHANGE", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			FinalResult fresult = cec.getFinalResult();
			return new SingleValueResultImpl<Integer>(rt, fresult.status_ch, inputData);
		}
    };
   private static final List<GenericGaRoResultType> RESULTS = Arrays.asList(TS_TOTAL, TS_WITH_DATA, TS_GOOD,
		   POWER_NUM, POWER_TIME_REL,
		   OVERALL_GAP_REL,
		   GAP_TIME_REL, ONLY_GAP_REL, TOTAL_TIME, DURATION_TIME,
		   WIN_OPEN_PERDAY, STATUS_CHANGE); //EVAL_RESULT
    
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
		return Arrays.asList(new PreEvaluationRequested(ID, IntervalRelation.AHEAD, false));
	}
	
	@Override
	public List<KPIPageDefinition> getPageDefinitionsOffered() {
		List<KPIPageDefinition> result = new ArrayList<>();
		KPIPageDefinition def = new KPIPageDefinition();
		def.resultIds.add(new String[]{"TS_TOTAL", "TS_WITH_DATA", "TS_GOOD", "POWER_NUM", "POWER_TIME_REL",
				"OVERALL_GAP_REL", "DURATION_HOURS"});
		def.providerId = Arrays.asList(new String[] {ID});
		def.configName = "Multi-GW Basic Quality Report Recurrent";
		def.urlAlias = "basicQualityRecurrent";
		def.specialIntervalsPerColumn.put("DURATION_HOURS", 1);
		result.add(def);
		return result;
	}
}
