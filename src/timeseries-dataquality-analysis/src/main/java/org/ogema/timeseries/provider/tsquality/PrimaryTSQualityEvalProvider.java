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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.timeseries.iterator.api.SampledValueDataPoint;
import org.ogema.util.directresourcegui.kpi.KPIMonitoringReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.timeseries.tsquality.QualityEvalProviderBase;
import org.smartrplace.tissue.util.format.StringFormatHelperSP;

import de.iwes.timeseries.eval.api.EvaluationInput;
import de.iwes.timeseries.eval.api.EvaluationInstance.EvaluationListener;
import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.SingleEvaluationResult;
import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.base.provider.utils.SingleValueResultImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoEvaluationCore;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoResultType;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.messaging.MessagePriority;

/**
 * Evaluate basic time series qualities per gateway including gap evaluation
 */
@Service(EvaluationProvider.class)
@Component
public class PrimaryTSQualityEvalProvider extends QualityEvalProviderBase {
	
	/** Adapt these values to your provider*/
    public final static String ID = "basic-quality_eval_provider";
    public final static String LABEL = "Basic Quality: Gap evaluation provider";
    public final static String DESCRIPTION = "Basic Quality: Provides gap evaluation, additional information in log file";
    
    protected static final Logger logger = LoggerFactory.getLogger(PrimaryTSQualityEvalProvider.class);
    
    public PrimaryTSQualityEvalProvider() {
        super(ID, LABEL, DESCRIPTION);
    }

    public PrimaryTSQualityEvalProvider(String id2, String label2, String description2) {
		super(id2, label2, description2);
	}
    	
 	public class EvalCore2 extends EvalCore {
    	int powerNum;
     	
 	
    	int winNum;
    	final int[] countWindowEvents;
    	long[] openEventStarted;
    	
    	public EvalCore2(List<EvaluationInput> input, List<ResultType> requestedResults,
    			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time,
    			int size, int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
    		super(input, requestedResults, configurations, listener, time, size, nrInput, idxSumOfPrevious, startEnd);
    		powerNum = nrInput[POWER_IDX];
    		tsNum -= powerNum;
    		
    	    winNum = input.get(WINDOW_IDX).getInputData().size();
    	    countWindowEvents = new int[winNum]; 
    		openEventStarted = new long[winNum];
    	    
    	    currentGwId = PrimaryTSQualityEvalProvider.this.currentGwId;
    		//List<TimeSeriesDataImpl> powerData = powerType.inputInfo;
    		//if(powerData != null) System.out.println("Power series data num:"+powerData.size()+" gwId:"+currentGwId);
      	}
      	
