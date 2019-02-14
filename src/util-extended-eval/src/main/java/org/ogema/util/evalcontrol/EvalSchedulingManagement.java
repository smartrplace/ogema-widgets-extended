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
package org.ogema.util.evalcontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.model.jsonresult.MultiEvalStartConfiguration;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.util.directresourcegui.kpi.KPIStatisticsUtil;
import org.ogema.util.evalcontrol.EvalScheduler.OverwriteMode;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.configuration.Configuration;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.api.extended.MultiResult;
import de.iwes.timeseries.eval.api.extended.util.AbstractMultiResult;
import de.iwes.timeseries.eval.api.extended.util.AbstractSuperMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoStdPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSuperEvalResult;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting.IntervalRelation;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting.PreEvaluationRequested;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.ResultHandler;
import de.iwes.util.timer.AbsoluteTimeHelper;

/** Organize scheduling of evalations including pre-evaluation*/
public class EvalSchedulingManagement {
	protected final EvalResultManagement evalResultMan;
	protected final EvalScheduler evalScheduler;
	//There is one place where we need access to implementation. Could probably be changed quite easily if necessary
	protected final EvalSchedulerImpl evalSchedulerImpl;
	//protected final GatewayBackupAnalysisAccess gatewayParser;
	protected final ApplicationManager appMan;
	protected final int stdStepSize;
	
	protected boolean exportDescriptor = false;
	public void setDescriptorExportMode(boolean exportDescriptor) {
		this.exportDescriptor = exportDescriptor;
	}

	public EvalSchedulingManagement(EvalResultManagement evalResultMan,
			EvalSchedulerImpl evalScheduler, int stdStepSize,
			ApplicationManager appMan) {
		this.evalResultMan = evalResultMan;
		this.evalScheduler = evalScheduler;
		this.evalSchedulerImpl = evalScheduler;
		//this.gatewayParser = gatewayParser;
		this.appMan = appMan;
		this.stdStepSize = stdStepSize;
	}

