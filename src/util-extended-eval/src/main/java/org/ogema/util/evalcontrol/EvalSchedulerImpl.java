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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.ResourceNotFoundException;
import org.ogema.generictype.GenericDataTypeDeclaration.TypeCardinality;
import org.ogema.model.alignedinterval.StatisticalAggregation;
import org.ogema.model.jsonresult.IndividualGWResultList;
import org.ogema.model.jsonresult.JSONResultFileData;
import org.ogema.model.jsonresult.MultiEvalManagementData;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.resourcemanipulator.timer.CountDownAbsoluteTimer;
import org.ogema.util.directresourcegui.jsonkpi.KPIStatisticsJSONFileManagement;
import org.ogema.util.directresourcegui.kpi.KPIMonitoringReport;
import org.ogema.util.directresourcegui.kpi.KPIStatisticsUtil;
import org.ogema.util.jsonresult.kpi.JSONIndividualGWResultList;
import org.ogema.util.jsonresult.kpi.JSONMultiKPIEvalResult;
import org.ogema.util.jsonresult.kpi.JSONStatisticalAggregation;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.configuration.Configuration;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance.GenericObjectConfiguration;
import de.iwes.timeseries.eval.api.extended.MultiResult;
import de.iwes.timeseries.eval.api.extended.util.AbstractSuperMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.ResultHandler;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;

public class EvalSchedulerImpl implements EvalScheduler {
	protected final MultiEvalManagementData appData;
	protected final EvalResultManagement evalResultMan;
	//protected final GatewayBackupAnalysisAccess gatewayParser;
	protected final ApplicationManager appMan;
	protected final EvalSchedulingManagement schedMan;
	
	protected final KPIStatisticsJSONFileManagement kpiJsonMgmt;
	
	//protected boolean exportDescriptor = false;
	public void setDescriptorExportMode(boolean exportDescriptor) {
		schedMan.setDescriptorExportMode(exportDescriptor);
		//this.exportDescriptor = exportDescriptor;
	}
	
	protected List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse = null;
	
	protected class ProviderData {
		public ProviderData(GaRoSingleEvalProvider evalProvider) {
			super();
			this.evalProvider = evalProvider;
		}
		public GaRoSingleEvalProvider evalProvider;
		//kpiUtils may be null if not initialized for kpi calculation
		//public List<KPIStatisticsManagement> kpiUtils;
	}
	//providerId -> ProviderData
	protected final Map<String, ProviderData> knownProviders = new HashMap<>();
	//Location of ResourceList<StatisticalAggregation> -> KPIStatisticsManagement data
	protected final Map<String, List<KPIStatisticsManagementI>> kpiUtilsMap = new HashMap<>();
	
	protected final int stdStepSize;
	protected int defaultIntervalsToCalculateForAutoScheduling;

	//private String evalProviderId;
	public boolean stopSignal;
	public void close() {
		stopSignal = true;
		for(AutoQueueTimer at: nextAutoSchedulingTime.values()) {
			at.destroy();
		}
		nextAutoSchedulingTime.clear();
	}
	
	public EvalSchedulerImpl(MultiEvalManagementData appData, int stdStepSize, EvalResultManagement evalResultMan,
			 ApplicationManager appMan, int defaultIntervalsToCalculateForAutoScheduling) {
		this.appData = appData;
		this.evalResultMan = evalResultMan;
		//this.gatewayParser = gatewayParser;
		this.appMan = appMan;
		this.stdStepSize = stdStepSize;
		this.defaultIntervalsToCalculateForAutoScheduling = defaultIntervalsToCalculateForAutoScheduling;
		
		if(!appData.isActive() ||
				ValueResourceHelper.setIfNew(appData.performAutoEvaluation(), true)) {
			appData.multiConfigs().create();
			appData.activate(true);
		}
		
		this.schedMan = new EvalSchedulingManagement(evalResultMan, this,
				stdStepSize, appMan);
		kpiJsonMgmt = new KPIStatisticsJSONFileManagement(evalResultMan);
	}

	@Override
	public void registerEvaluationProvider(GaRoSingleEvalProvider evalProvider) {
		if(!knownProviders.containsKey(evalProvider.id())) {
			knownProviders.put(evalProvider.id(), new ProviderData(evalProvider));
			evalResultMan.registerClass(evalProvider.getSuperResultClassForDeserialization());
		}
		List<MultiKPIEvalConfiguration> configs = getConfigs(evalProvider.id());
		for(MultiKPIEvalConfiguration config: configs)
			activateAutoScheduling(evalProvider, config.subConfigId().getValue());
	}
	@Override
	public void unregisterEvaluationProvider(GaRoSingleEvalProvider evalProvider) {
		String providerId = evalProvider.id();
		deactivateAutoScheduling(providerId);
		knownProviders.remove(providerId);
	}

	@Override
	public GaRoSingleEvalProvider getProvider(String evalProviderId) {
		ProviderData pd = knownProviders.get(evalProviderId);
		if(pd == null) return null;
		return pd.evalProvider;
	}
	
