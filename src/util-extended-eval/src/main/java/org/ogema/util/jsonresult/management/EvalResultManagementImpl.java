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
package org.ogema.util.jsonresult.management;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.jsonresult.JSONResultFileData;
import org.ogema.model.jsonresult.JsonOGEMAFileData;
import org.ogema.model.jsonresult.JsonOGEMAFileManagementData;
import org.ogema.model.jsonresult.MultiEvalManagementData;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.util.evalcontrol.EvalScheduler;
import org.ogema.util.evalcontrol.EvalSchedulerImpl;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.api.extended.util.AbstractSuperMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult.RoomData;
import de.iwes.timeseries.eval.garo.api.base.GaRoPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSuperEvalResult;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GaRoTestStarter;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.ResultHandler;
import de.iwes.util.resourcelist.ResourceListHelper;

public class EvalResultManagementImpl extends JsonOGEMAFileManagementImpl<AbstractSuperMultiResult<?>, JSONResultFileData> implements EvalResultManagement {
	private final EvalSchedulerImpl evalScheduler;
	
	/** Constructor
	 * @param appData configuration resource to use, should be configured here
	 * @param stdStepSize see {@link EvalScheduler#registerProviderForKPI(GaRoSingleEvalProvider, boolean)}
	 */
	public EvalResultManagementImpl(JsonOGEMAFileManagementData appData, int stdStepSize, ApplicationManager appMan) {
		super(appData, appMan);
		MultiEvalManagementData schedRes = getSchedRes();
		//if(gatewayParser != null)
			this.evalScheduler = new EvalSchedulerImpl(schedRes, stdStepSize, this, appMan, EvalResultManagementStd.DEFAULT_INTERVALS_TO_CALCULATE);
		//else this.evalScheduler = null;
	}
	/** Constructor
	 * @param appData configuration resource to use, may be virtual
	 * @param the configuration resource will be set to this basePath
	 * @param stdStepSize see {@link EvalScheduler#registerProviderForKPI(GaRoSingleEvalProvider, boolean)}
	 */
	public EvalResultManagementImpl(JsonOGEMAFileManagementData appData, String basePath, int stdStepSize, ApplicationManager appMan) {
		super(appData, basePath, appMan);
		MultiEvalManagementData schedRes = getSchedRes();
		//if(gatewayParser != null)
			this.evalScheduler = new EvalSchedulerImpl(schedRes, stdStepSize, this, appMan, EvalResultManagementStd.DEFAULT_INTERVALS_TO_CALCULATE);
		//else this.evalScheduler = null;
	}
	
	/** Constructor will create a new configuration resource
	 * @param parent parent of configuration resource, it will be added as decorator
	 * @param managementName name of configuration resource to be created
	 * @param the configuration resource will be set to this basePath
	 * @param stdStepSize see {@link EvalScheduler#registerProviderForKPI(GaRoSingleEvalProvider, boolean)}
	 */
	public EvalResultManagementImpl(Resource parent, String managementName, String basePath, int stdStepSize, ApplicationManager appMan) {
		super(parent, managementName, basePath, appMan);
		MultiEvalManagementData schedRes = getSchedRes();
		//if(gatewayParser != null)
			this.evalScheduler = new EvalSchedulerImpl(schedRes, stdStepSize, this, appMan, EvalResultManagementStd.DEFAULT_INTERVALS_TO_CALCULATE);
		//else this.evalScheduler = null;
	}

	
	MultiEvalManagementData getSchedRes() {
		return appData.getSubResource("multiEvalManagementData", MultiEvalManagementData.class);
	}

	@Override
	public <M extends AbstractSuperMultiResult<?>, N extends M> JSONResultFileData saveResult(M result,
			Class<N> typeToUse, int status, String baseFileName,
			boolean overwriteIfExisting, String providerId, List<JsonOGEMAFileData> preEvaluationsUsed) {
		JSONResultFileData myResult = saveResult(result, typeToUse, status, baseFileName, overwriteIfExisting, providerId);
		myResult.preEvaluationsUsed().create();
		for(JsonOGEMAFileData pre: preEvaluationsUsed) {
			myResult.addDecorator(ResourceListHelper.createNewDecoratorName("E", myResult.preEvaluationsUsed()), pre);
		}
		myResult.preEvaluationsUsed().activate(true);
		myResult.startTime().<TimeResource>create().setValue(result.getStartTime());
		myResult.endTime().<TimeResource>create().setValue(result.getEndTime());
		int num = result.intervalResults.size();
		myResult.timeIntervalNum().<IntegerResource>create().setValue(num);
		
		if(num > 0) {
			myResult.stepSize().<TimeResource>create().setValue((result.endTime - result.startTime)/num);
			if(result instanceof GaRoSuperEvalResult) {
				GaRoSuperEvalResult<?> sres = (GaRoSuperEvalResult<?>)result;
				Set<String> gws = new HashSet<>();	
				//TODO: For now we just check first interval
				for(RoomData reval: sres.intervalResults.get(0).roomEvals) {
					gws.add(reval.gwId);
				}
				myResult.gatewaysIncluded().create();
				myResult.gatewaysIncluded().setValues(gws.toArray(new String[0]));
			}
		}
		
		myResult.activate(true);
		return myResult;
	}