	/** Implementation for {@link EvalScheduler#queueEvalConfig(MultiKPIEvalConfiguration, boolean, ResultHandler, long, long, List, boolean, OverwriteMode, boolean, String, Set)}
	 * 
	 * Note that the method is called recursively to fill gaps and to calculate pre-evaluations.
	 * 
	 */
	public long[] queueEvalConfig(MultiKPIEvalConfiguration startConfig, boolean saveJsonResult,
			String resultFileName,
			ResultHandler<?> listener, long startTimeNonAligned, long endTime,
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse, boolean queuePreEvals,
			OverwriteMode overwriteMode, boolean overWritePreEvalSettings, String runSingleGw,
			Set<String> evalProvidersQueued) {
		if(evalProvidersQueued == null) evalProvidersQueued = new HashSet<>();
		String providerId = startConfig.evaluationProviderId().getValue();
		if(evalProvidersQueued.contains(providerId)) {
			System.out.println("   ****    Evalution "+providerId+
					" already done, not queued again.");
			return null;
		}
		else evalProvidersQueued.add(providerId);
		int stepSize = getStepSize(startConfig);
		if(overwriteMode == OverwriteMode.NO_OVERWRITE) {
			List<KPIStatisticsManagementI> kpiUtils = evalSchedulerImpl.configureKPIManagement(startConfig, (List<String>)null); //kpiUtilsMap.get(startConfig.getLocation());
			List<long[]> gaps = getGaps(kpiUtils, stepSize, startTimeNonAligned, endTime);
			/*if(kpiUtils == null || kpiUtils.isEmpty()) {
				long startTime = AbsoluteTimeHelper.getIntervalStart(startTimeNonAligned, stepSize);
				gaps = new ArrayList<>();
				gaps.add(new long[] {startTime, endTime});
			} else gaps = kpiUtils.get(0).getGapTimes(stepSize, startTimeNonAligned, endTime, true);*/
			if((gaps.size() == 1)) { // &&
			//		(gaps.get(0)[0] <= startTimeNonAligned) && (gaps.get(0)[1] >= endTime)) {
				//nothing found, check if data in JSON files is available
				long startTime = AbsoluteTimeHelper.getIntervalStart(startTimeNonAligned, stepSize);
				@SuppressWarnings("unchecked")
				AbstractSuperMultiResult<MultiResult> result = (AbstractSuperMultiResult<MultiResult>) evalResultMan.getAggregatedResult(providerId, startTime, endTime, true);
				//AbstractSuperMultiResult<MultiResult> result = evalResultMan.importFromJSON(object);
				//List<KPIStatisticsManagement> kpiUtils =
				if(result != null) {
					GaRoSingleEvalProvider eval = evalScheduler.getProvider(startConfig.evaluationProviderId().getValue());
					for(MultiResult ir:result.intervalResults) {
						if(ir instanceof AbstractMultiResult)
							((AbstractMultiResult)ir).timeOfCalculation = appMan.getFrameworkTime();
					}
					List<KPIStatisticsManagementI> kpis = evalResultMan.getEvalScheduler().calculateKPIs(
							startConfig, eval, result , stepSize , null, null);
					System.out.println("Created/updated KPIs for provider "+providerId+
							" baseInterval:"+stepSize+" #kpis:"+kpis.size());
					gaps = getGaps(kpiUtils, stepSize, startTimeNonAligned, endTime);
				}
			}
			if(gaps.isEmpty()) {
				System.out.println("   ****    No gaps found for "+providerId+
						", not queued again.");
				return null;
			}
			
			System.out.println("Found "+gaps.size()+" gaps for "+providerId);
			evalProvidersQueued.remove(providerId);
			for(long[] gap: gaps) {
				Set<String> evalProvidersQueuedLoc;
				if(gaps.size() == 1) evalProvidersQueuedLoc = evalProvidersQueued;
				else evalProvidersQueuedLoc = new HashSet<>(evalProvidersQueued);
				System.out.println("   ****    Queuing gap for "+providerId+
						", start:"+TimeUtils.getDateAndTimeString(gap[0])+", end:"+TimeUtils.getDateAndTimeString(gap[1]));
				queueEvalConfig(startConfig, saveJsonResult, resultFileName, listener,
						gap[0], gap[1], dataProvidersToUse, true, OverwriteMode.ONLY_PROVIDER_REQUESTED,
						overWritePreEvalSettings, runSingleGw, evalProvidersQueuedLoc);
			}
			// we have done it now;
			long[] result = new long[]{gaps.get(0)[0], gaps.get(gaps.size()-1)[1]};
			return result;
		}
		long startTime = AbsoluteTimeHelper.getIntervalStart(startTimeNonAligned, stepSize);
		
		if(queuePreEvals &&(!startConfig.preEvaluationSchedulingMode().isActive() &&
				startConfig.preEvaluationSchedulingMode().getValue() == 0)) {
			GaRoSingleEvalProvider eval = evalScheduler.getProvider(startConfig.evaluationProviderId().getValue());
			if(eval instanceof GaRoSingleEvalProviderPreEvalRequesting) {
				GaRoSingleEvalProviderPreEvalRequesting peval = (GaRoSingleEvalProviderPreEvalRequesting)eval;
				if(peval.preEvaluationsRequested() != null) for(PreEvaluationRequested pre: peval.preEvaluationsRequested()) {
					if(!pre.isRequired()) continue;
					GaRoSingleEvalProvider preProv = evalScheduler.getProvider(pre.getSourceProvider());
					IntervalRelation type = pre.getIntervalRelation();
					//TODO: Hopefully we do not have recursive pre-evaluation dependenices
					OverwriteMode subMode;
					if(overwriteMode == OverwriteMode.ALL_INCLUDING_PRE_EVALUATIONS) subMode = OverwriteMode.ALL_INCLUDING_PRE_EVALUATIONS;
					else subMode = OverwriteMode.NO_OVERWRITE;
					if(preProv != null) {
						final MultiKPIEvalConfiguration startConfigPre;
						if(overWritePreEvalSettings) {
							startConfigPre = evalScheduler.getOrCreateConfig(preProv.id(), startConfig.subConfigId().getValue(),
								startConfig.stepInterval().getValue(), true);
							configureDestination(startConfig, startConfigPre);
						} else
							startConfigPre = getFittingConfig(preProv.id(), startConfig);
						System.out.println("   ****    Queueing Pre-evaluation for "+startConfig.evaluationProviderId().getValue()+
								" with startConfig "+startConfigPre.getLocation()+ "(without gap check)");
						long startTimePre, endTimePre;
						switch(type) {
						case SAME:
							startTimePre = startTime;
							endTimePre = endTime;
							break;
						case AHEAD:
							//we assume that startTime is aligned
							startTimePre = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startTime, -1, startConfig.stepInterval().getValue());
							endTimePre = endTime;
							break;
						default:
							throw new IllegalStateException("Unknown IntervalRelation Type:"+type);
						}
						
						queueEvalConfig(startConfigPre, saveJsonResult, null, listener,
							startTimePre, endTimePre, dataProvidersToUse, true, subMode, overWritePreEvalSettings, runSingleGw, evalProvidersQueued);
					} else {
						appMan.getLogger().warn("Could not find Pre-Evaluation provider "+pre.getSourceProvider()+" for "+startConfig.getLocation());
					}
				}
			}
		}