    	/** In processValue the core data processing takes place. This method is called for each input
    	 * value of any input time series.*/
    	@Override
    	protected void processValue(int idxOfRequestedInput, int idxOfEvaluationInput,
    			int totalInputIdx, long timeStamp,
    			SampledValue sv, SampledValueDataPoint dataPoint, long duration) {
    		durationTime += duration;
    		
    		processCallforTS(totalInputIdx, idxOfRequestedInput, idxOfEvaluationInput,
    				timeStamp, false);
    		
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
        
        public FinalResult getFinalResult() {
			if(result != null) return result;
        	result = new FinalResult();
			
        	roomId = currentRoomId;
        	
        	long minNonGapTime = (long) (totalTime * (1.0f-MAX_GAP_FOR_GOOD_SERIES_SHARE));
        	long minNonGapTimeGold = (long) (totalTime * (1.0f-MAX_GAP_FOR_GOLD_SERIES_SHARE));
			for(int idx = 0; idx < size; idx++) {
				if(lastTimes[idx] > 0 && lastTimes[idx] < (endTime-1)) {
					int idxOfRequestedInput = getRequiredInputIdx(idx);
					int idxOfEvaluationInput = getEvaluationInputIdx(idx);
					processCallforTS(idx, idxOfRequestedInput, idxOfEvaluationInput, endTime-1, true);
				}
			}
			int thisReqIdx = 0;
			int countWithData = 0;
			int countGood = 0;
			int countGoodGold = 0;
			int countTotal = 0;
			int cntGaps = 0;
			Map<String, GapData> devicesWithGaps = new HashMap<>();
			GaRoDataType[] inputs = getGaRoInputTypes();
			for(int idx = 0; idx < size; idx++) {
				/*int idxOfRequestedInput = getRequiredInputIdx(idx);
				int idxOfEvaluationInput = getEvaluationInputIdx(idx);
    			GaRoDataTypeParam type = getParamType(idxOfRequestedInput);
    			String ts = type.inputInfo.get(idxOfEvaluationInput).id();*/
    			String ts = getTimeSeriesId(idx, PrimaryTSQualityEvalProvider.this);
				if((idx >= evalInstance.getIdxSumOfPrevious()[POWER_IDX]) &&
						(idx < evalInstance.getIdxSumOfPrevious()[POWER_IDX+1])) continue;
				if(countPoints[idx] > 0) {
					if(thisReqIdx != SETP_IDX)
						result.withDataNum++;
					countWithData++;
				}
				//If the previous is false, then durationTimes must be zero, but we keep the test as it is for now
				if(durationTimes[idx] >= minNonGapTime) {
					if(thisReqIdx != SETP_IDX)
						result.goodNum++;
					else
						logger.info("!!!! Found SETP_IDX:");
					countGood++;
				} else {
					String devId = getDevicePath(ts);
					GapData curDuration = devicesWithGaps.get(devId);
					long gapTime = totalTime-durationTimes[idx];
					if(curDuration == null || (curDuration.duration < gapTime)) {
						GapData gd = new GapData();
						gd.duration = gapTime;
						gd.firstGapStart = firstGapStart[idx];
						devicesWithGaps.put(devId, gd);
					}
				}
				if(durationTimes[idx] >= minNonGapTimeGold) {
					if(thisReqIdx != SETP_IDX)
						result.goodNumGold++;
					countGoodGold++;
				} else {
					//do something with non-golden series
				}
				countTotal++;
				cntGaps += countGaps[idx];
				int nextReqInput;
				if(idx == (size-1)) nextReqInput = Integer.MAX_VALUE;
				else nextReqInput = getRequiredInputIdx(idx+1);
				if(nextReqInput != thisReqIdx) {
					logger.info("Gw:"+currentGwId+" For "+inputs[thisReqIdx].label(null)+" withData:"+countWithData+" good:"+countGood+" golden:"+countGoodGold+" total:"+countTotal+" #Gaps:"+cntGaps);
					countWithData = 0;
					countGood = 0;
					countGoodGold = 0;
					countTotal = 0;
					cntGaps = 0;
				}
				thisReqIdx = nextReqInput;
			}
			for(Entry<String, GapData> gap: devicesWithGaps.entrySet()) {
    			logger.info("Total Gap in device "+currentGwId+":"+gap.getKey()+" of "+StringFormatHelperSP.getFormattedTimeOfDay(gap.getValue().duration, true)+" first starting:"+TimeUtils.getDateAndTimeString(gap.getValue().firstGapStart));
			}
			logger.info("Start:"+TimeUtils.getDateString(startTime)+" Gw:"+currentGwId+" Total withData:"+result.withDataNum+" good:"+result.goodNum+" golden:"+result.goodNumGold+" total:"+tsNum);
			return result;
        }
    }
    
