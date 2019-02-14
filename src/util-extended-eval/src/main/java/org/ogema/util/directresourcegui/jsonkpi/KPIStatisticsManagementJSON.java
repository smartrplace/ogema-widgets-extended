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
package org.ogema.util.directresourcegui.jsonkpi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.LongValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.tools.timeseries.api.FloatTimeSeries;
import org.ogema.util.directresourcegui.kpi.KPIStatisticsManagement.ResultForTimestep;
import org.ogema.util.evalcontrol.EvalScheduler;
import org.ogema.util.jsonresult.kpi.JSONAbsoluteTimeHelper;
import org.ogema.util.jsonresult.kpi.JSONStatisticalAggregation;

import de.iwes.timeseries.eval.api.extended.MultiResult;
import de.iwes.timeseries.eval.api.extended.util.AbstractSuperMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;


/** An instance of KPIStatisticsManagement calculates KPIs for a certain result type of a certain
 * EvaluationProvider.<br>
 * TODO: Note that currently only a single time interval type (like hour, day, ...) is supported, but
 * aggregation to longer intervals shall be supported in the future.
 * TODO: Currently the values for the different rooms in the gateways are always averaged. More aggregation
 * modes shall be supported in the future.
 */
public class KPIStatisticsManagementJSON implements KPIStatisticsManagementI {
	public final String providerId;
	public final String resultTypeId;
	protected final JSONStatisticalAggregation sAgg;
	protected final boolean forceCalculations;
	private Integer baseInterval = null;
	
	protected Map<String, KPIStatisticsManagementJSON> moreResultsOfProvider = new HashMap<>();
	//protected final GatewayBackupAnalysis gatewayParser;

	public KPIStatisticsManagementJSON(String resultTypeId, String providerId,
			JSONStatisticalAggregation sAgg, boolean forceCalculations) {
		this.sAgg = sAgg;
		this.forceCalculations = forceCalculations;
		this.resultTypeId = resultTypeId;
		this.providerId = providerId;
		//this.gatewayParser = gatewayParser;
	}
	
	public FloatTimeSeries getIntervalSchedule(int intervalType) {
		return JSONAbsoluteTimeHelper.getIntervalTypeStatistics(intervalType, sAgg);
		/*FloatResource parent =
		if(parent == null) return null;
		if(!parent.exists()) {
			parent.create();
			parent.historicalData().create();
			parent.activate(true);
		} else if(!parent.historicalData().exists()) {
			parent.historicalData().create();
			parent.activate(true);			
		}
//		return parent.historicalData();*/
	}
	
	/** Provides last update times, times should refer to basic interval
	 * In the future a StringArrayResource may contain version Strings to identify the
	 * exact version of the evaluation, which requires effective versioning of these
	 * algorithms, though.
	 */
	public FloatTimeSeries getLastUpdateSchedule() {
		return sAgg.lastUpdate();
		/*if(!sAgg.lastUpdate().historicalData().exists()) {
			sAgg.lastUpdate().historicalData().create();
			sAgg.lastUpdate().historicalData().activate(true);			
		}
		return sAgg.lastUpdate().historicalData();*/
	}
	
	//TODO: The efficiency of the following methods could be improved by storing relevant aligned times
	//TODO: When calculation takes place check whether data from higher resolution aggregation can be used
	//  to avoid touching raw data again
	public SampledValue getValue(int intervalType, long alignedNow, int intervalsIntoPast) {
		FloatTimeSeries sched = getIntervalSchedule(intervalType);
		if(sched == null) return null;
		long destTime = AbsoluteTimeHelper.addIntervalsFromAlignedTime(alignedNow, -intervalsIntoPast, intervalType);
		SampledValue val = sched.getValue(destTime);
		return val ;
	}
	public SampledValue getTimeOfCalculation(int intervalType, long alignedNow, int intervalsIntoPast) {
		if(baseInterval != null && intervalType != baseInterval)
			return null;
		FloatTimeSeries sched = getLastUpdateSchedule();
		if(sched == null) return null;
		long destTime = AbsoluteTimeHelper.addIntervalsFromAlignedTime(alignedNow, -intervalsIntoPast, intervalType);
		SampledValue val = sched.getValue(destTime);
		return val ;
	}
	