		return evalScheduler.queueEvalConfigWithoutPreEval(startConfig, saveJsonResult, resultFileName, listener, startTime, endTime,
				dataProvidersToUse, runSingleGw);
	}

	private List<long[]> getGaps(List<KPIStatisticsManagementI> kpiUtils, int stepSize,
			long startTimeNonAligned, long endTime) {
		if(kpiUtils == null || kpiUtils.isEmpty()) {
			List<long[]> gaps;
			long startTime = AbsoluteTimeHelper.getIntervalStart(startTimeNonAligned, stepSize);
			gaps = new ArrayList<>();
			gaps.add(new long[] {startTime, endTime});
			return gaps;
		} else return kpiUtils.get(0).getGapTimes(stepSize, startTimeNonAligned, endTime, true);
		
	}
	
	private MultiKPIEvalConfiguration getFittingConfig(String destProviderId, MultiKPIEvalConfiguration superConfig) {
		String subConfigId = superConfig.subConfigId().getValue();
		String subConfigIdBase = subConfigId;
		MultiKPIEvalConfiguration startConfigPre = evalScheduler.getConfig(destProviderId, subConfigId);
		int idx = 0;
		while(startConfigPre != null && !(haveConfigsFittingPreEvalSettings(superConfig, startConfigPre))) {
			if(subConfigIdBase == null) subConfigId = subConfigIdBase = "autoPreEval";
			else {
				subConfigId = subConfigIdBase + "_"+idx;
				idx++;
			}
			startConfigPre = evalScheduler.getConfig(destProviderId, subConfigId);
		}
		if(startConfigPre == null) {
			startConfigPre = evalScheduler.getOrCreateConfig(destProviderId, subConfigId,
					superConfig.stepInterval().getValue(), true);
			configureDestination(superConfig, startConfigPre);
		}
		return startConfigPre;
	}
	
	private void configureDestination(MultiKPIEvalConfiguration superConfig, MultiKPIEvalConfiguration startConfigPre) {
		if(startConfigPre.resultsRequested().exists()) startConfigPre.resultsRequested().deactivate(false);
		if(superConfig.gwIds().exists()) startConfigPre.gwIds().setAsReference(superConfig.gwIds());
		if(superConfig.kpisToCalculate().exists()) startConfigPre.kpisToCalculate().setAsReference(superConfig.kpisToCalculate());		
	}
	
	private boolean haveConfigsFittingPreEvalSettings(MultiKPIEvalConfiguration superConfig, MultiKPIEvalConfiguration c2) {
		if(!Arrays.equals(superConfig.gwIds().getValues(), c2.gwIds().getValues())) return false;
		//For Pre-eval we always calulate all results
		if(c2.resultsRequested().isActive() && (c2.resultsRequested().getValues().length > 0)) return false;
		if(!Arrays.equals(superConfig.kpisToCalculate().getValues(), c2.kpisToCalculate().getValues())) return false;
		if(superConfig.stepInterval().getValue() != c2.stepInterval().getValue()) return false;
		return true;
	}

	/** Here we assume that the pre-evaluation data is available and that the data requested for the
	 * actual provider is not yet available, so the evaluation has to be actually started. We are not
	 * handling ongoing intervals here.
	 */
	@SuppressWarnings("rawtypes")
	protected void performEvaluation(MultiKPIEvalConfiguration evalConfig, Boolean writeJSON, String resultFileName,
			long startTime, long endTime, ResultHandler<GaRoMultiResult> callResultHandler,
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse,
			boolean performBlocking, String runSingleGw) {
		String evalProviderId = evalConfig.evaluationProviderId().getValue();
		GaRoSingleEvalProvider provider = evalScheduler.getProvider(evalProviderId);
		List<GaRoPreEvaluationProvider> preProvs = new ArrayList<>();
		if(provider instanceof GaRoSingleEvalProviderPreEvalRequesting) {
			GaRoSingleEvalProviderPreEvalRequesting peval = (GaRoSingleEvalProviderPreEvalRequesting)provider;
			if(peval.preEvaluationsRequested() != null) for(PreEvaluationRequested pre: peval.preEvaluationsRequested()) {
				GaRoSuperEvalResult<?> sres = evalResultMan.getAggregatedResult(pre.getSourceProvider(), startTime, endTime, true);
				if(sres == null) {
					throw new IllegalStateException("Super result null for "+pre.getSourceProvider());
				}
				@SuppressWarnings("unchecked")
				GaRoStdPreEvaluationProvider preProv = new GaRoStdPreEvaluationProvider(sres);
				preProvs.add(preProv);
			}
		}
		List<String> gwIds;
		if(runSingleGw != null) {
			gwIds = Arrays.asList(new String[] {runSingleGw});
		} else if(evalConfig.gwIds().isActive() && evalConfig.gwIds().getValues().length > 0) {
			gwIds = Arrays.asList(evalConfig.gwIds().getValues());
		} else gwIds = null;
		List<ConfigurationInstance> additionalConfigurations;
		if(evalConfig.configurationResource().isActive() && (!provider.getConfigurations().isEmpty())) {
			additionalConfigurations = Arrays.asList(new ConfigurationInstance[] {
					getResourceConfig(evalConfig.configurationResource(), provider.getConfigurations().get(0))});
		} else additionalConfigurations = null;	
		
		int stepSize = getStepSize(evalConfig);
		ResultHandler<GaRoMultiResult> externalResultHandler = new ResultHandler<GaRoMultiResult>() {

			@SuppressWarnings("unchecked")
			@Override
			public void resultAvailable(AbstractSuperMultiResult result2, String jsonFileNameSpecifiedInCall) {
				//write to KPIs
				for(Object ir2: result2.intervalResults) {
					if(ir2 instanceof AbstractMultiResult)
						((AbstractMultiResult)ir2).timeOfCalculation = appMan.getFrameworkTime();
				}
				evalScheduler.calculateKPIs(evalConfig, provider, result2,
						stepSize, runSingleGw, null);
				if(callResultHandler != null) callResultHandler.resultAvailable(result2, jsonFileNameSpecifiedInCall);
			}
			
		};
		List<ResultType> resultsRequested;
		if(evalConfig.resultsRequested().isActive() && evalConfig.resultsRequested().getValues().length > 0) {
			resultsRequested = new ArrayList<>();
			for(String s: evalConfig.resultsRequested().getValues()) {
				for(ResultType rt: provider.resultTypes()) {
					if(rt.id().equals(s)) {
						resultsRequested.add(rt);
						break;
					}
				}
			}
		} else {
			resultsRequested = provider.resultTypes();
		}
		evalResultMan.performGenericMultiEvalOverAllDataBlocking(provider.getClass(), startTime, endTime,
				KPIStatisticsUtil.getIntervalTypeChronoUnit(stepSize), preProvs.toArray(new GaRoPreEvaluationProvider[0]),
				resultsRequested , gwIds, resultFileName, evalProviderId,
				provider.getSuperResultClassForDeserialization(), externalResultHandler, writeJSON!=null?writeJSON:evalConfig.saveJsonResults().getValue(),
						exportDescriptor,
						dataProvidersToUse, additionalConfigurations, performBlocking);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ConfigurationInstance getResourceConfig(Resource configResource, Configuration<?> configuration) {
		return new ConfigurationInstance.GenericObjectConfiguration(
				configResource, configuration);
	}

	public int getStepSize(MultiEvalStartConfiguration startConfig) {
		if(!startConfig.stepInterval().isActive()) return stdStepSize;
		else {
			int stepSize = startConfig.stepInterval().getValue();
			if(KPIStatisticsUtil.getIntervalTypeChronoUnit(stepSize) == null) return stdStepSize;
			return stepSize;
		}
		
	}
}
