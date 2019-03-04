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
package com.iee.app.evaluationofflinecontrol;

import java.io.File;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTimeZone;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.ogema.model.action.Action;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.resourcemanipulator.timer.CountDownAbsoluteTimer;
import org.ogema.util.directresourcegui.kpi.KPIStatisticsUtil;
import org.ogema.util.evalcontrol.EvalScheduler;
import org.ogema.util.evalcontrol.EvalScheduler.OverwriteMode;
import org.smartrplace.util.message.MessageImpl;

import com.iee.app.evaluationofflinecontrol.config.KPIPageConfig;
import com.iee.app.evaluationofflinecontrol.config.OfflineEvaluationControlConfig;
import com.iee.app.evaluationofflinecontrol.config.ProviderEvalOfflineConfig;
import com.iee.app.evaluationofflinecontrol.config.ResultToShow;
import com.iee.app.evaluationofflinecontrol.gui.KPIPageGWOverviewMultiKPI;
import com.iee.app.evaluationofflinecontrol.gui.KPIPageGWOverviewMultiKPI.KPIColumn;
import com.iee.app.evaluationofflinecontrol.gui.KPIPageGWOverviewMultiKPI.PageConfig;
import com.iee.app.evaluationofflinecontrol.gui.MainPage;
import com.iee.app.evaluationofflinecontrol.gui.element.RemoteSlotsDBBackupButton;
import com.iee.app.evaluationofflinecontrol.util.ScheduleViewerConfigProvEvalOff;
import com.iee.app.evaluationofflinecontrol.util.StandardConfigurations;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider.KPIMessageDefinitionProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider.KPIPageDefinition;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider.MessageDefinition;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GatewayDataExportI;
import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;

// here the controller logic is implemented
public class OfflineEvaluationControlController {
	public static final String APP_EVAL_SUBID = "OfflineEvaluationControl";
	public static final String APP_AUTO_SUBID = "OfflineEvaluationControl_Auto";
	public static final String GaRoMultiEvalDataProviderJAXB_PROVIDER_ID = "GaRoMultiEvalDataProviderJAXB";
	
	public OgemaLogger log;
    public ApplicationManager appMan;
    //private ResourcePatternAccess advAcc;
    public DefaultScheduleViewerConfigurationProviderExtended schedProv = ScheduleViewerConfigProvEvalOff.getInstance();
    
	public OfflineEvaluationControlConfig appConfigData;
	public final OfflineEvalServiceAccessBase serviceAccess;
	public final GatewayDataExportI gatewayDataExport;
	
	public boolean showShortIntervalsInKPIs = false;
	protected final PageConfig multiKPIViewConfig;
	
	protected class MessageProviderData {
		public MessageProviderData(KPIPageGWOverviewMultiKPI page, KPIMessageDefinitionProvider messageProvider) {
			this.page = page;
			this.messageProvider = messageProvider;
		}
		KPIPageGWOverviewMultiKPI page;
		KPIMessageDefinitionProvider messageProvider;
	}
	protected final Map<String, MessageProviderData> messageProviders = new HashMap<>();
	