	/** Use this if additional upper intervals shall be calculated for existing base interval values*/
	public void updateUpperTimeSteps(int baseType, int[] intervalTypesToUse) {
		this.baseInterval = baseType;
		FloatTimeSeries baseSched = getIntervalSchedule(baseType);
		for(SampledValue sv: baseSched.getValues(0)) {
			float val = sv.getValue().getFloatValue();
			reportNewBaseIntervalValue(sv.getTimestamp(), val, intervalTypesToUse, false);
		}
	}
	
	/** TODO: Make sure only completed intervals are written as we do not store values for ongoing intervals
	 * 
	 * @param result
	 * @param intervalTypesToUse
	 * @param subGW if null a summary KPI for all gateways in the result are calculated
	 */
	/*public void writeKPIs(AbstractSuperMultiResult<MultiResult> result, int[] intervalTypesToUse,
			String subGW) {
		//Schedule destinationSchedule = getIntervalSchedule(intervalType);
		this.baseInterval = intervalTypesToUse[0];
		long startTime = result.startTime;
		long endTime = result.endTime;
		long resultTime = startTime;
		if(resultTime == Long.MAX_VALUE) throw new IllegalStateException("resultTime not set correctly (LONG_MAX)");
		while(resultTime <= endTime) {
			boolean found = false;
			for(MultiResult ir: result.intervalResults) {
				if(ir.getStartTime() == resultTime) {
					if(!(ir instanceof GaRoMultiResult)) throw new IllegalStateException("No GaRo result!");
					GaRoMultiResult irGR = (GaRoMultiResult)ir;
					Map<String, Float> resultForTimeStep = GaRoEvalHelper.getKPIs(irGR, subGW);
					Float newVal = setAllKPIs(resultForTimeStep , resultTime, intervalTypesToUse,
							ir.timeOfCalculation());
					if(newVal == null) break; //NaN will be set
					reportNewBaseIntervalValue(resultTime, newVal, intervalTypesToUse, true);
					//destinationSchedule.addValue(resultTime, new FloatValue(newVal));
					found = true;
					setTimeOfCalc(resultTime, ir.timeOfCalculation(), this);
System.out.println("Wrote "+newVal+" for "+TimeUtils.getDateAndTimeString(resultTime));
					break;
				}
			}
			if(!found) reportNewBaseIntervalValue(resultTime, Float.NaN, intervalTypesToUse, true);
			//if(!found) destinationSchedule.addValue(resultTime, new FloatValue(Float.NaN));
			resultTime = AbsoluteTimeHelper.getNextStepTime(resultTime, intervalTypesToUse[0]);
		}		
		
	}*/
	public void writeKPIs(AbstractSuperMultiResult<MultiResult> result, int[] intervalTypesToUse,
			String subGW) {
		//Schedule destinationSchedule = getIntervalSchedule(intervalType);
		this.baseInterval = intervalTypesToUse[0];
		long startTime = result.startTime;
		long endTime = result.endTime;
		long resultTime = startTime;
		if(resultTime == Long.MAX_VALUE) throw new IllegalStateException("resultTime not set correctly (LONG_MAX)");
		while(resultTime <= endTime) {
			//boolean found = false;
			for(MultiResult ir: result.intervalResults) {
				if(ir.getStartTime() == resultTime) {
					if(!(ir instanceof GaRoMultiResult)) throw new IllegalStateException("No GaRo result!");
					GaRoMultiResult irGR = (GaRoMultiResult)ir;
					ResultForTimestep resultForTimeStep = new ResultForTimestep();
					resultForTimeStep.nums = GaRoEvalHelper.getKPIs(irGR, subGW);
					resultForTimeStep.strings = GaRoEvalHelper.getStringKPIs(irGR, subGW);
					//Float newVal =
					setAllKPIs(resultForTimeStep , resultTime, intervalTypesToUse,
							ir.timeOfCalculation());
					//if(newVal == null) break; //NaN will be set
					//reportNewBaseIntervalValue(resultTime, newVal, intervalTypesToUse, true);
					//destinationSchedule.addValue(resultTime, new FloatValue(newVal));
					//found = true;
					setTimeOfCalc(resultTime, ir.timeOfCalculation(), this);
					/*if(ir.timeOfCalculation() != null) {
						Schedule destinationSchedule = getLastUpdateSchedule();
						destinationSchedule.addValue(resultTime, new LongValue(ir.timeOfCalculation()));
					}*/
//System.out.println("Wrote "+newVal+" for "+TimeUtils.getDateAndTimeString(resultTime));
					break;
				}
			}
			//if(!found) reportNewBaseIntervalValue(resultTime, Float.NaN, intervalTypesToUse, true);
			//if(!found) destinationSchedule.addValue(resultTime, new FloatValue(Float.NaN));
			resultTime = AbsoluteTimeHelper.getNextStepTime(resultTime, intervalTypesToUse[0]);
		}		
		
	}

	
	public int getBaseInterval() {
		if(baseInterval != null) return baseInterval;
		baseInterval = sAgg.minimalInterval(); //AbsoluteTimeHelper.getMinimalInterval(sAgg);
		return baseInterval;
	}
	