 	/**
 	 * Define the results of the evaluation here including the final calculation
 	*/
    public final static GenericGaRoResultType WIN_OPEN_PERDAY = new GenericGaRoResultType("WIN_OPEN_PERDAY",
    		"Average windows openings per day", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore2 cec = ((EvalCore2)ec);
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

    public final static GenericGaRoResultType POWER_NUM = new GenericGaRoResultType("POWER_NUM",
    		"Number of rexometer power series", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore2 cec = ((EvalCore2)ec);
			return new SingleValueResultImpl<Integer>(rt, cec.powerNum, inputData);
		}
    };
    public final static GenericGaRoResultType POWER_TIME_REL = new GenericGaRoResultType("POWER_TIME_REL",
    		"Time share of valid rexometer power values", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore2 cec = ((EvalCore2)ec);
			cec.getFinalResult();
			int count = 0;
			long sum = 0;
			for(int i=cec.evalInstance.getIdxSumOfPrevious()[POWER_IDX]; i<cec.evalInstance.getIdxSumOfPrevious()[POWER_IDX+1]; i++) {
				count++;
				sum += cec.durationTimes[i];
			}
			return new SingleValueResultImpl<Float>(rt, (float) ((double)sum/(count*cec.totalTime)), inputData);
		}
    };
    private static final List<GenericGaRoResultType> RESULTS = Arrays.asList(TS_TOTAL, TS_WITH_DATA, TS_GOOD, TS_GOLD,
		   POWER_NUM, POWER_TIME_REL,
		   OVERALL_GAP_REL,
		   GAP_TIME_REL, ONLY_GAP_REL, TOTAL_TIME, DURATION_TIME,
		   WIN_OPEN_PERDAY); //EVAL_RESULT
    
	@Override
	protected List<GenericGaRoResultType> resultTypesGaRo() {
		return RESULTS;
	}

	@Override
	protected GenericGaRoEvaluationCore initEval(List<EvaluationInput> input, List<ResultType> requestedResults,
			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time, int size,
			int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
		return new EvalCore2(input, requestedResults, configurations, listener, time, size, nrInput, idxSumOfPrevious, startEnd);
	}
	
	public final static String[] qualityBaseResults = new String[]{"TS_TOTAL", "TS_WITH_DATA", "TS_GOOD", "POWER_NUM",
			"POWER_TIME_REL", "OVERALL_GAP_REL", "DURATION_HOURS", "timeOfCalculation"};	
	public final static String[] qualityResults = new String[]{"TS_TOTAL", "TS_WITH_DATA", "TS_GOOD", "TS_GOLD",
			"OVERALL_GAP_REL", "DURATION_HOURS", "timeOfCalculation"};
	public final static String[] rexoQualityResults = new String[]{"TS_TOTAL", "TS_WITH_DATA", "TS_GOOD", "TS_GOLD",
			"OVERALL_GAP_REL", "DURATION_HOURS", "timeOfCalculation"};
	public final static String[] rexoQualityResultsFromBase = new String[]{"POWER_TIME_REL"};

	@Override
	public List<KPIPageDefinition> getPageDefinitionsOffered() {
		List<KPIPageDefinition> result = new ArrayList<>();
		
		//Basic quality page (includes basic rexometer evaluation data)
		KPIPageDefinition def = new KPIPageDefinition();
		def.resultIds.add(qualityBaseResults);
		def.providerId = Arrays.asList(new String[] {"basic-quality_eval_provider"});
		def.configName = "Multi-GW Basic Quality Report";
		def.urlAlias = "basicQuality";
		def.specialIntervalsPerColumn.put("DURATION_HOURS", 1);
		def.specialIntervalsPerColumn.put("timeOfCalculation", 1);
		result.add(def);

		//Gold quality page (basicQualityStd, includes only homematic)
		def = new KPIPageDefinition();
		def.resultIds.add(qualityResults);
		def.providerId = Arrays.asList(new String[] {"basic-quality_eval_provider"});
		def.configName = "Multi-GW Standard Quality Report";
		def.urlAlias = "basicQualityStd";
		def.specialIntervalsPerColumn.put("DURATION_HOURS", 1);
		def.specialIntervalsPerColumn.put("timeOfCalculation", 1);
		def.specialIntervalsPerColumn.put("TS_GOLD", 1);
		def.messageProvider = "qualityMesGen";
		def.hideOverallLine = true;
		result.add(def);
		
		//Rexometer quality page
		def = new KPIPageDefinition();
		def.resultIds.add(rexoQualityResults);
		def.resultIds.add(rexoQualityResultsFromBase);
		def.providerId = Arrays.asList(new String[] {"rexo-quality_eval_provider", "basic-quality_eval_provider"});
		def.configName = "Rexometer including Submetering Quality Report";
		def.urlAlias = "rexoQualityStd";
		def.specialIntervalsPerColumn.put("DURATION_HOURS", 1);
		def.specialIntervalsPerColumn.put("timeOfCalculation", 1);
		def.specialIntervalsPerColumn.put("TS_GOLD", 1);
		def.hideOverallLine = true;
		def.messageProvider = "rexoQualityMesGen";
		result.add(def);
		return result;

	}
	
