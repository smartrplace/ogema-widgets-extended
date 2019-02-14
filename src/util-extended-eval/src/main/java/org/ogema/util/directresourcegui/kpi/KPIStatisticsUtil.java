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

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.model.ResourceList;
import org.ogema.model.alignedinterval.StatisticalAggregation;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.util.directresourcegui.jsonkpi.KPIStatisticsManagementJSON;
import org.ogema.util.jsonresult.kpi.JSONStatisticalAggregation;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.extended.MultiEvaluationInstance;
import de.iwes.timeseries.eval.api.extended.MultiResult;
import de.iwes.timeseries.eval.api.extended.util.AbstractSuperMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoEvalProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.CSVArchiveExporter;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting.PreEvaluationRequested;
import de.iwes.timeseries.eval.garo.multibase.GenericGaRoMultiProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GaRoEvalHelperGeneric;
import de.iwes.util.timer.AbsoluteTiming;

public class KPIStatisticsUtil {
	public static ChronoUnit getIntervalTypeChronoUnit(int intervalType) {
		switch(intervalType) {
		case 1:
			return ChronoUnit.YEARS;
		case 3:
			return  ChronoUnit.MONTHS;
		case 6:
			return  ChronoUnit.WEEKS;
		case 10:
			return  ChronoUnit.DAYS;
		case 100:
			return  ChronoUnit.HOURS;
		case 101:
			return  ChronoUnit.MINUTES;
		case 102:
			return  ChronoUnit.SECONDS;
		default:
			return null;
			//throw new UnsupportedOperationException("Interval type "+intervalType+" not supported as ChronoUnit!");
		}
	}
	
	public static Integer getIntervalType(ChronoUnit chronoUnit) {
		switch(chronoUnit) {
		case YEARS:
			return AbsoluteTiming.YEAR;
		case MONTHS:
			return AbsoluteTiming.MONTH;
		case WEEKS:
			return AbsoluteTiming.WEEK;
		case DAYS:
			return AbsoluteTiming.DAY;
		case HOURS:
			return AbsoluteTiming.HOUR;
		case MINUTES:
			return AbsoluteTiming.MINUTE;
		case SECONDS:
			return AbsoluteTiming.SECOND;
		default:
			return null;
			//throw new UnsupportedOperationException("Chrono type "+chronoUnit+" not supported as interval type!");
		}
	}
	