	/** We assume that the value belongs to the first element of intervalTypesToUse and that
	 * the intervals are sorted by increasing interval duration
	 * @param resultTime
	 * @param value
	 * @param intervalTypesToUse
	 */
	/*void reportNewBaseIntervalValue(long resultTime, float value, int[] intervalTypesToUse,
			boolean writeBaseValue) {
		FloatTimeSeries lastDestinationSchedule = null;
		int lastType = -1;
		for(int intervalType:intervalTypesToUse) {
			FloatTimeSeries destinationSchedule = getIntervalSchedule(intervalType);
			if(lastDestinationSchedule == null) {
				if(writeBaseValue) destinationSchedule.addValue(resultTime, new FloatValue(value));
			} else {
				Float newVal = aggregateValue(lastDestinationSchedule, lastType, destinationSchedule, intervalType, resultTime);
				if((newVal == null)&&(intervalType != AbsoluteTiming.WEEK)) break;
			}
			if(intervalType != AbsoluteTiming.WEEK) {
				lastDestinationSchedule = destinationSchedule;
				lastType = intervalType;
			} else if(lastDestinationSchedule == null && intervalTypesToUse.length > 1) {
				System.out.println("Warning: Base interval week cannot be aggregated!");
			}
		}
		
	}*/
	void reportNewBaseIntervalValue(long resultTime, ResultForTimestep resultForTimeStep, int[] intervalTypesToUse,
			boolean writeBaseValue) {
		if(resultTypeId.startsWith("$")) {
			String res = resultForTimeStep.strings.get(resultTypeId);
			if(sAgg.stringTimeSeries == null) sAgg.stringTimeSeries = new HashMap<>();
			sAgg.stringTimeSeries.put(resultTime, res);
			return;
		}
		Float res = resultForTimeStep.nums.get(resultTypeId);
		if(res == null) {
			reportNewBaseIntervalValue(resultTime, Float.NaN, intervalTypesToUse, writeBaseValue);
			//other.getValue().addValue(Float.NaN, timeStamp, intervalType);
		}
		else {
			reportNewBaseIntervalValue(resultTime, res, intervalTypesToUse, writeBaseValue);
			//other.getValue().addValue(res, timeStamp, intervalType);
		}
	}
	void reportNewBaseIntervalValue(long resultTime, float value, int[] intervalTypesToUse,
			boolean writeBaseValue) {
		FloatTimeSeries lastDestinationSchedule = null;
		int lastType = -1;
		for(int intervalType:intervalTypesToUse) {
			FloatTimeSeries destinationSchedule = getIntervalSchedule(intervalType);
			if(lastDestinationSchedule == null) {
				if(writeBaseValue) destinationSchedule.addValue(resultTime, new FloatValue(value));
			} else {
				Float newVal = aggregateValue(lastDestinationSchedule, lastType, destinationSchedule, intervalType, resultTime);
				if((newVal == null)&&(intervalType != AbsoluteTiming.WEEK)) break;
			}
			if(intervalType != AbsoluteTiming.WEEK) {
				lastDestinationSchedule = destinationSchedule;
				lastType = intervalType;
			} else if(lastDestinationSchedule == null && intervalTypesToUse.length > 1) {
				System.out.println("Warning: Base interval week cannot be aggregated!");
			}
		}
		
	}

