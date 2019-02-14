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
package org.ogema.util.directresourcegui.kpi;

import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import de.iwes.timeseries.eval.api.extended.MultiResult;
import de.iwes.timeseries.eval.api.extended.util.AbstractSuperMultiResult;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;

/** Extension of KPIStatisticsManagement for the KPIMonitoringReport template GUI class*/
public class KPIResultType implements KPIStatisticsManagementI {

	public final boolean isHeader;
	public final String specialLindeId;

	public final KPIStatisticsManagementI object;
	
 	/*public KPIResultType(String resultTypeId, GaRoSingleEvalProvider provider, StatisticalAggregation sAgg,
			boolean forceCalculations) {
		super(resultTypeId , provider.id(), sAgg, forceCalculations);
		this.isHeader = false;
		this.specialLindeId = null;
	}*/
 	public KPIResultType(KPIStatisticsManagementI kpiStatMan) {
		this(kpiStatMan, null);
	}
 	public KPIResultType(KPIStatisticsManagementI kpiStatMan, String specialLindeId) {
		//super(kpiStatMan.resultTypeId, kpiStatMan.providerId, kpiStatMan.sAgg, kpiStatMan.forceCalculations);
		object = kpiStatMan;
 		this.isHeader = false;
		this.specialLindeId = specialLindeId;
	}

	/** Use this constructor only to generate a header line object*/
	public KPIResultType(boolean isHeader) {
		//super(null, null, null, false);
		object = new KPIStatisticsManagement(null, null, null, false); 
		this.isHeader = isHeader;
		this.specialLindeId = null;
	}

	@Override
	public String specialLindeId() {
		return specialLindeId;
	}
	@Override
	public ReadOnlyTimeSeries getIntervalSchedule(int intervalType) {
		return object.getIntervalSchedule(intervalType);
	}
	@Override
	public ReadOnlyTimeSeries getLastUpdateSchedule() {
		return object.getLastUpdateSchedule();
	}
	@Override
	public SampledValue getValue(int intervalType, long alignedNow, int intervalsIntoPast) {
		return object.getValue(intervalType, alignedNow, intervalsIntoPast);
	}
	@Override
	public SampledValue getTimeOfCalculation(int intervalType, long alignedNow, int intervalsIntoPast) {
		return object.getTimeOfCalculation(intervalType, alignedNow, intervalsIntoPast);
	}
	@Override
	public void updateUpperTimeSteps(int baseType, int[] intervalTypesToUse) {
		object.updateUpperTimeSteps(baseType, intervalTypesToUse);
	}
	@Override
	public void writeKPIs(AbstractSuperMultiResult<MultiResult> result, int[] intervalTypesToUse, String subGW) {
		object.writeKPIs(result, intervalTypesToUse, subGW);		
	}
	@Override
	public int getBaseInterval() {
		return object.getBaseInterval();
	}
	@Override
	public SampledValue getValueNonAligned(int intervalType, long nonAlignedNow, int intervalsIntoPast) {
		return object.getValueNonAligned(intervalType, nonAlignedNow, intervalsIntoPast);
	}
	@Override
	public SampledValue getTimeOfCalculationNonAligned(int intervalType, long nonAlignedNow, int intervalsIntoPast) {
		return object.getTimeOfCalculationNonAligned(intervalType, nonAlignedNow, intervalsIntoPast);
	}
	@Override
	public List<long[]> getGapTimes(int intervalType, long startTimeNonAligned, long endTimeNonAligned,
			boolean checkMoreResultsOfProvider) {
		return object.getGapTimes(intervalType, startTimeNonAligned, endTimeNonAligned, checkMoreResultsOfProvider);
	}
	@Override
	public String providerId() {
		return object.providerId();
	}
	@Override
	public String resultTypeId() {
		return object.resultTypeId();
	}
	@Override
	public List<KPIStatisticsManagementI> ksmList() {
		return object.ksmList();
	}
	@Override
	public String getStringValue(long timeStamp, int intervalsIntoPast) {
		return object.getStringValue(timeStamp, intervalsIntoPast);
	}
	@Override
	public GaRoSingleEvalProvider getEvalProvider() {
		return object.getEvalProvider();
	}
	@Override
	public void addKPIHelperForSameProvider(KPIStatisticsManagementI other) {
		object.addKPIHelperForSameProvider(other);
	}
	@Override
	public void setBaseInterval(int intervalType) {
		object.setBaseInterval(intervalType);
	}
	@Override
	public void setScheduler(Object scheduler) {
		object.setScheduler(scheduler);
	}
	@Override
	public String evalConfigLocation() {
		return object.evalConfigLocation();
	}

}