	@Override
	protected Class<JSONResultFileData> getDescriptorType() {
		return JSONResultFileData.class;
	}
	
	 /** Perform multi-evaluation on JAXB resources for a GaRoSingleEvalProvider class
     * 
     * @param singleEvalProvider evaluation provider class to use. Note that this method is
     * 		usually not applicable for providers that require initialization. In this case reflective construction will fail.
     * @param gatewayParser object providing source data
     * @param startTime
     * @param endTime
     * @param resultStepSize
     * @param doExportCSV if not null the result will be exported as zipped csv file
     * @param doBasicEval if true the basic evaluation provider will be executed for quality checks (not recommended anymore)
     * @param preEvalProviders if singleEvalProvider requests pre evaluation data provide the respective providers here
     * @param resultsRequested if null all results offered by the provider will be calculated
     * @param gwIds gateways to be evaluated. If null no filtering of input will be applied.
     * @param result file name, should usually end on ".json"
     * @return
     */
	@Override
	public <P extends GaRoSingleEvalProvider> GaRoTestStarter<GaRoMultiResult> performGenericMultiEvalOverAllData(Class<P> singleEvalProvider,
				long startTime,
				long endTime, ChronoUnit resultStepSize,
				GaRoPreEvaluationProvider[] preEvalProviders, List<ResultType> resultsRequested, List<String> gwIds,
				String resultFileName, String providerId,
				Class<? extends GaRoSuperEvalResult<?>> superResultClassForDeserialization, boolean exportDescriptor,
				List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse) {
		EvalHandlerStd<P> h = new EvalHandlerStd<P>(singleEvalProvider, startTime, endTime,
				resultStepSize, preEvalProviders, resultsRequested, gwIds, null, providerId,
				superResultClassForDeserialization, this, null, true, exportDescriptor,
				dataProvidersToUse, null, false);
		return h.starter;
    }
	@Override
	public <P extends GaRoSingleEvalProvider> GaRoTestStarter<GaRoMultiResult> performGenericMultiEvalOverAllData(Class<P> singleEvalProvider,
			long startTime,
			long endTime, ChronoUnit resultStepSize,
			GaRoPreEvaluationProvider[] preEvalProviders, List<ResultType> resultsRequested, List<String> gwIds,
			String resultFileName, String providerId,
			Class<? extends GaRoSuperEvalResult<?>> superResultClassForDeserialization,
			ResultHandler<GaRoMultiResult> externalResultHandler, boolean writeJSON,
			boolean exportDescriptor,
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse,
			List<ConfigurationInstance> additionalConfigurations) {
		EvalHandlerStd<P> h = new EvalHandlerStd<P>(singleEvalProvider, startTime, endTime,
				resultStepSize, preEvalProviders, resultsRequested, gwIds, null, providerId,
				superResultClassForDeserialization, this, externalResultHandler, writeJSON, exportDescriptor,
				dataProvidersToUse, additionalConfigurations, false);
		return h.starter;
	}

	@Override
	public <P extends GaRoSingleEvalProvider> GaRoTestStarter<GaRoMultiResult> performGenericMultiEvalOverAllDataBlocking(Class<P> singleEvalProvider,
			long startTime,
			long endTime, ChronoUnit resultStepSize,
			GaRoPreEvaluationProvider[] preEvalProviders, List<ResultType> resultsRequested, List<String> gwIds,
			String resultFileName, String providerId,
			Class<? extends GaRoSuperEvalResult<?>> superResultClassForDeserialization,
			ResultHandler<GaRoMultiResult> externalResultHandler, boolean writeJSON,
			boolean exportDescriptor,
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse,
			List<ConfigurationInstance> additionalConfigurations,
			boolean performBlocking) {
		EvalHandlerStd<P> h = new EvalHandlerStd<P>(singleEvalProvider, startTime, endTime,
				resultStepSize, preEvalProviders, resultsRequested, gwIds, resultFileName, providerId,
				superResultClassForDeserialization, this, externalResultHandler, writeJSON, exportDescriptor,
				dataProvidersToUse, additionalConfigurations, performBlocking);
		return h.starter;
	}
	