	//TODO: Support other aggregations than averaging
	protected Float aggregateValue(FloatTimeSeries shortSchedule, int shortType, FloatTimeSeries longSchedule, int longType, long newResultTime) {
		long startTimeType = AbsoluteTimeHelper.getIntervalStart(newResultTime, longType);
		long endTimeType = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startTimeType, 1, longType);
		long curTime = startTimeType;
		int count = 0;
		float sum = 0;
		boolean isNan = false;
		long curTimePrev = startTimeType-1;
		while(curTime < endTimeType) {
			SampledValue val = shortSchedule.getValue(curTime);
			if(val == null) {
				if((!shortSchedule.getValues(curTimePrev+1, endTimeType).isEmpty()) && (count > 0)) {
					System.out.println("Found "+shortSchedule.getValues(curTimePrev+1, endTimeType).size()+
							" values for short "+shortType+" within untested interval with no match for type "+longType);
				}
				return null;
			}
			if(!isNan) {
				float fval = val.getValue().getFloatValue();
				if(Float.isNaN(fval)) isNan = true;
				else {
					sum += fval;
					count++;
				}
			}
			curTimePrev = curTime;
			curTime = AbsoluteTimeHelper.addIntervalsFromAlignedTime(curTime, 1, shortType);
		}
		float result;
		if(isNan) result = Float.NaN;
		else result = sum/count;
		longSchedule.addValue(startTimeType, new FloatValue(result));
		return result;
	}
	
	public SampledValue getValueNonAligned(int intervalType, long nonAlignedNow, int intervalsIntoPast) {
		long alignedNow = AbsoluteTimeHelper.getIntervalStart(nonAlignedNow, intervalType);
		return getValue(intervalType, alignedNow, intervalsIntoPast);
	}
	public SampledValue getTimeOfCalculationNonAligned(int intervalType, long nonAlignedNow, int intervalsIntoPast) {
		long alignedNow = AbsoluteTimeHelper.getIntervalStart(nonAlignedNow, intervalType);
		return getTimeOfCalculation(intervalType, alignedNow, intervalsIntoPast);
	}
	
	public List<long[]> getGapTimes(int intervalType, long startTimeNonAligned, long endTimeNonAligned,
			boolean checkMoreResultsOfProvider) {
		List<long[]> result = new ArrayList<>();
		long startTime = AbsoluteTimeHelper.getIntervalStart(startTimeNonAligned, intervalType);
		long lastStartTime = AbsoluteTimeHelper.getIntervalStart(endTimeNonAligned, intervalType);
		long[] currentGap = null;
		FloatTimeSeries sched = getIntervalSchedule(intervalType);
		List<FloatTimeSeries> moreResults = null;
		if(checkMoreResultsOfProvider) {
			moreResults = new ArrayList<>();
			for(KPIStatisticsManagementJSON m: moreResultsOfProvider.values()) {
				moreResults.add(m.getIntervalSchedule(intervalType));
			}
		}
		if(sched == null) return null;
		while(startTime <= lastStartTime) {
			SampledValue val = sched.getValue(startTime);
			boolean gapNow = (val == null);
			if((!gapNow) && checkMoreResultsOfProvider) {
				for(FloatTimeSeries schedMore: moreResults) {
					val = schedMore.getValue(startTime);
					if(val == null) gapNow = true;
					break;
				}
			}
			if((currentGap == null) && gapNow) {
				currentGap = new long[2];
				currentGap[0] = startTime;
			} else if((currentGap != null) && (!gapNow)) {
				currentGap[1] = startTime-1;
				result.add(currentGap);
				currentGap = null;
			}
			startTime = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startTime, 1, intervalType);
		}
		if(currentGap != null) {
			currentGap[1] = endTimeNonAligned;
			result.add(currentGap);
		}
		return result ;
	}

	@Override
	public void addKPIHelperForSameProvider(KPIStatisticsManagementI otherUtilIn) {
		if(!(otherUtilIn instanceof KPIStatisticsManagementJSON))
			throw new IllegalStateException("Wrong type:"+otherUtilIn.getClass().getName());
		KPIStatisticsManagementJSON otherUtil = (KPIStatisticsManagementJSON)otherUtilIn;
		this.moreResultsOfProvider.put(otherUtil.resultTypeId, otherUtil);
	}
	
	//public void addValue(float value, long timeStamp, int intervalType) {
	//	Schedule sched = getIntervalSchedule(intervalType);
	//	sched.addValue(timeStamp, new FloatValue(value));
	//}
	
	/**
	 * 
	 * @param resultForTimeStep
	 * @param timeStamp
	 * @return The value relevant to this result KPI helper
	 */
	/*private Float setAllKPIs(Map<String, Float> resultForTimeStep, long timeStamp, int[] intervalTypesToUse,
			Long timeOfCalculation) {
		for(Entry<String, KPIStatisticsManagementJSON> other: moreResultsOfProvider.entrySet()) {
			Float res = resultForTimeStep.get(other.getKey());
			if(res == null) {
				other.getValue().reportNewBaseIntervalValue(timeStamp, Float.NaN, intervalTypesToUse, true);
				//other.getValue().addValue(Float.NaN, timeStamp, intervalType);
			}
			else {
				other.getValue().reportNewBaseIntervalValue(timeStamp, res, intervalTypesToUse, true);
				//other.getValue().addValue(res, timeStamp, intervalType);
			}
			setTimeOfCalc(timeStamp, timeOfCalculation,other.getValue());
		}
		return resultForTimeStep.get(resultTypeId);
	}*/
	private void setAllKPIs(ResultForTimestep resultForTimeStep, long timeStamp, int[] intervalTypesToUse,
			Long timeOfCalculation) {
		for(Entry<String, KPIStatisticsManagementJSON> other: moreResultsOfProvider.entrySet()) {
			other.getValue().reportNewBaseIntervalValue(timeStamp, resultForTimeStep, intervalTypesToUse, true);
			/*Float res = resultForTimeStep.get(other.getKey());
			if(res == null) {
				other.getValue().reportNewBaseIntervalValue(timeStamp, Float.NaN, intervalTypesToUse, true);
				//other.getValue().addValue(Float.NaN, timeStamp, intervalType);
			}
			else {
				other.getValue().reportNewBaseIntervalValue(timeStamp, res, intervalTypesToUse, true);
				//other.getValue().addValue(res, timeStamp, intervalType);
			}*/
			setTimeOfCalc(timeStamp, timeOfCalculation,other.getValue());
		}
		reportNewBaseIntervalValue(timeStamp, resultForTimeStep, intervalTypesToUse, true);
		//return resultForTimeStep.get(resultTypeId);
	}

	
	private void setTimeOfCalc(long resultTime, Long timeOfCalculation, KPIStatisticsManagementJSON ksm) {
		if(timeOfCalculation != null) {
			FloatTimeSeries destinationSchedule = ksm.getLastUpdateSchedule();
			destinationSchedule.addValue(resultTime, new LongValue(timeOfCalculation));
		}
	}
	
	@Override
	public String providerId() {
		return providerId;
	}
	@Override
	public String resultTypeId() {
		return resultTypeId;
	}
	@Override
	public String specialLindeId() { return null;}
	@Override
	public List<KPIStatisticsManagementI> ksmList() {
		return null;
	}
	
	@Override
	public void setBaseInterval(int intervalType) {
		this.baseInterval = intervalType;
	}

	private GaRoSingleEvalProvider eval = null;
	private EvalScheduler scheduler;
	@Override
	public void setScheduler(Object scheduler) {
		if(!(scheduler instanceof EvalScheduler)) throw new IllegalStateException("scheduler must implement EvalScheduler!");
		this.scheduler = (EvalScheduler) scheduler;
	}
	
	@Override
	public GaRoSingleEvalProvider getEvalProvider() {
		if(eval == null && scheduler != null)
			eval = scheduler.getProvider(providerId);
		return eval;
	}

	@Override
	public String getStringValue(long timeStamp, int intervalsIntoPast) {
		if(sAgg.stringTimeSeries == null) return null;
		long alignedNow = AbsoluteTimeHelper.getIntervalStart(timeStamp, baseInterval);
		long destTime = AbsoluteTimeHelper.addIntervalsFromAlignedTime(alignedNow, -intervalsIntoPast, baseInterval);
		return sAgg.stringTimeSeries.get(destTime);
	}
	
	@Override
	public String evalConfigLocation() {
		return sAgg.resourceName;
	}
}