	private int[] getIntervalTypesToUse(int[] additionalIntervalTypes, int baseInterval) {
		if(additionalIntervalTypes == null || (additionalIntervalTypes.length == 0))
			return new int[]{baseInterval};
		else if(additionalIntervalTypes[0] == baseInterval)
			return additionalIntervalTypes;
		else {
			int[] intervalTypesToUse = new int[additionalIntervalTypes.length+1];
			intervalTypesToUse[0] = baseInterval;
			for(int i=1; i<intervalTypesToUse.length; i++) intervalTypesToUse[i] = additionalIntervalTypes[i-1];
			return intervalTypesToUse;
		}
	}
	@Override
	public MultiKPIEvalConfiguration registerProviderForKPI(GaRoSingleEvalProvider eval, String subConfigId, boolean recursive,
			int[] additionalIntervalTypes, boolean queueOnStartup, String[] gwIds) {
		//List<MultiKPIEvalConfiguration> result = new ArrayList<>();
		registerEvaluationProvider(eval);
		MultiKPIEvalConfiguration pConfig = getOrCreateConfig(eval.id(), subConfigId, stdStepSize, true);
		ValueResourceHelper.setCreate(pConfig.queueOnStartup(), queueOnStartup);
		pConfig.kpisToCalculate().create();
		int[] intervalTypesToUse = getIntervalTypesToUse(additionalIntervalTypes, stdStepSize);
		pConfig.kpisToCalculate().setValues(intervalTypesToUse);
		pConfig.gwIds().create();
		pConfig.gwIds().setValues(gwIds);
		pConfig.performAutoQueuing().<BooleanResource>create().setValue(true);
			
		pConfig.activate(true);
		
		/*if(recursive && (eval instanceof GaRoSingleEvalProviderPreEvalRequesting)) {
			GaRoSingleEvalProviderPreEvalRequesting peval = (GaRoSingleEvalProviderPreEvalRequesting)eval;
			if(peval.preEvaluationsRequested() != null) for(PreEvaluationRequested pre: peval.preEvaluationsRequested()) {
				GaRoSingleEvalProvider preProv = getProvider(pre.getSourceProvider());
				//TODO: Hopefully we do not have recursive pre-evaluation dependenices
				if(preProv != null) result.addAll(registerProviderForKPI(preProv, subConfigId, true, intervalTypesToUse, queueOnStartup, gwIds));
				else {
					result.add(getOrCreateConfig(pre.getSourceProvider(), subConfigId, stdStepSize, true));
				}
			}
		}*/
		
		activateAutoScheduling(eval, subConfigId);
		
		return pConfig;
	}

	@Override
	public MultiKPIEvalConfiguration unregisterProviderForKPI(GaRoSingleEvalProvider eval, String subConfigId, boolean recursive) {
		//List<MultiKPIEvalConfiguration> result = new ArrayList<>();
		MultiKPIEvalConfiguration pConfig = getConfig(eval.id(), subConfigId);
		deactivateAutoScheduling(eval.id());
		if(pConfig != null) {
			pConfig.deactivate(false);
		}
		return pConfig;
		
		/*if(recursive && (eval instanceof GaRoSingleEvalProviderPreEvalRequesting)) {
			GaRoSingleEvalProviderPreEvalRequesting peval = (GaRoSingleEvalProviderPreEvalRequesting)eval;
			if(peval.preEvaluationsRequested() != null) for(PreEvaluationRequested pre: peval.preEvaluationsRequested()) {
				GaRoSingleEvalProvider preProv = getProvider(pre.getSourceProvider());
				//TODO: Hopefully we do not have recursive pre-evaluation dependenices
				if(preProv != null) result.addAll(unregisterProviderForKPI(preProv, subConfigId, true));
				else {
					MultiKPIEvalConfiguration preConfig = getConfig(pre.getSourceProvider(), subConfigId);
					if(preConfig != null) {
						preConfig.deactivate(false);
						result.add(pConfig);
					}
				}
			}
		}*/
		//return result;
	}
	
	@Override
	public MultiKPIEvalConfiguration getConfig(String evalProviderId, String subConfigId) {
		for(MultiKPIEvalConfiguration mkc: appData.multiConfigs().getAllElements()) {
			if(mkc.evaluationProviderId().getValue().equals(evalProviderId)) {
				if(subConfigId == null || subConfigId.isEmpty())
					return mkc;
				else if(mkc.subConfigId().getValue().equals(subConfigId)) return mkc;
			}
		}
		return null;
	}
	
	@Override
	public List<MultiKPIEvalConfiguration> getConfigs(String evalProviderId) {
		if(evalProviderId == null) return appData.multiConfigs().getAllElements();
		List<MultiKPIEvalConfiguration> result = new ArrayList<>();
		for(MultiKPIEvalConfiguration mkc: appData.multiConfigs().getAllElements()) {
			if(mkc.evaluationProviderId().getValue().equals(evalProviderId)) {
				result.add(mkc);
			}
		}
		return result;
	}

	@Override
	public MultiKPIEvalConfiguration getOrCreateConfig(String evalProviderId, String subConfigId) {
		MultiKPIEvalConfiguration result = getConfig(evalProviderId, subConfigId);
		if(result == null) {
			MultiKPIEvalConfiguration anyForProvider =  getConfig(evalProviderId, null);
			String baseName;
			if(subConfigId == null) baseName = evalProviderId;
			else baseName = evalProviderId+"_"+subConfigId;
			String name = ResourceListHelper.createNewDecoratorName(ResourceUtils.getValidResourceName(baseName), appData.multiConfigs());
			//TODO
			//Object object = appData.multiConfigs().add();
			//result = (MultiKPIEvalConfiguration) object;
			result = appData.multiConfigs().addDecorator(name, MultiKPIEvalConfiguration.class);
			if(subConfigId != null && (!subConfigId.isEmpty()))
				result.subConfigId().<StringResource>create().setValue(subConfigId);
			result.evaluationProviderId().<StringResource>create().setValue(evalProviderId);
			if(anyForProvider != null) {
				result.resultKPIs().setAsReference(anyForProvider.resultKPIs());
				result.individualResultKPIs().setAsReference(anyForProvider.individualResultKPIs());
				if(!anyForProvider.queueGatewaysIndividually().exists())
					anyForProvider.queueGatewaysIndividually().create();
				result.queueGatewaysIndividually().setAsReference(anyForProvider.queueGatewaysIndividually());

				GaRoSingleEvalProvider eval = getProvider(evalProviderId);
				if(eval != null) {
					Resource configRes = evalResultMan.getEvalScheduler().getConfigurationResource(anyForProvider, eval);
					if(configRes != null && configRes.exists())
						result.configurationResource().setAsReference(configRes);
				}
			} else {
				result.resultKPIs().create();
				result.individualResultKPIs().create();
			}
		}
		return result;
	}