    public OfflineEvaluationControlController(ApplicationManager appMan, OfflineEvalServiceAccessBase evaluationOCApp, GatewayDataExportI gatewayDataExport,
    		PageConfig pageConfig) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		//this.advAcc = appMan.getResourcePatternAccess();
		this.serviceAccess = evaluationOCApp;
		this.gatewayDataExport = gatewayDataExport;
        this.multiKPIViewConfig = pageConfig;
		initConfigurationResource();
        initDemands();
	}

    /*
     * This app uses a central configuration resource, which is accessed here
     */
    private void initConfigurationResource() {
		String configResourceDefaultName = OfflineEvaluationControlConfig.class.getSimpleName().substring(0, 1).toLowerCase()+OfflineEvaluationControlConfig.class.getSimpleName().substring(1);
		appConfigData = appMan.getResourceAccess().getResource(configResourceDefaultName);
		if (appConfigData != null) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", getClass().getName());
		}
		else {
			appConfigData = (OfflineEvaluationControlConfig) appMan.getResourceManagement().createResource(configResourceDefaultName, OfflineEvaluationControlConfig.class);
			appConfigData.knownProviders().create();
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", getClass().getName());
		}
		if(!appConfigData.dataProvidersToUse().exists()) {
			appConfigData.dataProvidersToUse().create();
			appConfigData.dataProvidersToUse().setValues(new String[] {GaRoMultiEvalDataProviderJAXB_PROVIDER_ID});
			appConfigData.dataProvidersToUse().activate(false);
		}
		if(ValueResourceHelper.setIfNew(appConfigData.autoPreEval(), true)) appConfigData.autoPreEval().activate(false);
    }
    
    /*
     * register ResourcePatternDemands. The listeners will be informed about new and disappearing
     * patterns in the OGEMA resource tree
     */
    public void initDemands() {
    	appConfigData.dataProvidersToUse().addValueListener(new ResourceValueListener<StringArrayResource>() {

			@Override
			public void resourceChanged(StringArrayResource arg0) {
				serviceAccess.evalResultMan().getEvalScheduler().setStandardDataProvidersToUse(getDataProvidersToUse());
			}
		});
    	serviceAccess.evalResultMan().getEvalScheduler().setStandardDataProvidersToUse(getDataProvidersToUse());
    }

	public void close() {
        if(autoTimerMessaging != null) autoTimerMessaging.destroy();
        if(autoTimerSlotsBackup != null) autoTimerSlotsBackup.destroy();
    }

	/*
	 * if the app needs to consider dependencies between different pattern types,
	 * they can be processed here.
	 */
	public void processInterdependies() {
		// TODO Auto-generated method stub
		
	}
	
	public ProviderEvalOfflineConfig getCreateEvalPersistentData(String providerId) {
		for(ProviderEvalOfflineConfig known: appConfigData.knownProviders().getAllElements()) {
			if(known.providerId().getValue().equals(providerId)) return known;
		}
		ProviderEvalOfflineConfig result = appConfigData.knownProviders().addDecorator(
				ResourceUtils.getValidResourceName(providerId), ProviderEvalOfflineConfig.class);
		result.providerId().<StringResource>create().setValue(providerId);
		result.includeIntoStandardEval().<BooleanResource>create().setValue(false);
		result.activate(true);
		return result;
	}
	
	public GaRoSingleEvalProvider getEvalProvider(String id) {
		EvaluationProvider p = serviceAccess.getEvaluations().get(id);
		if(p == null) return null;
		if(p instanceof GaRoSingleEvalProvider) return (GaRoSingleEvalProvider) p;
		return null;
	}
	
	public GaRoMultiEvalDataProvider<?> getDataProvider(String id) {
		DataProvider<?> result = serviceAccess.getDataProviders().get(id);
		if(result instanceof GaRoMultiEvalDataProvider) return (GaRoMultiEvalDataProvider<?>) result;
		return null;
	}

	public List<GaRoMultiEvalDataProvider<?>> getDataProvidersToUse() {
		if(!appConfigData.dataProvidersToUse().isActive() || appConfigData.dataProvidersToUse().getValues().length == 0)
			return null;
		List<GaRoMultiEvalDataProvider<?>> result = new ArrayList<>();
		for(String s: appConfigData.dataProvidersToUse().getValues()) {
			GaRoMultiEvalDataProvider<?> dp = getDataProvider(s);
			if(dp != null) result.add(dp);
		}
		return result ;
	}
	
	public Set<String> getGatewayIds() {
		List<GaRoMultiEvalDataProvider<?>> provs = getDataProvidersToUse();
		Set<String> result = new HashSet<>();
		for(GaRoMultiEvalDataProvider<?> p: provs) {
			result.addAll(p.getGatewayIds());
		}
		return result ;
	}
	
	/** 0: No configuration set up
	 *  1: Configuration exists, but no auto-evaluation
	 *  2: Auto-evaluation active
	 * @return
	 */
	public int getAutoEvalStatus(String providerId) {
		EvalScheduler scheduler = serviceAccess.evalResultMan().getEvalScheduler();
		MultiKPIEvalConfiguration config = scheduler.getConfig(providerId, APP_EVAL_SUBID);
		if(config == null) return 0;
		if(config == null || (!config.isActive())) return 1;
		if((!config.performAutoQueuing().isActive()) ||
				(!config.performAutoQueuing().getValue())) return 1;
		if(!config.kpisToCalculate().isActive()) return 1;
		return 2;
	}
	public MultiKPIEvalConfiguration getAutoEvalConfig(String providerId) {
		EvalScheduler scheduler = serviceAccess.evalResultMan().getEvalScheduler();
		return scheduler.getConfig(providerId, APP_AUTO_SUBID);		
	}
	public MultiKPIEvalConfiguration getOrCreateEvalConfig(String evalProviderId,
			List<ResultType> resultsRequested,
			ChronoUnit chronoUnit, List<String> gwIDs, boolean forceValues,
			String subConfigId) {
		EvalScheduler scheduler = serviceAccess.evalResultMan().getEvalScheduler();
		Integer intervalType;
		if(chronoUnit == null)
			intervalType = null;
		else intervalType = KPIStatisticsUtil.getIntervalType(chronoUnit);
		int[] additionalIntervalTypes = MainPage.getKPIPageIntervals();
		MultiKPIEvalConfiguration startConfig = scheduler.getOrCreateConfig(evalProviderId, subConfigId,
				intervalType, additionalIntervalTypes, true);
		if((resultsRequested != null) && (forceValues || (!startConfig.resultsRequested().exists()))) {
			String[] results = new String[resultsRequested.size()];
			int i= 0;
			for(ResultType r: resultsRequested) {
				results[i] = r.id();
				i++;
			}
			if((!(results.length == 0)) && !startConfig.resultsRequested().exists()) {
				startConfig.resultsRequested().create();
			}
			startConfig.resultsRequested().setValues(results);
		}
		if((gwIDs != null) && (forceValues || (!startConfig.gwIds().exists()))) {
			if((!gwIDs.isEmpty()) && !startConfig.gwIds().exists()) {
				startConfig.gwIds().create();
			}
			startConfig.gwIds().setValues(gwIDs.toArray(new String[0]));
		}
		startConfig.activate(true);
		return startConfig;
	}
	
	/**
	 * @param eval
	 * @param resultsRequested
	 * @param chronoUnit if null standard interval will be used for creation
	 * @param gwIDs
	 * @return
	 */
	public MultiKPIEvalConfiguration getEvalConfig(GaRoSingleEvalProvider eval,
			List<ResultType> resultsRequested,
			ChronoUnit chronoUnit, List<String> gwIDs) {
		return getOrCreateEvalConfig(eval.id(),
				resultsRequested, chronoUnit, gwIDs, true, APP_EVAL_SUBID);
	}
	
	/** Standard mode for starting evaluations. Creates a suitable MultiKPIEvalConfiguration if no
	 * suitable exists and queues the respective evaluation.<br>
	 * Note that {@link #startEvaluationViaQueue(MultiKPIEvalConfiguration, long, long, OverwriteMode, String)}
	 * can be used to just start the evaluation for an existing MultiKPIEvalConfiguration*/
	public void startEvaluationViaQueue(GaRoSingleEvalProvider eval,
			List<ResultType> resultsRequested,
			ChronoUnit chronoUnit, long start, long end, List<String> gwIDs, OverwriteMode om,
			String resultFileName) {
		MultiKPIEvalConfiguration startConfig = getEvalConfig(eval,	resultsRequested, chronoUnit, gwIDs);
		//TODO: Make this configurable
		if(!startConfig.queueGatewaysIndividually().isActive()) {
			startConfig.queueGatewaysIndividually().<BooleanResource>create().setValue(true);
			startConfig.queueGatewaysIndividually().activate(false);
		}
		
		startEvaluationViaQueue(startConfig, start, end, om, resultFileName, null);
	}

	public void startEvaluationViaQueue(MultiKPIEvalConfiguration startConfig,
			long start, long end, OverwriteMode om,	String resultFileName, Set<String> providersDone) {
		serviceAccess.evalResultMan().getEvalScheduler().setDescriptorExportMode(true);
		serviceAccess.evalResultMan().getEvalScheduler().queueEvalConfig(startConfig , true, resultFileName, null, start, end,
				getDataProvidersToUse(), true, om, false, null, providersDone);
	}
	
	public void registerBackupListeners() {
		//Check for slotsDBBackup
		if(System.getProperty("org.ogema.eval.utilextended.slotsbackupretardmilli") != null) {
			if(autoTimerSlotsBackup == null) {
				log.info("Starting initial AutoQueueTimerSlotsBackup...");
				autoTimerSlotsBackup = new AutoQueueTimerSlotsBackup(appMan);
			}
		}		
	}
	
	public void registerExistingMultiPages( ) {
		for(KPIPageConfig item: appConfigData.kpiPageConfigs().getAllElements()) {
			if(item.sourceProviderId().isActive()) {
				GaRoSingleEvalProvider sourceEval = getEvalProvider(item.sourceProviderId().getValue());
				if(sourceEval != null) 
					if(addOrUpdateAndCreatePage(item, sourceEval))
						return;
			}
			if(item.pageId().isActive()) addMultiPage(item);
		}
	}
	
	public void createMultiPage(KPIPageConfig pageConfig, int intervalsIntoPast, String pageId, boolean autoAdd) {
		ValueResourceHelper.setCreate(pageConfig.defaultColumnsIntoPast(), intervalsIntoPast);
		ValueResourceHelper.setCreate(pageConfig.pageId(), pageId);
		if(autoAdd) addMultiPage(pageConfig);
	}
	
	public void addMultiPage(KPIPageConfig pageConfig) {
		addMultiPage(pageConfig, pageConfig.defaultColumnsIntoPast().getValue(),
				pageConfig.pageId().getValue());
	}
	private void addMultiPage(KPIPageConfig kpiPageConfig, int intervalsIntoPast, String pageId) {
		WidgetPage<?> pageNew;
		try {
			pageNew = serviceAccess.getWidgetApp().createWidgetPage(pageId+".html");
		} catch(IllegalArgumentException e) {
			//Page probably already exists, so we have to do nothing here
			return;
		}
		List<KPIColumn> colList = new ArrayList<>();
		List<ResultToShow> rlist = getResultsSorted(kpiPageConfig);
		for(ResultToShow p: rlist) {
			KPIColumn col = new KPIColumn();
			col.resultId = p.resultId().getValue();
			if(p.columnsIntoPast().isActive())
				col.pastColumnNum = p.columnsIntoPast().getValue();
			else
				col.pastColumnNum = intervalsIntoPast;
			colList.add(col);
		}
		KPIPageGWOverviewMultiKPI page = new KPIPageGWOverviewMultiKPI(pageNew, this, colList , kpiPageConfig, multiKPIViewConfig );
		serviceAccess.getMenu().addEntry("MultiKPI:"+pageId, pageNew);
		MenuConfiguration mc = pageNew.getMenuConfiguration();
		mc.setCustomNavigation(serviceAccess.getMenu());
		
		//Check for message providers
		if(kpiPageConfig.messageProviderId().isActive() && kpiPageConfig.sourceProviderId().isActive()) {
			GaRoSingleEvalProvider sourceEval = getEvalProvider(kpiPageConfig.sourceProviderId().getValue());
			if(sourceEval == null) return;
			KPIMessageDefinitionProvider mprov = sourceEval.getMessageProvider(kpiPageConfig.messageProviderId().getValue());
			if(mprov == null) return;
			messageProviders.put(kpiPageConfig.pageId().getValue(), new MessageProviderData(page, mprov));
			if(autoTimerMessaging == null) autoTimerMessaging = new AutoQueueTimerMessaging(appMan);
		}
	}

	public KPIPageConfig getOrCreateConfig(String name) {
		for(KPIPageConfig config: appConfigData.kpiPageConfigs().getAllElements()) {
			if(config.name().getValue().equals(name)) return config;
		}
		KPIPageConfig testConfig = appConfigData.kpiPageConfigs().add();
		testConfig.name().<StringResource>create().setValue(name);
		return testConfig;
	}
	
	/** Configure {@link KPIPageConfig} based on a list of EvaluationProviders
	 * and start all evaluations required for this.
	 * 
	 * @param providerId
	 * @param resultIds
	 * @param gwIds may be null especially if only configuration is done, no evaluation started
	 * @param configName
	 * @param configureSubGws currently only two options for the list of gateways included are supported.
	 * TODO: This has to be changed
	 * TODO: It seems that the evaluation is performed for each gateway and then for all gateways if true
	 * @param chronoUnit currently the evaluation interval is fixed to TEST_DEFAULT_INTERVALS_TO_CALC of
	 * 		chronoUnit
	 * TODO: This has to be changed
	 */
	public KPIPageConfig configureTestReport(List<String> providerId, List<String[]> resultIds,
			String[] gwIds,
			String configName, boolean configureSubGws, ChronoUnit chronoUnit,
			int TEST_DEFAULT_INTERVALS_TO_CALC,
			boolean configOnly) {
		EvalScheduler scheduler = serviceAccess.evalResultMan().getEvalScheduler();
		appConfigData.kpiPageConfigs().create();
		KPIPageConfig testConfig = getOrCreateConfig(configName);
		testConfig.resultsToShow().delete();
		testConfig.resultsToShow().create();

		List<MultiKPIEvalConfiguration> evalsToStart = new ArrayList<>();
		int idx = 0;
		for(int i=0; i<providerId.size(); i++) {
			//Note that intervalType etc. may not fit, but we do not care for sub-configs here
			MultiKPIEvalConfiguration comfort = scheduler.getConfig(providerId.get(i),
					OfflineEvaluationControlController.APP_EVAL_SUBID);
			if(comfort == null) {
				comfort = configureProvider(providerId.get(i),
						configureSubGws?resultIds.get(i):null,
						gwIds, chronoUnit);
				//comfort = configureTestReportForProvider(providerId.get(i),
				//		configName, resultIds.get(i), gwIds, configureSubGws, chronoUnit, testConfig);
			}
			evalsToStart.add(comfort);

			for(String res: resultIds.get(i)) {
				ResultToShow result1 = testConfig.resultsToShow().addDecorator(String.format("resultToShow_%03d", idx), ResultToShow.class); //.add();
				result1.resultId().<StringResource>create().setValue(res);
				result1.evalConfiguration().setAsReference(comfort);
				idx++;
			}
			if(configureSubGws) {
				ValueResourceHelper.setCreate(comfort.queueGatewaysIndividually(), true);
			}
		}

		appConfigData.kpiPageConfigs().activate(true);
		
		if(configOnly) {
			return testConfig;
		}		

		//We are starting the actual evaluation now
		long[] startEnd = scheduler.getStandardStartEndTime(evalsToStart.get(0), TEST_DEFAULT_INTERVALS_TO_CALC, true);
		if(startEnd == null) {
			System.out.println("  !!!!!   No Evaluation Start necessary (Should never occur)!");
			return testConfig;
		}
		Set<String> providersDone = new HashSet<>();
		//Map<String, Set<String>> gwProvidersDone = null;
		//if(configureSubGws) gwProvidersDone = new HashMap<>();
		for(MultiKPIEvalConfiguration config: evalsToStart) {
			System.out.println("     :::: Queuing "+config.evaluationProviderId().getValue()+ " from "+
			TimeUtils.getDateAndTimeString(startEnd[0])+" ("+startEnd[0]+")");
			
			startEvaluationViaQueue(config, startEnd[0], startEnd[1], OverwriteMode.ONLY_PROVIDER_REQUESTED, null, providersDone);
		}
		return testConfig;
	}
	
	/*public MultiKPIEvalConfiguration configureTestReportForProvider(String providerId, String configName,
			String[] resultIds, String[] gwIds, boolean configureSubGws, ChronoUnit chronoUnit, KPIPageConfig testConfig) {
		MultiKPIEvalConfiguration comfort = configureProvider(providerId,
				configureSubGws?resultIds:null,
				gwIds, chronoUnit);
//		if(configureSubGws) {
//			ValueResourceHelper.setCreate(comfort.queueGatewaysIndividually(), true);
//		}
//		
//		for(String res: resultIds) {
//			ResultToShow result1 = testConfig.resultsToShow().add();
//			result1.resultId().<StringResource>create().setValue(res);
//			result1.evalConfiguration().setAsReference(comfort);
//		}
		
		return comfort;
	}*/
	
	/** Configure {@link MultiKPIEvalConfiguration} for a provider with subConfigId "OfflineEvaluationControl"
	 * 
	 * @param providerId
	 * @param resultsRequested if null the result types are not set, which should lead to a calculation
	 * 		of all result types
	 * @return
	 */
	public MultiKPIEvalConfiguration configureProvider(String providerId,
			String[] resultsRequested, String[] gwIds, ChronoUnit chronoUnit) {
		EvalScheduler scheduler = serviceAccess.evalResultMan().getEvalScheduler();
		int intervalType = KPIStatisticsUtil.getIntervalType(chronoUnit);
		int[] additionalIntervalTypes = MainPage.getKPIPageIntervals();
		MultiKPIEvalConfiguration startConfig = scheduler.getOrCreateConfig(providerId, OfflineEvaluationControlController.APP_EVAL_SUBID,
				intervalType, additionalIntervalTypes, true);
		if(resultsRequested != null) {
			String[] results = new String[resultsRequested.length];
			int i= 0;
			for(String r: resultsRequested) {
				results[i] = r;
				i++;
			}
			if((!(results.length == 0)) && !startConfig.resultsRequested().exists()) {
				startConfig.resultsRequested().create();
			}
			startConfig.resultsRequested().setValues(results);
		}
		if((gwIds != null) && (gwIds.length > 0)) {
			startConfig.gwIds().create();
			startConfig.gwIds().setValues(gwIds);
			startConfig.activate(true);
		}
		return startConfig;
	}
	
	private long getNextAutoQueueTimeMessaging(ApplicationManager appMan) {
		Long autoQueueStep = Long.getLong("org.ogema.util.evalcontrol.development.autoQueueRepeatRate");
		if(autoQueueStep != null) {
			log.info("Scheduling next message for "+TimeUtils.getDateAndTimeString(appMan.getFrameworkTime()+autoQueueStep)+" based on autoQueueStep");
			return appMan.getFrameworkTime()+autoQueueStep;
		}
		long nextTime = AbsoluteTimeHelper.getNextStepTime(appMan.getFrameworkTime(), AbsoluteTiming.DAY);
		long retard = Long.getLong("org.ogema.eval.utilextended.autoevalretardmilli", 6*StandardConfigurations.HOUR_MILLIS)
				+30*StandardConfigurations.MINUTE_MILLIS;
		nextTime += retard;
		log.info("Scheduling next message for "+TimeUtils.getDateAndTimeString(nextTime)+" based on AbsoluteTiming");
		return nextTime;		
	}
    AutoQueueTimerMessaging autoTimerMessaging;
	class AutoQueueTimerMessaging extends CountDownAbsoluteTimer {
		
		public AutoQueueTimerMessaging(ApplicationManager appMan) {
			super(appMan, getNextAutoQueueTimeMessaging(appMan), new TimerListener() {
				@Override
				public void timerElapsed(Timer timer) {
					for(MessageProviderData e: messageProviders.values()) {
						sendMessage(e);
					}
					
					autoTimerMessaging = new AutoQueueTimerMessaging(appMan);
				}
			});
		}
	}
	
	public void sendMessage(String pageId) {
		sendMessage(messageProviders.get(pageId));
	}
	protected void sendMessage(MessageProviderData e) {
		Collection<?> lines = e.page.getObjectsInTable(null);
		@SuppressWarnings("unchecked")
		MessageDefinition mes = e.messageProvider.getMessage((Collection<KPIStatisticsManagementI>) lines,
				appMan.getFrameworkTime());
		serviceAccess.messageService().sendMessage(appMan.getAppID(),
				new MessageImpl(mes.getTitle(), mes.getMessage(), mes.getPriority()));
		System.out.println("         SENT MESSAGE "+mes.getTitle()+":\r\n"+mes.getMessage());
	}
	
	public void addOrUpdatePageConfigFromProvider(KPIPageDefinition def,
			GaRoSingleEvalProvider eval) {
		KPIPageConfig pageConfig = configureTestReport(def.providerId,
				def.resultIds, null,
				def.configName,
				true, def.chronoUnit, def.defaultIntervalsToCalc, true);
		pageConfig.sourceProviderId().<StringResource>create().setValue(eval.id());
		pageConfig.sourceProviderId().activate(false);
		if(def.hideOverallLine != null) {
			pageConfig.hideOverallLine().<BooleanResource>create().setValue(def.hideOverallLine);
			pageConfig.hideOverallLine().activate(false);
		}
		if(def.messageProvider != null) {
			pageConfig.messageProviderId().<StringResource>create().setValue(def.messageProvider);
			pageConfig.messageProviderId().activate(false);
		}
		createMultiPage(pageConfig, def.defaultIntervalsPerColumnType,
				def.urlAlias, false);
		if(def.specialIntervalsPerColumn != null && (!def.specialIntervalsPerColumn.isEmpty()))
		for(ResultToShow rts: pageConfig.resultsToShow().getAllElements()) {
			Integer specialNum = def.specialIntervalsPerColumn.get(rts.resultId().getValue());
			if(specialNum != null) {
				ValueResourceHelper.setCreate(rts.columnsIntoPast(), specialNum);
			}
		}
		addMultiPage(pageConfig);
		
	}
	
	/** Update if configuration has to be changed*/
	private static Set<String> done = new HashSet<>();
	public boolean addOrUpdateAndCreatePage(KPIPageConfig testConfig, GaRoSingleEvalProvider eval) {
	synchronized(done) {
		String pageId = testConfig.pageId().getValue();
		if(!done.add(pageId)) {
			return false;
		}
		List<KPIPageDefinition> pdef = eval.getPageDefinitionsOffered();
		KPIPageDefinition def = null;
		for(KPIPageDefinition def2: pdef) {
			if(def2.urlAlias.equals(pageId)) {
				def = def2;
				break;
			}
		}
		if(def == null) {
			log.warn("pageId "+pageId+" not found in source-provider "+eval.id()+" anymore!");
			return false;
		}
		boolean doUpdate = false;
		if(testConfig != null) {
			List<ResultToShow> existing = getResultsSorted(testConfig);
			int idx = 0;
			//if(existing.size() != def.resultIds.size()) doUpdate = true;
			for(String[] rr: def.resultIds) {
				for(String r: rr) {
					//TODO: Check also if existing.get(idx).columnsIntoPast() has to be changed 
					if(existing.size() <= idx ) {
						doUpdate = true;
						break;
					}
					if(!existing.get(idx).resultId().getValue().equals(r)) {
						doUpdate = true;
						break;
					}
					idx++;
				}
				if(doUpdate) break;
			}
			if((!doUpdate) && (existing.size() > idx))
				doUpdate = true;
		}
		if(doUpdate) {
			addOrUpdatePageConfigFromProvider(def, eval);
			return true;
		}
		//if(testConfig != null)
		//	addMultiPage(testConfig);
		return false;
	}
	}
	
	protected List<ResultToShow> getResultsSorted(KPIPageConfig testConfig) {
		List<ResultToShow> rlist = testConfig.resultsToShow().getAllElements();
		//workaround as reading from backup file does not maintain order in ResourceList
		if(Boolean.getBoolean("org.ogema.evaluationofflinecontrol.devmode")) {
			Comparator<ResultToShow> comp = new Comparator<ResultToShow>() {

				@Override
				public int compare(ResultToShow o1, ResultToShow o2) {
					return o1.getName().compareTo(o2.getName());
				}
			};
			rlist.sort(comp);
		}
		return rlist;
	}
	
	public boolean showBackupButton() {
		return appConfigData.showBackupButton().getValue();
	}
	
	private long getNextAutoQueueTimeSlotsBackup(ApplicationManager appMan) {
		Long autoQueueStep = Long.getLong("org.ogema.util.evalcontrol.development.slotsbackupStartupRetard");
		if(autoQueueStep != null) {
			log.info("Scheduling next message for "+TimeUtils.getDateAndTimeString(appMan.getFrameworkTime()+autoQueueStep)+" based on autoQueueStep");
			return appMan.getFrameworkTime()+autoQueueStep;
		}
		//SlotsDB is aligned according to UTC
		long nextTime = AbsoluteTimeHelper.getNextStepTime(appMan.getFrameworkTime(), DateTimeZone.UTC.getID(), AbsoluteTiming.DAY);
		long retard = Long.getLong("org.ogema.eval.utilextended.slotsbackupretardmilli", 0*StandardConfigurations.HOUR_MILLIS)
				+30*StandardConfigurations.MINUTE_MILLIS;
		nextTime += retard;
		log.info("Scheduling next SlotsDB-Backup for "+TimeUtils.getDateAndTimeString(nextTime)+" based on AbsoluteTiming");
		return nextTime;		
	}
    AutoQueueTimerSlotsBackup autoTimerSlotsBackup;
	class AutoQueueTimerSlotsBackup extends CountDownAbsoluteTimer {
		
		public AutoQueueTimerSlotsBackup(ApplicationManager appMan) {
			super(appMan, getNextAutoQueueTimeSlotsBackup(appMan), new TimerListener() {
				@Override
				public void timerElapsed(Timer timer) {
					performSlotsBackup();
					autoTimerSlotsBackup = new AutoQueueTimerSlotsBackup(appMan);
				}
			});
		}
	}
	
	public static final String generalBackupSource = "./data/semabox_01/extBackup";
	private void performSlotsBackup() {
		Resource resConfig = appMan.getResourceAccess().getResource("resAdminConfig");
		if(resConfig == null) return;
		ResourceList<?> configList = resConfig.getSubResource("configList", ResourceList.class);
		if(!configList.exists()) return;
		for(Resource el: configList.getAllElements()) {
			log.debug("Checking Action :"+el.getLocation());
			StringResource destinationDirectory  = el.getSubResource("destinationDirectory", StringResource.class);
			if(destinationDirectory.isActive() && destinationDirectory.getValue().startsWith(generalBackupSource)) {
				log.debug("Performing Action :"+el.getLocation());
				Action action = el.getSubResource("run", Action.class);
				if(action != null && action.isActive())
					performActionBlocking(action, 20000);
				break;
			}
		}
		long now = appMan.getFrameworkTime();
		//slotsDB is aligned according to UTC
		long endTime = AbsoluteTimeHelper.getIntervalStart(now, DateTimeZone.UTC.getID(), AbsoluteTiming.DAY);
		
		long startTime;
		long startTimeDefault = AbsoluteTimeHelper.addIntervalsFromAlignedTime(endTime, -1, DateTimeZone.UTC.getID(), AbsoluteTiming.DAY);
		startTimeDefault -= 12*StandardConfigurations.HOUR_MILLIS;
		if(appConfigData.lastAutoZipBackup().isActive()) {
			startTime = startTimeDefault;
		} else {
			startTime = appConfigData.lastAutoZipBackup().getValue();
			if(startTime > startTimeDefault)
				startTime = startTimeDefault;
		}
		//we set the window as noon to noon to have exactly one folder time (=day start) in the interval
		endTime -= 12*StandardConfigurations.HOUR_MILLIS;
		RemoteSlotsDBBackupButton.performSlotsBackup(Paths.get("./data/"), Paths.get("./data/backupzip/remoteSlots"+StringFormatHelper.getDateForPath(now)+".zip"),
				startTime, endTime, Arrays.asList(new String[] {""}), new File(generalBackupSource), true);
		if(!appConfigData.lastAutoZipBackup().isActive()) {
			appConfigData.lastAutoZipBackup().create();
			appConfigData.lastAutoZipBackup().setValue(endTime);
			appConfigData.lastAutoZipBackup().activate(false);
		} else
			appConfigData.lastAutoZipBackup().setValue(endTime);
	}

	@Deprecated //Use version from ActionHelper as soon as it is released
	public static void performActionBlocking(Action ac, long maxDuration) {
		ac.stateControl().setValue(true);
		long maxEnd = System.currentTimeMillis()+maxDuration;
		while((ac.stateControl().getValue())&&(System.currentTimeMillis() < maxEnd)) {
			try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
		}
	}

}
