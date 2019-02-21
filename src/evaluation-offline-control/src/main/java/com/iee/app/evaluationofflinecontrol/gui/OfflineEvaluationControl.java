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
package com.iee.app.evaluationofflinecontrol.gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.ResourceNotFoundException;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.externalviewer.extensions.SmartEffEditOpenButton;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.util.directresourcegui.kpi.KPIStatisticsUtil;
import org.ogema.util.evalcontrol.EvalScheduler.OverwriteMode;
import org.ogema.util.jsonresult.management.JsonOGEMAFileManagementImpl;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;

import com.iee.app.evaluationofflinecontrol.OfflineEvalServiceAccess;
import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;
import com.iee.app.evaluationofflinecontrol.gui.element.RemoteSlotsDBBackupButton;
import com.iee.app.evaluationofflinecontrol.util.ScheduleViewerConfigProvEvalOff;
import com.iee.app.evaluationofflinecontrol.util.SmartEffPageConfigProvEvalOff;
import com.iee.app.evaluationofflinecontrol.util.StandardConfigurations;

import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.configuration.Configuration;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance.GenericObjectConfiguration;
import de.iwes.timeseries.eval.api.semaextension.variant.GaRoEvalHelperJAXB_V2;
import de.iwes.timeseries.eval.api.semaextension.variant.GatewayDataExportUtil;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoStdPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSuperEvalResult;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.CSVArchiveExporter;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider.KPIPageDefinition;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GaRoTestStarter;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;
import de.iwes.widgets.multiselect.extended.MultiSelectExtended;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;
import de.iwes.widgets.template.DefaultDisplayTemplate;


public class OfflineEvaluationControl {
	public static final String defaultText = ButtonData.defaultText;
	public static final String status0 = "Create Auto-Eval Config";					
	public static final String status1 = "Edit Auto-Eval Config (Off)";
	public static final String status2 = "Edit Auto-Eval Config (On)";					
	public static final String statusEval = "Save and Close Auto-Eval Config";

    private final EvalResultManagement evalResultMan;
    
	private final OfflineEvaluationControlController controller;
	private final TemplateMultiselect<String> multiSelectGWs;
	private List<ResultType> resultMultiSelection = new ArrayList<ResultType>();
	private final TemplateMultiselect<ResultType> resultsSelection;
	private final PreEvalSelectDropdown selectPreEval1;
	private final PreEvalSelectDropdown selectPreEval2;
	private final TemplateDropdown<OverwriteMode> overWriteDrop;
	private final WidgetPage<?> page;
	private final TemplateDropdown<GaRoSingleEvalProvider> selectProvider;
	private final Button startOfflineEval;
	private final Button openScheduleViewer;
	private final Button addKPIPageButton;
	private final TextField evalName;
	private final Button buttonAutoEvalConfig;
	private final MultiSelectExtended<String> gateWaySelection;
	private final TemplateDropdown<String> selectConfig;
	private final TemplateDropdown<String> selectSingleValueIntervals;
	
	public final long UPDATE_RATE = 5*1000;
	public EvaluationProvider selectEval;

	private GaRoTestStarter<GaRoMultiResult> lastEvalStarted = null;
	private final Button stopLastEvalButton;
	