	public static final KPIMessageDefinitionProvider qualityMesGen = new KPIMessageDefinitionProvider() {

		@Override
		public MessageDefinition getMessage(Collection<KPIStatisticsManagementI> kpis, long currentTime) {
			return new MessageDefinition() {
				@Override
				public String getTitle() {
					return "SEMA Data Quality Evaluation Report";
				}

				@Override
				public String getMessage() {
					String mes = "Time of message creation: "+TimeUtils.getDateAndTimeString(currentTime)+"\r\n"+
							" Data Overview: https://sema.iee.fraunhofer.de:8443/com/example/app/evaluationofflinecontrol/basicQualityStd.html\r\n"+
							" Data Overview including Rexometer: https://sema.iee.fraunhofer.de:8443/com/example/app/evaluationofflinecontrol/basicQuality.html\r\n"+
							" Evaluation Overview: https://www.ogema-source.net/wiki/display/SEMA/Wettbewerb+und+Feldtest+ab+Mitte+2018\r\n"+
							getMessageDataLost(kpis, currentTime, qualityResults);
					return mes;
				}
				
			};
		}
	};
	public static final KPIMessageDefinitionProvider lostRexoMesGen = new KPIMessageDefinitionProvider() {

		@Override
		public MessageDefinition getMessage(Collection<KPIStatisticsManagementI> kpis, long currentTime) {
			return new MessageDefinition() {
				@Override
				public String getTitle() {
					return "Rexometer SEMA Data Quality Evaluation Report";
				}

				@Override
				public String getMessage() {
					String mes = "Time of message creation: "+TimeUtils.getDateAndTimeString(currentTime)+"\r\n"+
							" Rexo Details: https://sema.iee.fraunhofer.de:8443/com/example/app/evaluationofflinecontrol/rexoQualityStd.html\r\n"+
							" Data Overview Homematic: https://sema.iee.fraunhofer.de:8443/com/example/app/evaluationofflinecontrol/basicQualityStd.html\r\n"+
							" Data Overview including Rexometer: https://sema.iee.fraunhofer.de:8443/com/example/app/evaluationofflinecontrol/basicQuality.html\r\n"+
							" Evaluation Overview: https://www.ogema-source.net/wiki/display/SEMA/Wettbewerb+und+Feldtest+ab+Mitte+2018\r\n"+
							getMessageDataLost(kpis, currentTime, rexoQualityResults,
									0.75f, 1.5f, Arrays.asList(new String[] {"POWER_TIME_REL"}));
					return mes;
				}
				
				public MessagePriority getPriority() {
					return MessagePriority.HIGH;
				};
			};
		}
	};
	@Override
	public KPIMessageDefinitionProvider getMessageProvider(String messageProviderId) {
		if(messageProviderId.equals("qualityMesGen")) return qualityMesGen;
		if(messageProviderId.equals("rexoQualityMesGen")) return lostRexoMesGen;
		return null;
	}
	