	@Override
	public MultiKPIEvalConfiguration getOrCreateConfig(String evalProviderId, String subConfigId, Integer stepInterval,
			boolean forceValues) {
		return getOrCreateConfig(evalProviderId, subConfigId, stepInterval, null, forceValues);
	}
	
	@Override
	public MultiKPIEvalConfiguration getOrCreateConfig(String evalProviderId, String subConfigId, Integer stepInterval,
			int[] additionalIntervalTypes, boolean forceValues) {
		MultiKPIEvalConfiguration result = getOrCreateConfig(evalProviderId, subConfigId);
		if(forceValues || (!result.stepInterval().exists())) {
			if(stepInterval != null)
				result.stepInterval().<IntegerResource>create().setValue(stepInterval);
			else
				result.stepInterval().<IntegerResource>create().setValue(stdStepSize);
			if(additionalIntervalTypes != null) {
				result.kpisToCalculate().create();
				int[] intervalTypesToUse = getIntervalTypesToUse(additionalIntervalTypes,
						result.stepInterval().getValue());
				result.kpisToCalculate().setValues(intervalTypesToUse);				
			}
		}
		result.activate(true);
		return result;
	}

	@Override
	public MultiKPIEvalConfiguration removeConfig(String evalProviderId, String subConfigId) {
		MultiKPIEvalConfiguration result = getConfig(evalProviderId, subConfigId);
		if(result != null) {
			GaRoSingleEvalProvider prov = getProvider(evalProviderId);
			if(prov != null)
				unregisterProviderForKPI(prov, subConfigId, false);
			result.delete();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T extends Resource> T getConfigurationResource(MultiKPIEvalConfiguration evalConfig, GaRoSingleEvalProvider eval) {
		List<Configuration<?>> configs = eval.getConfigurations();
		if(configs.isEmpty()) return null;
		Configuration<?> configLoc2= configs.get(0);
		ConfigurationInstance ci = configLoc2.defaultValues();
		if(ci instanceof GenericObjectConfiguration) {
			GenericObjectConfiguration<?> goc = (GenericObjectConfiguration<?>)ci;
			Object typeIn = goc.getValue();
			if(!(typeIn instanceof Class)) {
				throw new IllegalStateException("Expecting resource type!");
			}
			Class<T> type = (Class<T>)typeIn;
			
			try {
				Resource configRes = evalConfig.addDecorator("configurationResource", type);
				return (T) configRes;
			} catch(ResourceNotFoundException e) {
				appMan.getLogger().warn("Existing config type:"+evalConfig.configurationResource().getResourceType().getName()+", expected:"+type.getName(), e);
				return null;
			}
		}
		return null;		
	}
	
	private static class MultiEvalRun implements MultiEvalRunI {
		public MultiEvalRun(MultiKPIEvalConfiguration config, Boolean saveJsonResult, String resultFileName,
				ResultHandler<?> listener,
				long startTime, long endTime, List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse,
				String runSingleGw) {
			this.config = config;
			this.saveJsonResult = saveJsonResult;
			this.resultFileName = resultFileName;
			this.listener = listener;
			this.startTime = startTime;
			this.endTime = endTime;
			this.dataProvidersToUse = dataProvidersToUse;
			this.runSingleGw = runSingleGw;
		}
		final MultiKPIEvalConfiguration config;
		final Boolean saveJsonResult;
		final String resultFileName;
		final ResultHandler<?> listener;
		final long startTime;
		final long endTime;
		final List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse;
		final String runSingleGw;
		
		public MultiKPIEvalConfiguration getConfig() {
			return config;
		}
		public Boolean getSaveJsonResult() {
			return saveJsonResult;
		}
		public ResultHandler<?> getListener() {
			return listener;
		}
		public long getStartTime() {
			return startTime;
		}
		public long getEndTime() {
			return endTime;
		}
		public List<GaRoMultiEvalDataProvider<?>> getDataProvidersToUse() {
			return dataProvidersToUse;
		}
		@Override
		public long startTimeOfCalculation() {
			// TODO Auto-generated method stub
			return 0;
		}
		@Override
		public long calculationDuration() {
			// TODO Auto-generated method stub
			return 0;
		}
	}
	
	@Override
	public List<MultiEvalRunI> getQueueElements() {
		if(!isQueueThreadRunning) return null;
		return new ArrayList<>(qThread.queue);
	}
	@Override
	public MultiEvalRunI getElementCurrentlyExecuted() {
		if(!isQueueThreadRunning) return null;
		return qThread.mrun;
	}
	
	volatile boolean isQueueThreadRunning = false;
	volatile QueueThread qThread = null;
	
	class QueueThread implements Callable<Void> {
		private LinkedList<MultiEvalRun> queue = new LinkedList<>(); //Collections.synchronizedList(new LinkedList<>());
		private MultiEvalRun mrun;
		
		public void addElement(MultiEvalRun newRun) {
			synchronized(queue) {
				queue.add(newRun);
			}
		}
		public MultiEvalRun fetchFirst() {
			synchronized(queue) {
				if(queue.isEmpty()) return null;
				return queue.remove();
			}
		}
		
		public QueueThread(MultiEvalRun firstRun) {
			addElement(firstRun);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Void call() throws Exception {
			//isQueueThreadRunning = true;
			mrun = fetchFirst();
			while(mrun != null && (!stopSignal)) {
				mrun.config.isRunning().setValue(true);
				System.out.println("Start queued evaluation for "+mrun.config.getLocation());
				try {
				schedMan.performEvaluation(mrun.config, mrun.saveJsonResult, mrun.resultFileName,
						mrun.startTime, mrun.endTime,
						(ResultHandler<GaRoMultiResult>) mrun.listener, mrun.dataProvidersToUse, true,
						mrun.runSingleGw);
				//Check if message shall be generated
				
				} catch(Exception e) {
					e.printStackTrace();
					System.out.println("Caught Exception in Eval, will continue with next task");
				}
				System.out.println("Finish queued evaluation for "+mrun.config.getLocation());
				mrun.config.isRunning().setValue(false);
				mrun = fetchFirst();
			}
			isQueueThreadRunning = false;
			return null;
		}
		
	}
	
	@Override
	public long[] queueEvalConfig(String evalProvider, String subConfigId, Boolean saveJsonResult, ResultHandler<?> listener, int defaultIntervalsToCalculate) {
		MultiKPIEvalConfiguration config = getConfig(evalProvider, subConfigId);
		if(config == null) return null;
		return queueEvalConfig(config, saveJsonResult, listener, defaultIntervalsToCalculate, false,
				OverwriteMode.ONLY_PROVIDER_REQUESTED, true);
	}

	/**
	 * 
	 * @param startConfig
	 * @param endTime must be aligned
	 * @return
	 */
	private long getStartTime(MultiKPIEvalConfiguration startConfig, long endTime, int defaultIntervalsToCalculate,
			boolean forceRecalculation) {
		long lastEnd = 0;
		List<JSONResultFileData> exist = evalResultMan.getDataOfProvider(startConfig.evaluationProviderId().getValue());
		for(JSONResultFileData ex: exist) {
			if(ex.endTime().getValue() > lastEnd) lastEnd = ex.endTime().getValue();
		}
		if(exist.isEmpty() || (lastEnd == 0) || ((lastEnd >= endTime) && forceRecalculation))
			return AbsoluteTimeHelper.addIntervalsFromAlignedTime(endTime, -defaultIntervalsToCalculate,
				startConfig.stepInterval().getValue());
		if(lastEnd >= endTime) {
			return -1;
			//throw new IllegalStateException("There is data stored for the future for "+startConfig.getLocation());
		}
		return AbsoluteTimeHelper.getIntervalStart(lastEnd+1, startConfig.stepInterval().getValue());
	}
	
	@Override
	public long[] queueEvalConfig(MultiKPIEvalConfiguration startConfig, boolean saveJsonResult,
			ResultHandler<?> listener, int defaultIntervalsToCalculate, boolean queuePreEvals,
			OverwriteMode overwriteMode, boolean overWritePreEvalSettings) {
		long[] startEnd = getStandardStartEndTime(startConfig, defaultIntervalsToCalculate, false);
		if(startEnd == null || startEnd[0] >= startEnd[1]) return null;
		return queueEvalConfig(startConfig, saveJsonResult, listener, startEnd[0], startEnd[1],
				dataProvidersToUse, queuePreEvals, overwriteMode, overWritePreEvalSettings);
	}
	
	@Override
	public long[] queueEvalConfig(MultiKPIEvalConfiguration startConfig, boolean saveJsonResult,
			ResultHandler<?> listener, long startTime, long endTime,
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse, boolean queuePreEvals,
			OverwriteMode overwriteMode, boolean overWritePreEvalSettings) {
		return queueEvalConfig(startConfig, saveJsonResult, null, listener, startTime, endTime,
				dataProvidersToUse, queuePreEvals, overwriteMode, overWritePreEvalSettings, null, null);
	}
	@Override
	public long[] queueEvalConfig(MultiKPIEvalConfiguration startConfig, boolean saveJsonResult,
			String resultFileName, ResultHandler<?> listener, long startTime, long endTime,
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse, boolean queuePreEvals,
			OverwriteMode overwriteMode, boolean overWritePreEvalSettings, String runSingleGw,
			Set<String> evalProvidersQueued) {
		return schedMan.queueEvalConfig(startConfig, saveJsonResult, resultFileName, listener, startTime, endTime,
				dataProvidersToUse, queuePreEvals, overwriteMode, overWritePreEvalSettings,
				runSingleGw, evalProvidersQueued);
	}
	
	@Override
	public long[] queueEvalConfigWithoutPreEval(MultiKPIEvalConfiguration startConfig, boolean saveJsonResult, String resultFileName,
			ResultHandler<?> listener, long startTime, long endTime,
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse, String runSingleGw) {
		MultiEvalRun mrun = new MultiEvalRun(startConfig, saveJsonResult, resultFileName, listener, startTime, endTime, dataProvidersToUse,
				runSingleGw);
		if(!isQueueThreadRunning) {
			isQueueThreadRunning = true;
			System.out.println("Starting new Thread for eval of "+mrun.config.getLocation());
			qThread = new QueueThread(mrun);
			Executors.newSingleThreadExecutor().submit(qThread);
		} else {
			System.out.println("Adding to existing Thread for eval: "+mrun.config.getLocation());
			qThread.addElement(mrun);
		}
		return new long[] {startTime, endTime};
	}
	
	@Override
	public long[] getStandardStartEndTime(MultiKPIEvalConfiguration startConfig, int defaultIntervalsToCalculate,
			boolean forceRecalculation) {
		long endTime = AbsoluteTimeHelper.getIntervalStart(appMan.getFrameworkTime(), startConfig.stepInterval().getValue());
		final long startTime = getStartTime(startConfig, endTime, defaultIntervalsToCalculate, forceRecalculation);
		if(startTime <=0 || startTime >= endTime) return null;
		return new long[] {startTime, endTime};
	}
	
	//////////////////////////////
	// Auto-Evaluation API Impl
	//////////////////////////////
	
	@Override
	public MultiKPIEvalConfiguration activateAutoEvaluation(String evalProviderId, String subConfigId) {
		MultiKPIEvalConfiguration result = getConfig(evalProviderId, subConfigId);
		if(result == null) return null;
		result.kpisToCalculate().activate(false);
		result.performAutoQueuing().<BooleanResource>create().setValue(true);
		if(!result.performAutoQueuing().isActive()) result.performAutoQueuing().activate(false);
		GaRoSingleEvalProvider eval = getProvider(evalProviderId);
		if(eval != null) activateAutoScheduling(eval, subConfigId);
		return result ;
	}

	@Override
	public MultiKPIEvalConfiguration deactivateAutoEvaluation(String evalProviderId, String subConfigId) {
		MultiKPIEvalConfiguration result = getConfig(evalProviderId, subConfigId);
		if(result == null) return null;
		result.kpisToCalculate().deactivate(false);
		result.performAutoQueuing().<BooleanResource>create().setValue(false);
		deactivateAutoScheduling(evalProviderId);
		return result ;
	}
	

	@Override
	public boolean activateAutoEvaluation() {
		if(!appData.performAutoEvaluation().<BooleanResource>create().setValue(true)) return false;
		appData.performAutoEvaluation().activate(false);
		//We start auto-queueing for maximum one configuration per provider
		Set<String> providersDone = new HashSet<>();
		for(MultiKPIEvalConfiguration mkc: appData.multiConfigs().getAllElements()) {
			if(mkc.performAutoQueuing().isActive() && mkc.performAutoQueuing().getValue()) {
				String provider = mkc.evaluationProviderId().getValue();
				if(providersDone.contains(provider)) continue;
				providersDone.add(provider);
				GaRoSingleEvalProvider eval = getProvider(provider);
				startAutoScheduling(eval, mkc);
			}
		}
		//for(ProviderData p: knownProviders.values()) {
			//TODO: This will just test a random configuration of the provider if there
			//are several sub-configurations
		//}
		//throw new UnsupportedOperationException("not implemented yet!");
		//appData.performAutoEvaluation().setValue(true);
		return true;
	}

	@Override
	public boolean isAutoEvaluationActive() {
		return (!appData.performAutoEvaluation().isActive()) || appData.performAutoEvaluation().getValue();
	}
	
	@Override
	public boolean deactivateAutoEvaluation() {
		if(!appData.performAutoEvaluation().<BooleanResource>create().setValue(false)) return false;
		appData.performAutoEvaluation().activate(false);
		for(String e: nextAutoSchedulingTime.keySet()) {
			deactivateAutoScheduling(e);
		}
		//throw new UnsupportedOperationException("not implemented yet!");
		//appData.performAutoEvaluation().<BooleanResource>create().setValue(false);
		return true;
	}

	@Override
	public void registerAutoEvalResultListener(ResultHandler<?> listener) {
		throw new UnsupportedOperationException("not implemented yet!");	
	}

	@Override
	public List<KPIStatisticsManagementI> calculateKPIs(MultiKPIEvalConfiguration evalConfig,
			GaRoSingleEvalProvider provider,
			AbstractSuperMultiResult<MultiResult> result,
			int baseInterval,
			String runSingleGw, String[] gwIds) {
		List<KPIStatisticsManagementI> kpiUtils;
		
		//make sure cached data is not re-used, otherwise new KPIs are not written
		/*ResourceList<StatisticalAggregation> resultKPIs = evalConfig.resultKPIs();
		kpiUtilsMap.remove(resultKPIs.getLocation());
		for(IndividualGWResultList ind: evalConfig.individualResultKPIs().getAllElements()) {
			resultKPIs = ind.data();
			kpiUtilsMap.remove(resultKPIs.getLocation());
		}*/
		List<String> forceResultIds = Arrays.asList(evalConfig.resultsRequested().getValues()); //new ArrayList<>();

		if(runSingleGw != null) {
			kpiUtils = configureKPIManagementAndWriteKPIsSingle(evalConfig, result, provider, runSingleGw, baseInterval, forceResultIds);
			//kpiUtils = configureKPIManagement(evalConfig, provider, runSingleGw); //kpiUtilsMap.get(evalConfig.getLocation());
		} else {
			if(evalConfig.queueGatewaysIndividually().getValue()) {
				if(gwIds == null) gwIds = evalConfig.gwIds().getValues(); 
				for(String gw: gwIds) {
					kpiUtils = configureKPIManagementAndWriteKPIsSingle(evalConfig, result, provider, gw, baseInterval, forceResultIds);
					//kpiUtils = configureKPIManagement(evalConfig, provider, gw);
				}
			}
			kpiUtils = configureKPIManagementAndWriteKPIsSingle(evalConfig, result, provider, null, baseInterval, forceResultIds);
			//kpiUtils = configureKPIManagement(evalConfig); //kpiUtilsMap.get(evalConfig.getLocation());
		}
		if(Boolean.getBoolean("org.ogema.util.extended.eval.saveKPIasJSON")) {
			kpiJsonMgmt.saveJSONMultiKPIEvalResult(evalConfig, 10);
		}
		
		return kpiUtils;
	}
	
	private List<KPIStatisticsManagementI> configureKPIManagementAndWriteKPIsSingle(
			MultiKPIEvalConfiguration evalConfig,
			AbstractSuperMultiResult<MultiResult> result,
			GaRoSingleEvalProvider provider, String singleGw, int baseInterval, Collection<String> forceResultIds) {
		List<KPIStatisticsManagementI> kpiUtils;
		
		if(singleGw != null) {
			kpiUtils = configureKPIManagement(evalConfig, provider, singleGw, forceResultIds); //kpiUtilsMap.get(evalConfig.getLocation());
		} else {
			kpiUtils = configureKPIManagement(evalConfig, forceResultIds); //kpiUtilsMap.get(evalConfig.getLocation());
		}
		
		//Here we only have to set the first one, others will be processed via moreResultsOfProvider
		if((kpiUtils != null)&&(!kpiUtils.isEmpty())) {
			int[] kpisToCalculate = evalConfig.kpisToCalculate().getValues();
			int[] intervalTypesToUse = getIntervalTypesToUse(kpisToCalculate, baseInterval);
			kpiUtils.get(0).writeKPIs(result, intervalTypesToUse, singleGw);
		}
		return kpiUtils;
	}
	
	//***************************************************
	// Auto-Scheduling Implementation
	//***************************************************
	
	//We need this map to find ongoing timers that can be killed when auto-scheduling shall be stopped
	//providerId -> time
	private ConcurrentHashMap<String, AutoQueueTimer> nextAutoSchedulingTime = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Boolean> stopAutoTimer = new ConcurrentHashMap<>();
	
	// This is the standard queueing configuration used by auto-scheduling
	@Override
	public void queueAutoEvalConfig(MultiKPIEvalConfiguration config) {
		EvalSchedulerImpl.this.queueEvalConfig(config, config.saveJsonResults().getValue(), null,
				defaultIntervalsToCalculateForAutoScheduling, true, OverwriteMode.NO_OVERWRITE, false);
	}
	
	//* Start auto-scheduling if not started before. The stepInterval should be set if the respective
	//MultiKPIEvalConfiguration is active at all
	private void activateAutoScheduling(GaRoSingleEvalProvider eval, String subConfigId) {
		if(appData.performAutoEvaluation().isActive() && (!appData.performAutoEvaluation().getValue())) return;
		String providerId = eval.id();
		if(nextAutoSchedulingTime.get(providerId) != null) return;
		MultiKPIEvalConfiguration config = getConfig(providerId, subConfigId);
		startAutoScheduling(eval, config);
	}
	private void startAutoScheduling(GaRoSingleEvalProvider eval, MultiKPIEvalConfiguration config) {
		String providerId = eval.id();
		if(config == null || (!config.isActive())) return;
		if((!config.performAutoQueuing().isActive()) ||
				(!config.performAutoQueuing().getValue())) return;
		if(!config.kpisToCalculate().isActive()) return;
		
		registerKPIUtils(config, eval, null);
		//ProviderData pd = knownProviders.get(providerId);
		//if(pd.kpiUtils == null)
		//	pd.kpiUtils = KPIStatisticsUtil.setupKPIUtilsForProvider(eval, config.resultKPIs());
		
		if(config.queueOnStartup().getValue()) {
			queueAutoEvalConfig(config);
		}
		//long nextTime = AbsoluteTimeHelper.getNextStepTime(appMan.getFrameworkTime(), config.stepInterval().getValue());
		AutoQueueTimer autoTimer = new AutoQueueTimer(appMan, providerId, config);
		nextAutoSchedulingTime.put(providerId, autoTimer);
	}

	/** We need the provider here as we have to get all resultIds if the evalConfig does not
	 * 	contain this information
	 */
	private List<KPIStatisticsManagementI> registerKPIUtils(MultiKPIEvalConfiguration evalConfig,
			GaRoSingleEvalProvider eval, Collection<String> forceResultIds) {
		if(Boolean.getBoolean("org.ogema.util.extended.eval.saveKPIasJSON")) {
			JSONMultiKPIEvalResult jmul = kpiJsonMgmt.getOrLoadOrCreateResult(evalConfig);
			if(jmul.resultKPIs == null) jmul.resultKPIs = new HashMap<>();
			if(forceResultIds != null) forceResultIds(jmul.resultKPIs, forceResultIds);
			
			String listLoc = evalConfig.getLocation();
			return registerKPIUtilsJSON(evalConfig, eval, jmul.resultKPIs, listLoc, (forceResultIds!=null));
		} else
			return registerKPIUtils(evalConfig, eval, evalConfig.resultKPIs(), (forceResultIds!=null));
	}
	private List<KPIStatisticsManagementI> registerKPIUtils(MultiKPIEvalConfiguration evalConfig,
			GaRoSingleEvalProvider eval, ResourceList<StatisticalAggregation> resultKPIs, boolean forceRefresh) {
		//String providerId = eval.id();
		//ProviderData pd = knownProviders.get(providerId);
		List<KPIStatisticsManagementI> kpiUtils;
		if(forceRefresh) kpiUtils = null;
		else kpiUtils = kpiUtilsMap.get(resultKPIs.getLocation());
		/*if(pd == null) {
			pd = new ProviderData(eval);
			knownProviders.put(providerId, pd);
		}*/
		if(kpiUtils == null) {
			List<String> resultIdsAll = null;
			if(evalConfig.resultsRequested().isActive())
				resultIdsAll = Arrays.asList(evalConfig.resultsRequested().getValues());
			else {
				resultIdsAll = new ArrayList<>();
				for(ResultType r: eval.resultTypes()) {
					resultIdsAll.add(r.id());
				}
			}
			List<String> resultIds = new ArrayList<>();
			for(String rid: resultIdsAll) {
				ResultType type = KPIMonitoringReport.getResultTypeById(eval, rid);
				if(type != null && (type instanceof GaRoDataTypeI) && 
						(((GaRoDataTypeI)type).typeCardinality() != TypeCardinality.SINGLE_VALUE))
					continue;
				resultIds.add(rid);
			}
			//MultiKPIEvalConfiguration config = getConfig(providerId, evalConfig.subConfigId().getValue());
			kpiUtils = KPIStatisticsUtil.setupKPIUtilsForProvider(evalConfig.evaluationProviderId().getValue(),
					resultKPIs, //evalConfig.resultKPIs(),
					resultIds);
			kpiUtilsMap.put(resultKPIs.getLocation(), kpiUtils);
		}
		return kpiUtils;
	}
	private List<KPIStatisticsManagementI> registerKPIUtilsJSON(MultiKPIEvalConfiguration evalConfig,
			GaRoSingleEvalProvider eval, Map<String, JSONStatisticalAggregation> resultKPIs,
			String listLocation, boolean forceRefresh) {
		//String providerId = eval.id();
		//ProviderData pd = knownProviders.get(providerId);
		List<KPIStatisticsManagementI> kpiUtils;
		if(forceRefresh) kpiUtils = null;
		else kpiUtils = kpiUtilsMap.get(listLocation);
		/*if(pd == null) {
			pd = new ProviderData(eval);
			knownProviders.put(providerId, pd);
		}*/
		if(kpiUtils == null) {
			//TODO: This might have to be changed also for non-JSON registerKPIUtils
			Set<String> resultIdsAll = resultKPIs.keySet();
			/*if(evalConfig.resultsRequested().isActive())
				resultIdsAll = Arrays.asList(evalConfig.resultsRequested().getValues());
			else {
				resultIdsAll = new ArrayList<>();
				for(ResultType r: eval.resultTypes()) {
					resultIdsAll.add(r.id());
				}
			}*/
			List<String> resultIds = new ArrayList<>();
			for(String rid: resultIdsAll) {
				ResultType type = KPIMonitoringReport.getResultTypeById(eval, rid);
				if(type != null && (type instanceof GaRoDataTypeI) && 
						(((GaRoDataTypeI)type).typeCardinality() != TypeCardinality.SINGLE_VALUE))
					continue;
				resultIds.add(rid);
			}
			//MultiKPIEvalConfiguration config = getConfig(providerId, evalConfig.subConfigId().getValue());
			kpiUtils = KPIStatisticsUtil.setupKPIUtilsForProviderJSON(evalConfig.evaluationProviderId().getValue(),
					resultKPIs, //evalConfig.resultKPIs(),
					resultIds);
			kpiUtilsMap.put(listLocation, kpiUtils);
		}
		return kpiUtils;
	}
	
	private void deactivateAutoScheduling(String providerId) {
		AutoQueueTimer auto = nextAutoSchedulingTime.remove(providerId);
		if(auto != null) auto.destroy();
	}

	private static long getNextAutoQueueTime(ApplicationManager appMan,
			MultiKPIEvalConfiguration config) {
		Long autoQueueStep = Long.getLong("org.ogema.util.evalcontrol.development.autoQueueRepeatRate");
		if(autoQueueStep != null) return appMan.getFrameworkTime()+autoQueueStep;
		long nextTime = AbsoluteTimeHelper.getNextStepTime(appMan.getFrameworkTime(), config.stepInterval().getValue());
		long retard = Long.getLong("org.ogema.eval.utilextended.autoevalretardmilli", AbsoluteTimeHelper.getStandardInterval(config.stepInterval().getValue())/4);
		nextTime += retard;
		appMan.getLogger().info("Current Time of scheduling: "+TimeUtils.getDateAndTimeString(appMan.getFrameworkTime()));
		appMan.getLogger().info("Next Auto-Eval scheduled at "+TimeUtils.getDateAndTimeString(nextTime)+" for "+config.getLocation()+"("+config.evaluationProviderId().getValue()+")");
		return nextTime;		
	}
	
	class AutoQueueTimer extends CountDownAbsoluteTimer {
		
		public AutoQueueTimer(ApplicationManager appMan, String providerId,
				MultiKPIEvalConfiguration config) {
			super(appMan, getNextAutoQueueTime(appMan, config), new TimerListener() {
				
				@Override
				public void timerElapsed(Timer timer) {
					Boolean stopSignal = stopAutoTimer.get(providerId);
					if((stopSignal != null && stopSignal)) return;

					queueAutoEvalConfig(config);
					AutoQueueTimer autoTimer = new AutoQueueTimer(appMan,providerId, config);
					nextAutoSchedulingTime.put(providerId, autoTimer);
				}
			});
		}
		
	}

	@Override
	public List<KPIStatisticsManagementI> configureKPIManagement(String providerId) {
		GaRoSingleEvalProvider eval = getProvider(providerId);
		if(eval == null) return Collections.emptyList();
		List<MultiKPIEvalConfiguration> configs = getConfigs(providerId);
		List<KPIStatisticsManagementI> result = new ArrayList<>();
		for(MultiKPIEvalConfiguration config: configs) {
			if(config == null || (!config.isActive())) continue;
			result.addAll(configureKPIManagement(config, eval));
		}
		return result;
		//ProviderData pd = knownProviders.get(providerId);
		//if(pd.kpiUtils == null) {
		//	GaRoSingleEvalProvider eval = getProvider(providerId);
		//	MultiKPIEvalConfiguration config = getConfig(providerId);
		//	if(eval != null) pd.kpiUtils = KPIStatisticsUtil.setupKPIUtilsForProvider(eval, config.resultKPIs());
		//}
		//return pd.kpiUtils;
	}
	
	@Override
	public List<KPIStatisticsManagementI> configureKPIManagement(MultiKPIEvalConfiguration config, GaRoSingleEvalProvider eval) {
		return configureKPIManagement(config, eval, null, null);		
	}
	
	public ResourceList<StatisticalAggregation> getKPIStats(MultiKPIEvalConfiguration config, GaRoSingleEvalProvider eval,
			String subGateway) {
		if(!config.queueGatewaysIndividually().getValue()) return null;
		ResourceList<StatisticalAggregation> kpiStats = null;
		String name = ResourceUtils.getValidResourceName(subGateway);
		for(IndividualGWResultList rlist: config.individualResultKPIs().getAllElements()) {
			if(rlist.getName().equals(name)) {
				kpiStats = rlist.data();
				break;
			}
		}
		if(kpiStats == null) {
			IndividualGWResultList indGwResList = config.individualResultKPIs().addDecorator(name, IndividualGWResultList.class);
			indGwResList.data().create();
			indGwResList.data().setElementType(StatisticalAggregation.class);
			indGwResList.activate(true);
			kpiStats = indGwResList.data();
		}
		return kpiStats;
	}
	public Map<String, JSONStatisticalAggregation> getKPIStatsJSON(MultiKPIEvalConfiguration config, GaRoSingleEvalProvider eval,
			String subGateway, Collection<String> forceResultIds) {
		if(!config.queueGatewaysIndividually().getValue()) return null;
		Map<String, JSONStatisticalAggregation>  kpiStats = null;
		String name = ResourceUtils.getValidResourceName(subGateway);
		JSONMultiKPIEvalResult jmul = kpiJsonMgmt.getOrLoadOrCreateResult(config);
		
		if(jmul.individualResultKPIs != null) for(Entry<String, JSONIndividualGWResultList> rlist: jmul.individualResultKPIs.entrySet()) {
			if(rlist.getKey().equals(name)) {
				kpiStats = rlist.getValue().data;
				break;
			}
		}
		if(kpiStats == null) {
			if(jmul.individualResultKPIs == null) jmul.individualResultKPIs = new HashMap<>();
			JSONIndividualGWResultList indGwResList = new JSONIndividualGWResultList();
			jmul.individualResultKPIs.put(name, indGwResList);
			kpiStats = indGwResList.data;
		}
		if(forceResultIds != null) forceResultIds(kpiStats, forceResultIds);
		
		return kpiStats;
	}
	
	private void forceResultIds(Map<String, JSONStatisticalAggregation> data, Collection<String> forceResultIds) {
		for(String result: forceResultIds) {
			if(data.get(result) == null) {
				data.put(result, new JSONStatisticalAggregation());
			}
		}
	}
	
	@Override
	public List<KPIStatisticsManagementI> configureKPIManagement(MultiKPIEvalConfiguration config, GaRoSingleEvalProvider eval,
			String subGateway, Collection<String> forceResultIds) {
		if(Boolean.getBoolean("org.ogema.util.extended.eval.saveKPIasJSON")) {
			if(subGateway == null) return registerKPIUtils(config, eval, forceResultIds);
			Map<String, JSONStatisticalAggregation> kpiStats = getKPIStatsJSON(config, eval, subGateway, forceResultIds);
			if(kpiStats == null) return null;
			String loc = config.getLocation()+"_"+subGateway;
			return registerKPIUtilsJSON(config, eval, kpiStats, loc, (forceResultIds != null));			
		} else {
			if(subGateway == null) return registerKPIUtils(config, eval, forceResultIds);
			ResourceList<StatisticalAggregation> kpiStats = getKPIStats(config, eval, subGateway);
			if(kpiStats == null) return null;
			return registerKPIUtils(config, eval, kpiStats, (forceResultIds != null));
		}
	}
	
	/** Public only for EvalSchedulingManagement. Note that this version only configures the KPIStatisticsManagement
	 * for overall KPIs, not for gateway-specific KPIs
	 * 
	 * @param config
	 * @param forceResultIds 
	 * @return
	 */
	public List<KPIStatisticsManagementI> configureKPIManagement(MultiKPIEvalConfiguration config,
			Collection<String> forceResultIds) {
		//if(!config.kpisToCalculate().isActive()) return Collections.emptyList();
		GaRoSingleEvalProvider eval = getProvider(config.evaluationProviderId().getValue());
		return registerKPIUtils(config, eval, forceResultIds);				
	}
	
	@Override
	public Collection<String> getIndividualGatewayList(MultiKPIEvalConfiguration config) {
		if(Boolean.getBoolean("org.ogema.util.extended.eval.saveKPIasJSON")) {
			return kpiJsonMgmt.getGateways(config);
		} else {
			List<String> gws;
			if(config.individualResultKPIs().size() > 0) {
				gws = new ArrayList<>();
				for(IndividualGWResultList s: config.individualResultKPIs().getAllElements()) gws.add(s.getName());
			} else gws = Arrays.asList(config.gwIds().getValues());
			return gws;
		}
		
	}

	@Override
	public void setStandardDataProvidersToUse(List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse) {
		this.dataProvidersToUse = dataProvidersToUse;
	}
	
	@Override
	public Map<String, Long> getNextExecutionTimes() {
		Map<String, Long> execTimes = new HashMap<String, Long>();
		
		nextAutoSchedulingTime.forEach( (eventId, timer) -> {
			execTimes.put(eventId, timer.getExecutionTime());
		});
		
		return execTimes;
	}
	
}