	public static <T extends MultiResult, P extends GaRoSingleEvalProvider> AbstractSuperMultiResult<T> performGenericMultiEvalOverAllDataBlocking(Class<P> singleEvalProvider,
			long startTime,
			long endTime, ChronoUnit resultStepSize, CSVArchiveExporter doExportCSV, boolean doBasicEval,
			GaRoPreEvaluationProvider[] preEvalProviders, List<ResultType> resultsRequested, List<String> gwIds,
			String resultFileName) {
		try {
			P singleProvider = singleEvalProvider.newInstance();
			if(singleProvider instanceof GaRoSingleEvalProviderPreEvalRequesting) {
				GaRoSingleEvalProviderPreEvalRequesting preEval = (GaRoSingleEvalProviderPreEvalRequesting)singleProvider;
				int i= 0;
				for(PreEvaluationRequested req: preEval.preEvaluationsRequested()) {
					preEval.preEvaluationProviderAvailable(i, req.getSourceProvider(), preEvalProviders[i]);
					i++;
				}
			}
			//GenericGaRoMultiProviderJAXB<P> multiProvider = new GenericGaRoMultiProviderJAXB<P>(singleProvider, doBasicEval);
			//GenericGaRoMultiProvider<P> multiProvider = gatewayParser.getMultiEvalProvider(singleProvider, doBasicEval);
			GenericGaRoMultiProvider<P> multiProvider = new GenericGaRoMultiProvider<P>(singleProvider, doBasicEval); //gatewayParser.getMultiEvalProvider(singleProvider, doBasicEval);
			AbstractSuperMultiResult<T> result = performEvaluation(
					multiProvider,
				startTime, endTime, resultStepSize,
				resultFileName!=null?resultFileName:singleEvalProvider.getSimpleName()+"Result.json", doExportCSV, resultsRequested, gwIds);
			return result;
		} catch(InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends MultiResult> AbstractSuperMultiResult<T> performEvaluation(GaRoEvalProvider<?> garoEval, long startTime,
			long endTime, ChronoUnit resultStepSize,
			String jsonOutFileName, CSVArchiveExporter doExportCSV,
			List<ResultType> resultsRequested, List<String> gwIds) {

		MultiEvaluationInstance<T> eval =
		(MultiEvaluationInstance<T>) GaRoEvalHelperGeneric.startGaRoMultiEvaluationOverAllData(startTime, endTime, resultStepSize,
    				garoEval, resultsRequested, gwIds, null, null, null);
        AbstractSuperMultiResult<T> result = eval.getResult();
        
        System.out.printf("evaluation runs done: %d\n", result.intervalResults.size());
		return result;
	}
	
	public static StatisticalAggregation getCreateStatAgg(String resultName, ResourceList<StatisticalAggregation> resList) {
		if(resList == null) {
			throw new IllegalStateException("resList is null!!");
		}
		for(StatisticalAggregation sAgg: resList.getAllElements()) {
			if(sAgg.getName().equals(resultName)) {
				if(sAgg.name().exists()) sAgg.name().delete();
				return sAgg;
			}
		}
		StatisticalAggregation result = resList.addDecorator(ResourceUtils.getValidResourceName(resultName), StatisticalAggregation.class);
		//result.name().<StringResource>create().setValue(resultName);
		result.activate(true);
		return result;
	}
	
	public static List<KPIStatisticsManagementI> setupKPIUtilsForProvider(String evalProviderId,
			ResourceList<StatisticalAggregation> resultKPIs, List<String> resultTypeIds) {
		List<KPIStatisticsManagementI> kpiList = new ArrayList<>();
		for(String resultType: resultTypeIds) {
			KPIStatisticsManagementI kpi;
			StatisticalAggregation sAgg = KPIStatisticsUtil.getCreateStatAgg(resultType, resultKPIs);
			kpi = new KPIStatisticsManagement(resultType, evalProviderId, sAgg, true);				
			kpiList.add(kpi);
		}
	    //set reference to all other providers
	    for(KPIStatisticsManagementI kpi: kpiList) {
	    	ArrayList<KPIStatisticsManagementI> newList = new ArrayList<>(kpiList);
	    	newList.remove(kpi);
	    	//copy newList into moreResultsOfProvider, which has to be done for each element separately
	    	for(KPIStatisticsManagementI other: newList) kpi.addKPIHelperForSameProvider(other);
	    }
		return kpiList;
	}
	
	//JSON versions
	public static JSONStatisticalAggregation getCreateStatAggJSON(String resultName, Map<String, JSONStatisticalAggregation> resList) {
		if(resList == null) {
			throw new IllegalStateException("resList is null!!");
		}
		for(Entry<String, JSONStatisticalAggregation> sAgg: resList.entrySet()) {
			if(sAgg.getKey().equals(resultName)) {
				//if(sAgg.name().exists()) sAgg.name().delete();
				return sAgg.getValue();
			}
		}
		JSONStatisticalAggregation result = new JSONStatisticalAggregation();
		result.resourceName = resultName;
		resList.put(ResourceUtils.getValidResourceName(resultName), result);
		//result.name().<StringResource>create().setValue(resultName);
		//result.activate(true);
		return result;
	}
	
	public static List<KPIStatisticsManagementI> setupKPIUtilsForProviderJSON(String evalProviderId,
			Map<String, JSONStatisticalAggregation> resultKPIs, List<String> resultTypeIds) {
		List<KPIStatisticsManagementI> kpiList = new ArrayList<>();
		for(String resultType: resultTypeIds) {
			JSONStatisticalAggregation sAgg = KPIStatisticsUtil.getCreateStatAggJSON(resultType, resultKPIs);
			KPIStatisticsManagementJSON kpi = new KPIStatisticsManagementJSON(resultType, evalProviderId, sAgg, true);
			kpiList.add(kpi);
		}
	    //set reference to all other providers
	    for(KPIStatisticsManagementI kpi: kpiList) {
	    	ArrayList<KPIStatisticsManagementI> newList = new ArrayList<>(kpiList);
	    	newList.remove(kpi);
	    	//copy newList into moreResultsOfProvider, which has to be done for each element separately
	    	for(KPIStatisticsManagementI other: newList) kpi.addKPIHelperForSameProvider(other);
	    }
		return kpiList;
	}
	

}