	public static final int COLUMN_WIDTH = 15;
	public static String getMessageDataLost(Collection<KPIStatisticsManagementI> kpis, long currentTime,
			String[] resultIds) {
		return getMessageDataLost(kpis, currentTime, resultIds, 0.75f, 1.5f, Collections.emptyList());
	}
	/** Report KPI columns that either dropped significantly or were increased significantly compared
	 * to the day before the current day evaluated.
	 * @param kpis
	 * @param currentTime
	 * @param resultIds resultIds for the kpis, must be same index
	 * @param downThreshold if the current value is below downThreshold * <previous Value> a warning
	 * 		line in the message is generated 
	 * @param upThreshold
	 * @param idsToCheckAlways if a warning was reported for a gateway (table line) usually no more
	 * 		columns are checked. The ids given here will still be checked and potentially additional
	 * 		lines will be generated.
	 * @return
	 */
	public static String getMessageDataLost(Collection<KPIStatisticsManagementI> kpis, long currentTime,
			String[] resultIds, float downThreshold, float upThreshold, List<String> idsToCheckAlways) {
		String mes = "";
		for(KPIStatisticsManagementI gw: kpis) {
			if(gw.specialLindeId().startsWith("Overall")) continue;
			int idx = 0;
			boolean checkStillRexoOnly = false;
			for(KPIStatisticsManagementI kpi2: gw.ksmList()) {
				if(checkStillRexoOnly && (!idsToCheckAlways.contains(kpi2.resultTypeId())))
					continue;
				if(resultIds[idx].equals("timeOfCalculation")) {
					//we do not care about this
					continue;
				} else {
					if(kpi2.resultTypeId().startsWith("$")) {
						//we do not care about String values here
						continue;									
					} else {
						SampledValue sv = kpi2.getValueNonAligned(AbsoluteTiming.DAY, currentTime, 1);
						SampledValue svPrev = kpi2.getValueNonAligned(AbsoluteTiming.DAY, currentTime, 2);
						if((sv == null) && (svPrev != null)) {
							mes += (getRightAlignedString(gw.specialLindeId(), COLUMN_WIDTH)+" has NO value ANYMORE in "+kpi2.resultTypeId());
						} else if((sv != null) && (svPrev == null)) {
							mes += (getRightAlignedString(gw.specialLindeId(), COLUMN_WIDTH)+" got back value for "+kpi2.resultTypeId())+" : "+sv.getValue().getFloatValue();
						} else if(sv != null) {
							float val = sv.getValue().getFloatValue();
							float valPrev = svPrev.getValue().getFloatValue();
							int mode = 0;
							if(val < downThreshold*valPrev) mode = -1;
							else if(val > upThreshold*valPrev) mode = 1;
							if(mode != 0) {
								final GaRoDataTypeI dataType;
								GaRoSingleEvalProvider prov = kpi2.getEvalProvider();
								ResultType r = null;
								if(prov != null ) {
									r  = KPIMonitoringReport.getResultTypeById(prov, kpi2.resultTypeId());
								}
								if(r != null && r instanceof GaRoDataTypeI) dataType = (GaRoDataTypeI) r;
								else dataType = null;
								mes += (getRightAlignedString(gw.specialLindeId(), COLUMN_WIDTH)+"#"+kpi2.resultTypeId()+
										((mode>0)?" jumped up ":" fell down ")+" from "+KPIMonitoringReport.formatValue(svPrev, dataType)+
										" to "+KPIMonitoringReport.formatValue(sv, dataType)+"\r\n");
								checkStillRexoOnly = true;
							}
						}
					}
				}
				idx++;
			}
		}
		return mes;
		
	}
	
	public static String getRightAlignedString(String in, int len) {
		if(in.length() >= len) return in.substring(0, len);
		return StringUtils.repeat(' ', len-in.length())+in;
	}
}