	@Override
	public EvalScheduler getEvalScheduler() {
		return evalScheduler;
	}
	
	@Override
	public List<JSONResultFileData> getDataOfProvider(String providerId, long startTime, long endTime,
			boolean includeOverlap) {
		List<JSONResultFileData> result = new ArrayList<>();
		for(JsonOGEMAFileData fd: currentWorkspace.fileData().getAllElements()) {
			if(fd.evaluationProviderId().getValue().equals(providerId)) {
				JSONResultFileData fdr = (JSONResultFileData)fd;
				boolean use = useDescriptor(fdr, startTime, endTime, includeOverlap);
				if(use)	result.add(fdr);
			}
		}
		return result ;
	}

	public List<JSONResultFileData> getDataOfConfig(MultiKPIEvalConfiguration startConfig, long startTime, long endTime,
			boolean includeOverlap) {
		List<JSONResultFileData> result = new ArrayList<>();
		for(JsonOGEMAFileData fd: currentWorkspace.fileData().getAllElements()) {
			JSONResultFileData fdr = (JSONResultFileData)fd;
			if(fdr.startConfiguration().equalsLocation(startConfig)) {
				boolean use = useDescriptor(fdr, startTime, endTime, includeOverlap);
				if(use)	result.add(fdr);
			}
		}
		return result ;
	}

	private boolean useDescriptor(JSONResultFileData fdr, long startTime, long endTime, boolean includeOverlap) {
		if(includeOverlap) {
			if(fdr.startTime().getValue() < endTime && fdr.endTime().getValue() > startTime) return true;
		} else {
			if(fdr.startTime().getValue() >= startTime && fdr.endTime().getValue() <= endTime) return true;					
		}
		return false;
	}
	
	@Override
	public GaRoSuperEvalResult<?>  getAggregatedResult(String providerId, long startTime, long endTime,
			boolean includeOverlap) {
		List<JSONResultFileData> descs = getDataOfProvider(providerId);
		if(descs.isEmpty()) {
			return null;
		}
		return getAggregatedResult(descs, startTime, endTime, includeOverlap);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public GaRoSuperEvalResult<?> getAggregatedResult(List<JSONResultFileData> descs,
			long startTime, long endTime, boolean includeOverlap) {
		if(descs.isEmpty()) return null;
		//sort by startTime
		Collections.sort(descs, new Comparator<JSONResultFileData>(){
		     public int compare(JSONResultFileData o1, JSONResultFileData o2){
		         if(o1.startTime().getValue() == o2.startTime().getValue())
		             return 0;
		         return o1.startTime().getValue() < o2.startTime().getValue() ? -1 : 1;
		     }
		});
		
		GaRoSuperEvalResult<?> result = null;
		long minStartTime = Long.MAX_VALUE;
		long maxEndTime = 0;
		for(JSONResultFileData desc: descs) {
			boolean use = useDescriptor(desc, startTime, endTime, includeOverlap);
			if(!use) continue;
			AbstractSuperMultiResult<?> superResLoc = importFromJSON(desc);
			if(superResLoc.startTime < minStartTime) minStartTime = superResLoc.startTime;
			if(superResLoc.endTime > maxEndTime) maxEndTime = superResLoc.endTime;
			if(result == null) {
				if(superResLoc instanceof GaRoSuperEvalResult)
					result = (GaRoSuperEvalResult<?>) superResLoc;
				else {
					result = new GaRoSuperEvalResult<>(superResLoc.getInputData(), minStartTime,
						superResLoc.configurations);
					result.intervalResults.addAll((Collection) superResLoc.intervalResults);
				}
			} else {
				result.intervalResults.addAll((Collection) superResLoc.intervalResults);				
			}
		}
		if(result == null) return null;
		result.startTime = minStartTime;
		result.endTime = maxEndTime;
		return result ;
	}
	
	public void close() {
		evalScheduler.close();
	}
}