	public OfflineEvaluationControl(final WidgetPage<?> page, final OfflineEvaluationControlController app) {
		
		this.page = page;
		this.controller = app; 
		this.evalResultMan = controller.serviceAccess.evalResultMan(); //EvalResultManagementStd.getInstance(app.appMan);
		addHeader();
		
		//List<String> jsonList1 = new ArrayList<>();
		//List<String> jsonList2 = new ArrayList<>();
		List<String> configOptions = StandardConfigurations.getConfigOptions();
		List<String> singleValueOption = new ArrayList<>();
		
		singleValueOption.add("Days Value");
		singleValueOption.add("Weeks Value");
		singleValueOption.add("Months Value");
		//options that do not contribute to longer KPIs, just for manual check
		singleValueOption.add("Hours Value");
		singleValueOption.add("Minutes Value");
		singleValueOption.add("Current Hour Single From-To Minutes");

		//init GaRo
		final TemplateInitSingleEmpty<GaRoSingleEvalProvider> init = new TemplateInitSingleEmpty<GaRoSingleEvalProvider>(page, "init", false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected GaRoSingleEvalProvider getItemById(String configId) {
				for(GaRoSingleEvalProvider p: getProviders()) {
					if(p.id().equals(configId)) return p;
				}
				if(getProviders().isEmpty())
					return null;
				else return getProviders().get(0);
			}

			@Override
			public void init(OgemaHttpRequest req) {
				super.init(req);
				//switchToEvalConfig(req);
				//List<String> toUse = GatewayConfigPage.getGwsToUse(app);
				//multiSelectGWs.selectItems(toUse, req);
				
				selectProvider.update(getProviders(), req);
				selectProvider.selectItem(getSelectedItem(req), req);
				
				initResultsGws(req, false);
			}
		};
		
		page.append(init);
		
		//number of available json files
		final Label selectPreEval1Count = new Label(page, "selectPreEvalCount1");
		final Label selectPreEval2Count = new Label(page, "selectPreEvalCount2");

		//provider drop-down
		selectProvider 
			= new TemplateDropdown<GaRoSingleEvalProvider>(page, "selectProvider") {

				private static final long serialVersionUID = 1L;

			@Override
			public void updateDependentWidgets(OgemaHttpRequest req) {
				//EvaluationProvider eval =  getSelectedItem(req);
				//When the provider changes we have to adapt the results, but we do not change the gateway selection
				initResultsGws(req, true);
				//if(eval != null ) {
				//	resultMultiSelection = eval.resultTypes();
				//	resultsSelection.update(resultMultiSelection, req);
				//	resultsSelection.selectItems(resultMultiSelection, req);
				//}
				boolean autoPreEval = controller.appConfigData.autoPreEval().getValue();
				selectPreEval1.onGETRemote(autoPreEval, req);
				selectPreEval2.onGETRemote(autoPreEval, req);
				evalName.setValue(getSelectedLabel(req)+"Result.json", req);
			}
			
		};
		
		selectProvider.setTemplate(new DefaultDisplayTemplate<GaRoSingleEvalProvider>() {
			@Override
			public String getLabel(GaRoSingleEvalProvider object, OgemaLocale locale) {
				return object.getClass().getSimpleName();
			}
		});

		//TextField toMinutesBeforeNow = new TextField(page, "toMinutesBeforeNow");
		//toMinutesBeforeNow.setDefaultValue("0");
		//TextField fromMinutesBeforeNow = new TextField(page, "fromMinutesBeforeNow");
		//fromMinutesBeforeNow.setDefaultValue("10");
		//configuration drop-down
		selectConfig = 
				new TemplateDropdown<String>(page, "selectConfig") {

			private static final long serialVersionUID = 1L;
			
			public void onGET(OgemaHttpRequest req) {
				//boolean showSingleHourOptions = false;
				if(getStatus(req) >= 0) {
					enable(req);
					//String sel = getSelectedItem(req);
					//if(sel != null && sel.equals("Current Hour Single From-To Minutes"))
					//	showSingleHourOptions = true;
				} else disable(req);
				/*if(showSingleHourOptions) {
					toMinutesBeforeNow.setWidgetVisibility(true, req);
					fromMinutesBeforeNow.setWidgetVisibility(true, req);
				} else {
					toMinutesBeforeNow.setWidgetVisibility(false, req);
					fromMinutesBeforeNow.setWidgetVisibility(false, req);
				}*/
			}
			
		};
		selectConfig.setDefaultItems(configOptions);
		
		//result multi-selection
		resultsSelection = new TemplateMultiselect<ResultType>(page, "resultSelections") {

			private static final long serialVersionUID = 1L;
			
			public void onGET(OgemaHttpRequest req) {
				/*GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
				if(eval != null ) {
					resultMultiSelection = eval.resultTypes();
					update(resultMultiSelection, req);
				/*if(getStatus(req) >= 0) {
						resultMultiSelection = eval.resultTypes();
						update(resultMultiSelection, req);
						selectItems(resultMultiSelection, req);
					} else {
						MultiKPIEvalConfiguration mulcon = getOrCreateAutoConfig(req);
						String[] multiSelectedResults = mulcon.resultsRequested().getValues();
						List<ResultType> resultsRequested = getResultsSelected(Arrays.asList(multiSelectedResults), eval);
						selectItems(resultsRequested, req);
					}	
				}*/
			};
			
		};
		resultsSelection.setDefaultWidth("100%");
		resultsSelection.selectDefaultItems(resultMultiSelection);

		resultsSelection.setTemplate(new DefaultDisplayTemplate<ResultType>() {
			@Override
			public String getLabel(ResultType object, OgemaLocale locale) {
				return object.label(OgemaLocale.ENGLISH);
			}
		});
		
		final MultiSelectExtended<ResultType> resultSelectionExtended = 
				new MultiSelectExtended<ResultType>(page, "resultSelectionExtended", resultsSelection, true, "", true, false);

		//gateway multi-selection
		multiSelectGWs = new TemplateMultiselect<String>(page, "gateWaySelectionMS") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				//update(controller.serviceAccess.gatewayParser.getGatewayIds(), req);
				/*GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
				if(eval != null ) {
					if(getStatus(req) >= 0) {
						Set<String> ids = controller.getGatewayIds();
						update(ids, req);
						selectItems(ids, req);
					} else {
						MultiKPIEvalConfiguration mulcon = getOrCreateAutoConfig(req);
						String[] multiGws = mulcon.gwIds().getValues();
						selectItems(Arrays.asList(multiGws), req);
					}	
				}*/
			}
		};
		multiSelectGWs.setDefaultWidth("100%");
		multiSelectGWs.selectDefaultItems(controller.getGatewayIds());
		//multiSelectGWs.selectDefaultItems(controller.serviceAccess.gatewayParser.getGatewayIds());
		
		gateWaySelection = 
				new MultiSelectExtended<String>(page, "gateWaySelection", multiSelectGWs, true, "", true, false);

		//single value intervals drop-down
		selectSingleValueIntervals = 
				new TemplateDropdown<String>(page, "singleValueIntervals");
		selectSingleValueIntervals.setDefaultItems(singleValueOption);
		
		//set a new json file name
		evalName = new TextField(page, "evalName") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(getStatus(req) >= 0) {
					enable(req);
				} else disable(req);
			}
		};

		//first preEvaluation drop-down
		selectPreEval1 = new PreEvalSelectDropdown(page, "selectPreEval1",
				selectProvider, selectPreEval1Count, 0);
		
		//second preEvaluation drop-down
		selectPreEval2 = new PreEvalSelectDropdown(page, "selectPreEval2",
				selectProvider, selectPreEval2Count, 1);
		
		overWriteDrop = new TemplateDropdown<OverwriteMode>(page, "overwriteDrop") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(controller.appConfigData.autoPreEval().getValue()) {
					setWidgetVisibility(true, req);
					selectPreEval1Count.setText("Shall existing results for the requested time span be re-calculated and overwritten?", req);
					if(getStatus(req) >= 0) {
						enable(req);
					} else disable(req);
				} else
					setWidgetVisibility(false, req);
			}
	};
		overWriteDrop.setDefaultItems(Arrays.asList(new OverwriteMode[] {OverwriteMode.NO_OVERWRITE,
				OverwriteMode.ONLY_PROVIDER_REQUESTED, OverwriteMode.ALL_INCLUDING_PRE_EVALUATIONS
		}));
		overWriteDrop.selectDefaultItem(OverwriteMode.ONLY_PROVIDER_REQUESTED);
		
		//start offline evaluation for selected provider
		startOfflineEval = new Button(page, "startOfflineEval", "Start Offline Evaluation") {

			private static final long serialVersionUID = 1L;
			
			public void onGET(OgemaHttpRequest req) {
				if(getStatus(req) >= 0) {
					if(resultsSelection.getSelectedItems(req).isEmpty() 
							|| multiSelectGWs.getSelectedItems(req).isEmpty() 
							|| (selectPreEval1.getSelectedItem(req) == null && selectPreEval1.isVisible(req))) {
						disable(req);
					} else enable(req);
					setText("Start Offline Evaluation", req);
				} else {
					enable(req);
					setText("Schedule Auto-Eval Run", req);
				}
			};
			
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				
				GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
				if(getStatus(req) < 0) {
					List<MultiKPIEvalConfiguration> configs =
							app.serviceAccess.evalResultMan().getEvalScheduler().getConfigs(eval.id());
					for(MultiKPIEvalConfiguration config: configs) {
						//from EvalSchedulerImpl#startAutoScheduling
						if(config == null || (!config.isActive())) continue;
						if((!config.performAutoQueuing().isActive()) ||
								(!config.performAutoQueuing().getValue())) continue;
						if(!config.kpisToCalculate().isActive()) continue;
						app.serviceAccess.evalResultMan().getEvalScheduler().queueAutoEvalConfig(config);
						break;
					}
					return;
				}
				//String selectedEvalProvider = selectProvider.getSelectedLabel(req);
				String config = selectConfig.getSelectedLabel(req);
				//String singleValue = selectSingleValueIntervals.getSelectedLabel(req);
				String singleValue = selectSingleValueIntervals.getSelectedLabel(req);
				boolean singleHour = false;
				if(singleValue.equals("Current Hour Single From-To Minutes"))
					singleHour = true;
				ChronoUnit chronoUnit = getChronoUnit(req);	
			    
				List<String> multiSelectedResults = (List<String>) resultsSelection.getSelectedLabels(req);
				List<ResultType> resultsRequested = getResultsSelected(multiSelectedResults, eval);
				List<String> gwIDs = (List<String>) gateWaySelection.multiSelect.getSelectedLabels(req);
				
				IntervalConfiguration itv;
				if(singleHour) {
					itv = new IntervalConfiguration();
					long now = app.appMan.getFrameworkTime();
					int interval = AbsoluteTiming.MINUTE;
					int toMinutesBeforeNow = 1;
					int fromMinutesBeforeNow = 10;
					
					itv.end = AbsoluteTimeHelper.getIntervalStart(now, interval)-toMinutesBeforeNow;
					itv.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(itv.end+1, -fromMinutesBeforeNow, interval);
					app.showShortIntervalsInKPIs = true;					
				} else if(chronoUnit == ChronoUnit.MINUTES || chronoUnit == ChronoUnit.HOURS) {
					itv = new IntervalConfiguration();
					long now = app.appMan.getFrameworkTime();
					int interval = KPIStatisticsUtil.getIntervalType(chronoUnit);
					IntervalConfiguration itvSelected = StandardConfigurations.getConfigDuration(config, app.appMan);
					int num = (int) ((itvSelected.end - itvSelected.start + 60000) / AbsoluteTimeHelper.getStandardInterval(AbsoluteTiming.DAY));
					
					itv.end = AbsoluteTimeHelper.getIntervalStart(now, interval)-1;
					itv.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(itv.end+1, -num, interval);
					app.showShortIntervalsInKPIs = true;
				} else
					itv = StandardConfigurations.getConfigDuration(config, app.appMan);
				
				OverwriteMode om = overWriteDrop.getSelectedItem(req);
				File pre = selectPreEval1.getSelectedItem(req);
				Path preEvalFile = null;
				if(pre != null)
					preEvalFile = selectPreEval1.getSelectedItem(req).toPath(); //Paths.get(FILE_PATH+"/"+selectPreEval1.getSelectedLabel(req));
				String jsonFileName = evalName.getValue(req);
				
				//if(getStatus(req) >= 0)
					startEvalutionLikeStartPage(jsonFileName, eval, itv, chronoUnit,
						resultsRequested, gwIDs, om, preEvalFile);
				/*else {
					MultiKPIEvalConfiguration configLoc = app.getOrCreateEvalConfig(eval.id(), resultsRequested, chronoUnit, gwIDs, true,
							OfflineEvaluationControlController.APP_AUTO_SUBID);
					//Same call as in EvalSchedulerImpl.queueEvalConfig(MultiKPIEvalConfiguration)
					app.serviceAccess.evalResultMan().getEvalScheduler().queueAutoEvalConfig(configLoc);
				}*/
			}
		};
		
		startOfflineEval.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);

		openScheduleViewer = getScheduleViewerOpenButton(page, "openScheduleViewer", multiSelectGWs, selectProvider,
				app, selectConfig,
			new ScheduleViewerOpenButtonDataProvider() {
				
				@Override
				public IntervalConfiguration getITVConfiguration(String config, ApplicationManager appMan) {
					return StandardConfigurations.getConfigDuration(config, app.appMan);
				}
				
				@Override
				public List<TimeSeriesData> getData(OgemaHttpRequest req) {
					final GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
					final List<String> gwIDs = (List<String>) gateWaySelection.multiSelect.getSelectedLabels(req);
					
					List<GaRoMultiEvalDataProvider<?>> dps = controller.getDataProvidersToUse();
					GaRoMultiEvalDataProvider<?> dp = dps.get(0);
					List<TimeSeriesData> input = GaRoEvalHelper.getFittingTSforEval(dp, eval, gwIDs);
					return input;
				}
			}
		);
		
		SmartEffEditOpenButton openConfigResourceEdit = new SmartEffEditOpenButton(page,
				"openConfigResourceEdit", "Configuration Resource",
				"admin/org_sp_example_smarteff_eval_capability_BuildingEvalParamsPage.html",
				SmartEffPageConfigProvEvalOff.PROVIDER_ID,
				new SmartEffPageConfigProvEvalOff()) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				final GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
				List<Configuration<?>> configs = eval.getConfigurations();
				if(configs.isEmpty()) {
					setWidgetVisibility(false, req);
					return;
				}
				Configuration<?> config = configs.get(0);
				Class<?> type = config.configurationType();
				ConfigurationInstance ci = config.defaultValues();
				if(!(ci instanceof GenericObjectConfiguration))
					throw new IllegalStateException("ConfigurationInstance does not match type!");
				Object vals = ((GenericObjectConfiguration<?>)ci).getValue();
				if(type.equals(GenericObjectConfiguration.class) &&
						(vals instanceof Class)) {
					setWidgetVisibility(true, req);
				} else setWidgetVisibility(false, req);
			}
			
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				final GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
				Configuration<?> configLoc2= eval.getConfigurations().get(0);
				ConfigurationInstance ci = configLoc2.defaultValues();
				if(ci instanceof GenericObjectConfiguration) {
					GenericObjectConfiguration<?> goc = (GenericObjectConfiguration<?>)ci;
					Object typeIn = goc.getValue();
					if(!(typeIn instanceof Class)) {
						throw new IllegalStateException("Expecting resource type!");
					}
					//@SuppressWarnings("unchecked")
					//Class<? extends Resource> type = (Class<? extends Resource>)typeIn;
					
					ChronoUnit chronoUnit = getChronoUnit(req);	
					List<String> multiSelectedResults = (List<String>) resultsSelection.getSelectedLabels(req);
					List<ResultType> resultsRequested = getResultsSelected(multiSelectedResults, eval);
					List<String> gwIDs = (List<String>) gateWaySelection.multiSelect.getSelectedLabels(req);
					MultiKPIEvalConfiguration evalConfig = controller.getEvalConfig(eval, resultsRequested, chronoUnit, gwIDs);
					Resource configRes = evalResultMan.getEvalScheduler().getConfigurationResource(evalConfig, eval);
					if(!configRes.exists()) {
						configRes.create();
						List<MultiKPIEvalConfiguration> anyForProvider = evalResultMan.getEvalScheduler().getConfigs(eval.id());
						for(MultiKPIEvalConfiguration any: anyForProvider) {
							try {
								Resource anyRes = evalResultMan.getEvalScheduler().getConfigurationResource(any, eval);
								//any.getSubResource("configurationResource", type);
								if(anyRes.exists() && (!anyRes.equalsLocation(configRes)))
									System.out.println("Warning: Configuration "+anyRes.getLocation()+" already exists!");
								else
									anyRes.setAsReference(configRes);
							} catch(ResourceNotFoundException e) {
								System.out.println("Warning: Configuration resource of wrong type at "+any.getLocation());
							}
						}
					}
					String ci2 = addConfig(evalConfig.configurationResource(), "Configuration for EvaluationProvider "+eval.label(req.getLocale()));
					setConfigId(ci2, req);
					//} catch(ResourceNotFoundException e) {
					//	throw new IllegalStateException("Existing config type:"+evalConfig.configurationResource().getResourceType().getName()+", expected:"+type.getName(), e);
					//}
				}
			}
		};
		/*openScheduleViewer = new ScheduleViewerOpenButton(page, "openScheduleViewer", "Schedule Viewer",
				ScheduleViewerConfigProvEvalOff.PROVIDER_ID,
				ScheduleViewerConfigProvEvalOff.getInstance()) {

			private static final long serialVersionUID = 1L;
			
			public void onGET(OgemaHttpRequest req) {
				if(multiSelectGWs.getSelectedItems(req).isEmpty()) {
					disable(req);
				} else enable(req);
			};
			
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				final GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
				final List<String> gwIDs = (List<String>) gateWaySelection.multiSelect.getSelectedLabels(req);
				
				List<GaRoMultiEvalDataProvider<?>> dps = controller.getDataProvidersToUse();
				GaRoMultiEvalDataProvider<?> dp = dps.get(0);
				List<TimeSeriesData> input = GaRoEvalHelper.getFittingTSforEval(dp, eval, gwIDs);
				ReadOnlyTimeSeries timeSeries;
				List<ReadOnlyTimeSeries> result = new ArrayList<>();
				Map<ReadOnlyTimeSeries, String> shortNames = new HashMap<ReadOnlyTimeSeries, String>();
				Map<ReadOnlyTimeSeries, String> longNames = new HashMap<ReadOnlyTimeSeries, String>();
				//int idx = -1;
				for (TimeSeriesData tsdBase : input) {
					//idx++;
					if(!(tsdBase instanceof TimeSeriesDataOffline)) throw new IllegalStateException("getStartAndEndTime only works on TimeSeriesData input!");
					TimeSeriesDataOffline tsd = (TimeSeriesDataOffline) tsdBase;
					timeSeries = tsd.getTimeSeries();
					if(tsd instanceof TimeSeriesDataExtendedImpl) {
						TimeSeriesDataExtendedImpl tse = (TimeSeriesDataExtendedImpl)tsd;
						if(tse.type instanceof GaRoDataTypeI) {
							if(tse.getIds().size() > 1) {
								String gwId = tse.getIds().get(0);
								String prop = System.getProperty("org.ogema.evaluationofflinecontrol.scheduleviewer.expert.sensorsToFilterOut."+gwId);
								if(prop != null) {
									List<String> sensorsToFilterOut = Arrays.asList(prop.split(","));
									String shortId = tse.getProperty("deviceName");
									if(shortId != null)
										if(sensorsToFilterOut.contains(shortId)) continue;
								}
							}
							String inputLabel = ((GaRoDataTypeI)tse.type).label(null).replace("Measurement", "");
							shortNames.put(timeSeries, StringListFormatUtils.getStringFromList(tse.getIds(), inputLabel));
							longNames.put(timeSeries, StringListFormatUtils.getStringFromList(tse.getIds(), tsd.label(null), inputLabel));
						} else {
							shortNames.put(timeSeries, StringListFormatUtils.getStringFromList(tse.getIds()));
							longNames.put(timeSeries, StringListFormatUtils.getStringFromList(tse.getIds(), tsd.label(null)));
						}
					} else {
						shortNames.put(timeSeries, tsd.label(null));
						longNames.put(timeSeries, tsd.description(null));
					}
					if(timeSeries != null) result.add(timeSeries);
				}
				List<Collection<TimeSeriesFilter>> programs = new ArrayList<>();
				List<TimeSeriesFilter> programsInner = new ArrayList<>();
				programsInner.add(new DefaultTimeSeriesFilterExtended("Filter for "+eval.id(),
						shortNames, longNames, null, null));
				programs.add(programsInner);
				
				String config = selectConfig.getSelectedLabel(req);
				IntervalConfiguration itv = StandardConfigurations.getConfigDuration(config, app.appMan);
				final long startTime;
				final long endTime;
				if(itv.multiStart == null || itv.multiStart.length > 0) {
					startTime = itv.start;
					endTime = itv.end;
				} else {
					startTime = itv.multiStart[0];
					endTime = itv.multiEnd[itv.multiStart.length-1];
				}
				
				final ScheduleViewerConfiguration viewerConfiguration =
						ScheduleViewerConfigurationBuilder.newBuilder().setPrograms(programs).
						setStartTime(startTime).setEndTime(endTime).build();
				
				String ci = addConfig(new DefaultDedicatedTSSessionConfiguration(result, viewerConfiguration));
				setConfigId(ci, req);
			}
		};*/
		openScheduleViewer.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);

		Button backupButton = getBackupButton(page,
				"backupButton", multiSelectGWs, app, gateWaySelection, selectConfig);
		
		stopLastEvalButton = new Button(page, "stopLastEvalButton", "Stop Last Eval") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(lastEvalStarted != null) enable(req);
				else disable(req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				System.out.println("Last Eval Started: "+(lastEvalStarted != null));
				if(lastEvalStarted != null)
					System.out.println("Last Eval Started: "+(lastEvalStarted != null)+" eval:"+(lastEvalStarted.eval!=null));
				if(lastEvalStarted.eval == null)
					System.out.println("Last Eval Started: "+(lastEvalStarted != null)+" getEval():"+(lastEvalStarted.getEval()!=null));
				lastEvalStarted.getEval().stopExecution();
			}
		};
		
		addKPIPageButton = new Button(page, "addKPIPageButton", "Add KPI-pages offered by provider") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				final GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
				List<KPIPageDefinition> pdef = eval.getPageDefinitionsOffered();
				if(pdef != null && (!pdef.isEmpty()))
					setWidgetVisibility(true, req);
				else setWidgetVisibility(false, req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				final GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
				List<KPIPageDefinition> pdef = eval.getPageDefinitionsOffered();
				for(KPIPageDefinition def: pdef) {
					controller.addOrUpdatePageConfigFromProvider(def, eval);
					/*KPIPageConfig pageConfig = controller.configureTestReport(def.providerId,
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
					controller.createMultiPage(pageConfig, def.defaultIntervalsPerColumnType,
							def.urlAlias, false);
					if(def.specialIntervalsPerColumn != null && (!def.specialIntervalsPerColumn.isEmpty()))
					for(ResultToShow rts: pageConfig.resultsToShow().getAllElements()) {
						Integer specialNum = def.specialIntervalsPerColumn.get(rts.resultId().getValue());
						if(specialNum != null) {
							ValueResourceHelper.setCreate(rts.columnsIntoPast(), specialNum);
						}
					}
					controller.addMultiPage(pageConfig);*/
				}
			}
		};
		
		buttonAutoEvalConfig = new Button(page, "buttonAutoEvalConfig") {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				String text = getText(req);
				if(text == null || text.equals(defaultText))  {
					final GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
					int status = app.getAutoEvalStatus(eval.id());
					currentStarter(status, req);
					return;
				}
			}
			public void onPrePOST(String data, OgemaHttpRequest req) {
				String text = getText(req);
				final GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
				if(text.equals(statusEval)) {
					MultiKPIEvalConfiguration mulcon = app.getAutoEvalConfig(eval.id());
					final List<String> gwIDs = (List<String>) gateWaySelection.multiSelect.getSelectedLabels(req);
					final List<String> multiSelectedResults = (List<String>) resultsSelection.getSelectedLabels(req);
					mulcon.gwIds().setValues(gwIDs.toArray(new String[0]));
					mulcon.resultsRequested().setValues(multiSelectedResults.toArray(new String[0]));
					int status = app.getAutoEvalStatus(eval.id());
					currentStarter(status, req);
					//switchToAutoConfig(eval.id(), req);
				} else {
					setText(statusEval, req);
					//switchToEvalConfig(req);
				}
				initResultsGws(req, false);
			}
			
			private void currentStarter(int status, OgemaHttpRequest req) {
				if(status == 0) {
					setText(status0, req);					
				} else if(status == 1) {
					setText(status1, req);
				} else {
					setText(status2, req);					
				}
			}
		};
		
		BooleanResourceCheckbox autoEvalActiveCheck = new BooleanResourceCheckbox(page, "autoEvalActive", "Auto-Eval active") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				final GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
				MultiKPIEvalConfiguration config = controller.getAutoEvalConfig(eval.id());
				if(config == null) {
					disable(req);
				} else {
					enable(req);
					selectItem(config.performAutoQueuing(), req);
				}
			}
		};
		
		int i = 0;
		StaticTable table1 = new StaticTable(9, 3);
		page.append(table1);
		table1.setContent(i, 0, "Name/ID"		);
		table1.setContent(i, 1, selectProvider	);
		table1.setContent(i, 2, " 		"		);
		i++;
		table1.setContent(i, 0, "Configuration"	);
		table1.setContent(i, 1, selectConfig	);
		table1.setContent(i, 2, buttonAutoEvalConfig		);
		i++;
		table1.setContent(i, 0, "Result Selection"		);
		table1.setContent(i, 1, resultSelectionExtended	);
		table1.setContent(i, 2, autoEvalActiveCheck				);
		i++;
		table1.setContent(i, 0, "Gateways Selection"	);
		table1.setContent(i, 1, gateWaySelection		);
		table1.setContent(i, 2, "		"				);
		i++;
		table1.setContent(i, 0, "Single Value Intervals"	);
		table1.setContent(i, 1, selectSingleValueIntervals	);
		table1.setContent(i, 2, "       	"				);
		i++;
		table1.setContent(i, 0, selectPreEval1Count );
		table1.setContent(i, 1, selectPreEval1     	);
		table1.setContent(i, 2, overWriteDrop		);
		i++;
		table1.setContent(i, 0, selectPreEval2Count );
		table1.setContent(i, 1, selectPreEval2  	);
		table1.setContent(i, 2, openConfigResourceEdit	);
		i++;
		table1.setContent(i, 0, "Evaluation Acronym");
		table1.setContent(i, 1, evalName			);
		table1.setContent(i, 2, backupButton		);
		i++;
		table1.setContent(i, 0, startOfflineEval 	);
		table1.setContent(i, 1, openScheduleViewer  );
		table1.setContent(i, 2, stopLastEvalButton	);
		table1.setContent(i, 2, addKPIPageButton	);
		
		selectProvider.triggerAction(evalName, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(resultsSelection, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(startOfflineEval, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(openScheduleViewer, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(selectPreEval1Count, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(selectPreEval1, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(selectPreEval2Count, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(selectPreEval2, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(evalName, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(autoEvalActiveCheck, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
		resultsSelection.triggerAction(startOfflineEval, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		multiSelectGWs.triggerAction(startOfflineEval, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		multiSelectGWs.triggerAction(openScheduleViewer, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectPreEval1.triggerAction(startOfflineEval, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selectProvider.triggerAction(startOfflineEval, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		resultSelectionExtended.selectAllButton.triggerAction(startOfflineEval, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		resultSelectionExtended.deselectAllButton.triggerAction(startOfflineEval, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		gateWaySelection.selectAllButton.triggerAction(startOfflineEval, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		gateWaySelection.deselectAllButton.triggerAction(startOfflineEval, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		startOfflineEval.triggerOnPOST(stopLastEvalButton);
		buttonAutoEvalConfig.triggerOnPOST(startOfflineEval);
		buttonAutoEvalConfig.triggerOnPOST(selectConfig);
		buttonAutoEvalConfig.triggerOnPOST(multiSelectGWs);
		buttonAutoEvalConfig.triggerOnPOST(resultsSelection);
		buttonAutoEvalConfig.triggerOnPOST(buttonAutoEvalConfig);
		buttonAutoEvalConfig.triggerOnPOST(overWriteDrop);
		buttonAutoEvalConfig.triggerOnPOST(evalName);
		//selectConfig.triggerOnPOST(fromMinutesBeforeNow);
		//selectConfig.triggerOnPOST(toMinutesBeforeNow);
	}

	protected int getStatus(OgemaHttpRequest req) {
		String text = buttonAutoEvalConfig.getText(req);
		if(text == null || text.equals(defaultText)) return 0;
		switch(text) {
		case status0: return 0;
		case status1: return 1;
		case status2: return 2;
		case statusEval: return -1;
		default: throw new IllegalStateException("Unknown text of buttonAutoEvalConfig: "+text);
		}
	}

	protected void initResultsGws(OgemaHttpRequest req, boolean resultOnly) {
		GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
		if(eval != null ) {
			if(getStatus(req) >= 0) {
				if(!resultOnly) {
					Set<String> ids = controller.getGatewayIds();
					multiSelectGWs.update(ids, req);
					Collection<String> toUse = GatewayConfigPage.getGwsToUse(controller);
					multiSelectGWs.selectItems(toUse, req);
				}
				resultMultiSelection = eval.resultTypes();
				resultsSelection.update(resultMultiSelection, req);
				resultsSelection.selectItems(resultMultiSelection, req);
			} else {
				MultiKPIEvalConfiguration mulcon = getOrCreateAutoConfig(req);
				if(!resultOnly) {
					String[] multiGws = mulcon.gwIds().getValues();
					multiSelectGWs.selectItems(Arrays.asList(multiGws), req);
				}				
				String[] multiSelectedResults = mulcon.resultsRequested().getValues();
				List<ResultType> resultsRequested = getResultsSelected(Arrays.asList(multiSelectedResults), eval);
				resultsSelection.selectItems(resultsRequested, req);
			}	
		}
	}
	
	/*protected void switchToEvalConfig(OgemaHttpRequest req) {
		Collection<String> toUse = GatewayConfigPage.getGwsToUse(controller);
		multiSelectGWs.selectItems(toUse, req);		
	}
	protected void switchToAutoConfig(String providerId, OgemaHttpRequest req) {
		MultiKPIEvalConfiguration config = controller.getAutoEvalConfig(providerId);
		Collection<String> toUse;
		if(config == null)
			toUse = GatewayConfigPage.getGwsToUse(controller);
		else
			toUse = Arrays.asList(config.gwIds().getValues());
		multiSelectGWs.selectItems(toUse, req);		
	}*/
	
	/** This method could be offered as an OSGi service in the future
	 * For this task the method should be moved to the controller.
	 * Compared to {@link #startEvaluation(GaRoSingleEvalProvider, List, ChronoUnit, long, long, List, CSVArchiveExporter, String, OverwriteMode, Path)}
	 * this method mainly supports multiFileSuffix.*/
	public void startEvalutionLikeStartPage(String jsonFileName,
			GaRoSingleEvalProvider eval,
			IntervalConfiguration itv,
			ChronoUnit chronoUnit, List<ResultType> resultsRequested,
			List<String> gwIDs, OverwriteMode om, Path preEvalFile) {
		String newPath = "";
		int j = 1;
		String selectedEvalProvider = eval.getClass().getSimpleName();
		if (jsonFileName == null || jsonFileName.trim().isEmpty())
			jsonFileName = selectedEvalProvider+"Result";
		if(jsonFileName.endsWith(".json")) jsonFileName = jsonFileName.substring(0, jsonFileName.length()-5);
		String FILE_PATH = evalResultMan.getFilePath(null, 10, "");
		Path providerPath = Paths.get(FILE_PATH+"/"+jsonFileName+".json");
		if(!Files.isRegularFile(providerPath)) {
			newPath = jsonFileName;
		} else {
			while(j < JsonOGEMAFileManagementImpl.MAX_RESULT_WITH_SAME_BASE_NAME) {
				if(itv.multiFileSuffix != null) {
					boolean ok = true;
					for(String s: itv.multiFileSuffix) {
						providerPath = Paths.get(FILE_PATH+"/"+getFileNameByIndex(jsonFileName, j)+s+".json");
						if(Files.isRegularFile(providerPath)) {
							ok = false;
							break;
						}
					}
					if(ok) {
						newPath = getFileNameByIndex(jsonFileName, j);
						break;
					}
				} else {
					providerPath = Paths.get(FILE_PATH+"/"+getFileNameByIndex(jsonFileName, j)+".json");
					if(!Files.isRegularFile(providerPath)) {
						newPath = getFileNameByIndex(jsonFileName, j);
						break;
					}
				}
				j++;
			}
		}
		
		CSVArchiveExporter csvWriter = null;
		if(controller.appConfigData.writeCsv().getValue()) {
			csvWriter = new GatewayDataExportUtil.CSVArchiveExporterGDE(controller.gatewayDataExport);
		}
		
		if(itv.multiStart == null) {
			startEvaluation(eval, resultsRequested, chronoUnit, itv.start, itv.end, gwIDs, csvWriter,
					newPath+".json", om, preEvalFile);
		} else for(int i=0; i<itv.multiStart.length; i++) {
			itv.start = itv.multiStart[i];
			itv.end = itv.multiEnd[i];
			startEvaluation(eval, resultsRequested, chronoUnit, itv.start, itv.end, gwIDs, csvWriter,
					newPath+itv.multiFileSuffix[i]+".json", om, preEvalFile);
		}
		
	}
	
	private String getFileNameByIndex(String jsonFileName, int j) {
		String newPath = j < 10?jsonFileName+"_0"+j:jsonFileName+"_"+j;
		return newPath;
	}
	
	/** startEvaluation: Supports different starting modes. The standard start mode is now
	 * using the queue, but for generation of CSV imports still using
	 * GaRoEvalHelperJAXB.performGenericMultiEvalOverAllData is required. Also manual choice of
	 * pre-evaluation is still supported.<br>
	 * If standard mode is used then {@link OfflineEvaluationControlController#startEvaluationViaQueue(GaRoSingleEvalProvider, List, ChronoUnit, long, long, List, OverwriteMode, String)}
	 * is almost equivalent to this method.
	 * 
	 * @param eval
	 * @param resultsRequested
	 * @param chronoUnit
	 * @param start
	 * @param end
	 * @param gwIDs
	 * @param csvWriter
	 * @param newPath
	 * @param om
	 * @param preEvalFile may be null if manual selected pre-evaluation is not required
	 */
	private void startEvaluation(GaRoSingleEvalProvider eval,
			List<ResultType> resultsRequested,
			ChronoUnit chronoUnit, long start, long end, List<String> gwIDs,
			CSVArchiveExporter csvWriter, String newPath,
			OverwriteMode om, Path preEvalFile) {

		Class<? extends GaRoSuperEvalResult<?>> typeToUse = null;
		if(eval.getSuperResultClassForDeserialization() != null)
			typeToUse = eval.getSuperResultClassForDeserialization();
		
		boolean usedPreEval = false;
		if(eval instanceof GaRoSingleEvalProviderPreEvalRequesting && (!controller.appConfigData.autoPreEval().getValue())) {
			GaRoSingleEvalProviderPreEvalRequesting peval = (GaRoSingleEvalProviderPreEvalRequesting)eval;
			if(peval.preEvaluationsRequested() != null) switch(peval.preEvaluationsRequested().size()) {
			case 0:
				break;
			case 1:
				/**TODO: Use SuperEval-class provided by the respective provider via
				 * {@link GaRoMultiResultExtended#getSuperResultClassForDeserialization()}
				 */
				GaRoPreEvaluationProvider preProvider = 
						new GaRoStdPreEvaluationProvider<GaRoMultiResultDeser, GaRoSuperEvalResult<GaRoMultiResultDeser>>
				(GaRoSuperEvalResultDeser.class, preEvalFile.toString());
			
				//CHECK
				lastEvalStarted = evalResultMan.performGenericMultiEvalOverAllData(eval.getClass(),
						start, end, // TODO: SK
						chronoUnit,
						new GaRoPreEvaluationProvider[] {preProvider}, resultsRequested, gwIDs, newPath,
						eval.id(), typeToUse, true, controller.getDataProvidersToUse());
				usedPreEval = true;
				break;
			case 2:
				//TODO
				throw new UnsupportedOperationException("Two pre evaluations not implemented yet!");
				/*Path preEvalFile1 = Paths.get(FILE_PATH+"/"+selectPreEval1.getSelectedLabel(req));
				GaRoPreEvaluationProvider<Resource> preProvider1 = 
						new GaRoStdPreEvaluationProvider<Resource, OutsideTemperatureMultiResult, GaRoSuperEvalResult<Resource, OutsideTemperatureMultiResult>>
				(GaRoSuperEvalResultOut.class, preEvalFile1.toString());

				Path preEvalFile2 = Paths.get(FILE_PATH+"/"+selectPreEval2.getSelectedLabel(req));
				GaRoPreEvaluationProvider<Resource> preProvider2 = 
						new GaRoStdPreEvaluationProvider<Resource, GaRoMultiResultDeser, GaRoSuperEvalResult<Resource, GaRoMultiResultDeser>>
				(GaRoSuperEvalResultDeser.class, preEvalFile2.toString());

				
				GaRoEvalHelperJAXB.performGenericMultiEvalOverAllData(selectProvider.getSelectedItem(req).getClass(),
						controller.serviceAccess.gatewayParser,
						start, end,
						chronoUnit,
						null, false,
						new GaRoPreEvaluationProvider[] {preProvider1, preProvider2}, resultsRequested, gwIDs, newPath);
				usedPreEval = true;
				break;*/
			case 3:
				//TODO
				break;
			default:
				throw new IllegalStateException("maximum 3 PreEvaluation supported!");
			}
		}
		
		if(!usedPreEval) {
			if(controller.appConfigData.autoPreEval().getValue()) {
				controller.startEvaluationViaQueue(eval, resultsRequested, chronoUnit, start, end,
						gwIDs, om, newPath);
			} else if(!controller.appConfigData.writeCsv().getValue())
				lastEvalStarted = evalResultMan.performGenericMultiEvalOverAllData(eval.getClass(),
					start, end,
					chronoUnit,
					null, resultsRequested, gwIDs, newPath,
					eval.id(), typeToUse, true,
					controller.getDataProvidersToUse());
			else {
				//without queue. This is just supported for csvWriting
				lastEvalStarted = GaRoEvalHelperJAXB_V2.performGenericMultiEvalOverAllData(eval.getClass(),
					((OfflineEvalServiceAccess)controller.serviceAccess).gatewayParser(),
					start, end,
					chronoUnit,
					csvWriter, true, null, resultsRequested, gwIDs, newPath, null);
			}
		} 
	}

	private void addHeader() {
		Header header = new Header(page, "header", "Offline Evaluation Control");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
	}

	public static EvaluationProvider getEvalationProviderById(String providerID, Collection<EvaluationProvider> providers) {
		for(EvaluationProvider eval : providers) {
			if(eval != null && providerID.equals(eval.id())) {
				return eval;
			}
		}
		return null;
	}

	private List<GaRoSingleEvalProvider> getProviders() {
		//return controller.serviceAccess.getEvaluations().values();
		return 	controller.serviceAccess.getEvaluations().values().stream()
				.filter(provider -> provider instanceof GaRoSingleEvalProvider)
				.map(provider -> (GaRoSingleEvalProvider) provider)
				.collect(Collectors.toList());

	}

	private List<ResultType> getResultsSelected(List<String> multiSelectedResults, GaRoSingleEvalProvider eval) {
		List<ResultType> resultsRequested = new ArrayList<>();
		
		for(String name : multiSelectedResults) {
			for(ResultType rt : eval.resultTypes() ) {
				if(name.equals(rt.label(OgemaLocale.ENGLISH))) {
					resultsRequested.add(rt);
				}
			}
		}
		return resultsRequested;
	}
	
	private MultiKPIEvalConfiguration getOrCreateAutoConfig(OgemaHttpRequest req) {
		GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
		List<String> multiSelectedResults = (List<String>) resultsSelection.getSelectedLabels(req);
		List<ResultType> resultsRequested = getResultsSelected(multiSelectedResults, eval);
		List<String> gwIDs = (List<String>) gateWaySelection.multiSelect.getSelectedLabels(req);
		ChronoUnit chronoUnit = getChronoUnit(req);	
		if(chronoUnit.getDuration().getSeconds() < 24*3600)
			chronoUnit = ChronoUnit.DAYS;
		MultiKPIEvalConfiguration configLoc = controller.getOrCreateEvalConfig(eval.id(), resultsRequested, chronoUnit, gwIDs, false,
				OfflineEvaluationControlController.APP_AUTO_SUBID);
		return configLoc;
	}

	private ChronoUnit getChronoUnit(OgemaHttpRequest req) {
		String singleValue = selectSingleValueIntervals.getSelectedLabel(req);
		switch(singleValue) {
		case "Days Value": 
			return ChronoUnit.DAYS;
		
		case "Weeks Value": 
			return ChronoUnit.WEEKS;
		
		case "Months Value": 
			return ChronoUnit.MONTHS;
		case "Hours Value":
			return ChronoUnit.HOURS;
		case "Minutes Value":
			return ChronoUnit.MINUTES;
		case "Current Hour Single From-To Minutes":
			return ChronoUnit.MINUTES;
		default: return null;
		}
	}
	
	public static interface ScheduleViewerOpenButtonDataProvider {
		List<TimeSeriesData> getData(OgemaHttpRequest req);
		IntervalConfiguration getITVConfiguration(String config, ApplicationManager appMan);
	}
	public static ScheduleViewerOpenButton getScheduleViewerOpenButton(WidgetPage<?> page, String widgetId,
			TemplateMultiselect<String> multiSelectGWs,
			TemplateDropdown<GaRoSingleEvalProvider> selectProvider,
			OfflineEvaluationControlController controller,
			//MultiSelectExtended<String> gateWaySelection,
			TemplateDropdown<String> selectConfig,
			ScheduleViewerOpenButtonDataProvider provider) {
		return new ScheduleViewerOpenButtonEval(page, widgetId, "Data Viewer",
				ScheduleViewerConfigProvEvalOff.PROVIDER_ID,
				ScheduleViewerConfigProvEvalOff.getInstance()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected List<TimeSeriesData> getTimeseries(OgemaHttpRequest req) {
				return provider.getData(req);
			}

			@Override
			protected String getEvaluationProviderId(OgemaHttpRequest req) {
				return selectProvider.getSelectedItem(req).id();
			}

			@Override
			protected IntervalConfiguration getITVConfiguration(OgemaHttpRequest req) {
				String config = selectConfig.getSelectedLabel(req);
				return provider.getITVConfiguration(config, controller.appMan);
			}
	
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(multiSelectGWs.getSelectedItems(req).isEmpty()) {
					disable(req);
				} else enable(req);
			};

		};
	}
	
	public static Button getBackupButton(WidgetPage<?> page, String widgetId,
			TemplateMultiselect<String> multiSelectGWs,
			//TemplateDropdown<GaRoSingleEvalProvider> selectProvider,
			OfflineEvaluationControlController controller,
			MultiSelectExtended<String> gateWaySelection,
			TemplateDropdown<String> selectConfig) {
	return new RemoteSlotsDBBackupButton(page, widgetId, "Backup data for GWs/Interval") {
		private static final long serialVersionUID = 1L;
		
		@Override
		public void onGET(OgemaHttpRequest req) {
			if(multiSelectGWs.getSelectedItems(req).isEmpty()) {
				setWidgetVisibility(false, req);
			} else if(controller.showBackupButton())
				setWidgetVisibility(true, req);
			else
				setWidgetVisibility(false, req);
		};

		@Override
		protected IntervalConfiguration getInterval(OgemaHttpRequest req) {
			String config = selectConfig.getSelectedLabel(req);
			if(config.equals("TestOneMonth")) {
				IntervalConfiguration r = new IntervalConfiguration();
				long now = controller.appMan.getFrameworkTime();
				r.end = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.MONTH)-1;
				r.end = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -1, AbsoluteTiming.MONTH);
				r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -1, AbsoluteTiming.MONTH);
				return r;
			}
			return StandardConfigurations.getConfigDuration(config, controller.appMan);
		}
		
		@Override
		protected List<String> getGWIds(OgemaHttpRequest req) {
			return (List<String>) gateWaySelection.multiSelect.getSelectedLabels(req);
		}
	};
	}
}
