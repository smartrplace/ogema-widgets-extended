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

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.util.evalcontrol.EvalScheduler;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;
import com.iee.app.evaluationofflinecontrol.config.KPIPageConfig;
import com.iee.app.evaluationofflinecontrol.config.ResultToShow;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.multiselect.extended.MultiSelectExtendedStringArray;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;

public class ConfigurationPage {
	//private final WidgetPage<?> page;
	private final OfflineEvaluationControlController controller;

	public ConfigurationPage(final WidgetPage<?> page, final OfflineEvaluationControlController app) {
		
		//this.page = page;
		this.controller = app;
		
		MultiSelectExtendedStringArray<DataProvider<?>> dataProviderSelect = new MultiSelectExtendedStringArray<DataProvider<?>>(page, "dataProviderSelect",
				true, null, false, true, app.appConfigData.dataProvidersToUse()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Collection<DataProvider<?>> getItems(String[] currentlySelected, OgemaHttpRequest req) {
				Collection<DataProvider<?>> result = new ArrayList<DataProvider<?>>();
				for(DataProvider<?> p: app.serviceAccess.getDataProviders().values()) {
					if(p instanceof GaRoMultiEvalDataProvider) result.add(p);
				}
				return result ;
			}

			@Override
			protected DataProvider<?> getItemByString(String id) {
				return app.getDataProvider(id);
			}

			@Override
			protected String getId(DataProvider<?> item) {
				return item.id();
			}

			@Override
			protected String getLabel(DataProvider<?> item, OgemaLocale locale) {
				return item.label(locale);
			}
		};
		
		BooleanResourceCheckbox writeCSV = new BooleanResourceCheckbox(page, "writeCsv",
				"", app.appConfigData.writeCsv()) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				BooleanResource sel = getSelectedItem(req);
				if(!sel.isActive()) {
					sel.create();
					sel.activate(false);
				}
			}
		};
		
		BooleanResourceCheckbox autoPreEval = new BooleanResourceCheckbox(page, "autoPreEval",
				"", app.appConfigData.autoPreEval());
		SimpleCheckbox checkPerformAutoEval = new SimpleCheckbox(page, "checkPerformAutoEval", "") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				boolean stat = controller.serviceAccess.evalResultMan().getEvalScheduler().isAutoEvaluationActive();
				setValue(stat, req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				boolean stat = getValue(req);
				EvalScheduler scheduler = controller.serviceAccess.evalResultMan().getEvalScheduler();
				if(stat) scheduler.activateAutoEvaluation();
				else scheduler.deactivateAutoEvaluation();
			}
		};
		SimpleCheckbox checkShowShortIntervals = new SimpleCheckbox(page, "checkShowShortIntervals", "") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				setValue(controller.showShortIntervalsInKPIs, req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				boolean stat = getValue(req);
				controller.showShortIntervalsInKPIs = stat;
			}
		};
		BooleanResourceCheckbox showBackup = new BooleanResourceCheckbox(page, "showBackup",
				"", app.appConfigData.showBackupButton());
		
		/** Basic Quality Evaluation*/
		Button configureMultiGWQualityPage = new Button(page, "configureMultiGWQualityPage",
				"Configure Multi-GW Basic Quality KPI page") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				List<String[]> resultIds = new ArrayList<>();
				resultIds.add(new String[]{"TS_TOTAL", "TS_WITH_DATA", "TS_GOOD", "POWER_NUM", "POWER_TIME_REL",
						"OVERALL_GAP_REL", "DURATION_HOURS"});
				KPIPageConfig pageConfig = controller.configureTestReport(Arrays.asList(new String[] {"basic-quality_eval_provider"}),
						resultIds, null,
						"Multi-GW Basic Quality Report",
						true, TEST_CHRONO, TEST_DEFAULT_INTERVALS_TO_CALC, true);
				controller.createMultiPage(pageConfig, 2, "basicQuality", false);
				for(ResultToShow rts: pageConfig.resultsToShow().getAllElements()) {
					if(rts.resultId().getValue().equals("DURATION_HOURS")) {
						ValueResourceHelper.setCreate(rts.columnsIntoPast(), 1);
					}
					if(rts.resultId().getValue().equals("POWER_NUM")) {
						ValueResourceHelper.setCreate(rts.columnsIntoPast(), 1);
					}
				}
				controller.addMultiPage(pageConfig);
			}
		};
		
		Button configureMultiGWQualityRecPage = new Button(page, "configureMultiGWQualityRecPage",
				"Configure Multi-GW Basic Quality Recurrent-Eval KPI page") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				List<String[]> resultIds = new ArrayList<>();
				resultIds.add(new String[]{"TS_TOTAL", "TS_WITH_DATA", "TS_GOOD", "POWER_NUM", "POWER_TIME_REL",
						"OVERALL_GAP_REL", "DURATION_HOURS"});
				KPIPageConfig pageConfig = controller.configureTestReport(Arrays.asList(new String[] {"basic-recurrent-quality_eval_provider"}),
						resultIds, null,
						"Multi-GW Basic Quality Report Recurrent",
						true, TEST_CHRONO, TEST_DEFAULT_INTERVALS_TO_CALC, true);
				controller.createMultiPage(pageConfig, 2, "basicQualityRecurrent", false);
				for(ResultToShow rts: pageConfig.resultsToShow().getAllElements()) {
					if(rts.resultId().getValue().equals("DURATION_HOURS")) {
						ValueResourceHelper.setCreate(rts.columnsIntoPast(), 1);
					}
					if(rts.resultId().getValue().equals("POWER_NUM")) {
						ValueResourceHelper.setCreate(rts.columnsIntoPast(), 1);
					}
				}
				controller.addMultiPage(pageConfig);
				
				resultIds = new ArrayList<>();
				resultIds.add(new String[]{"TS_WITH_DATA",
						"OVERALL_GAP_REL", "DURATION_HOURS", "STATUS_CHANGE"});
				pageConfig = controller.configureTestReport(Arrays.asList(new String[] {"basic-recurrent-quality_eval_provider"}),
						resultIds, null,
						"Quality Status Change Report",
						true, TEST_CHRONO, TEST_DEFAULT_INTERVALS_TO_CALC, true);
				controller.createMultiPage(pageConfig, 2, "basicQualityRecStatusCh", false);
				for(ResultToShow rts: pageConfig.resultsToShow().getAllElements()) {
					if(rts.resultId().getValue().equals("DURATION_HOURS")) {
						ValueResourceHelper.setCreate(rts.columnsIntoPast(), 1);
					}
				}
				controller.addMultiPage(pageConfig);

			}
		};
		
		Button configureMultiGWQualityPageWin = new Button(page, "configureMultiGWQualityPageWin",
				"Configure Multi-GW Basic Quality KPI page for Window Openings (Sub)") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				List<String[]> resultIds = new ArrayList<>();
				resultIds.add(new String[]{"WIN_OPEN_PERDAY", "TS_WITH_DATA", "TS_GOOD",
						"OVERALL_GAP_REL", "DURATION_HOURS", "timeOfCalculation"});
				KPIPageConfig pageConfig = controller.configureTestReport(Arrays.asList(new String[] {"basic-quality_eval_provider"}),
						resultIds, null, //TEST_MULTI_GW_IDs,
						"Multi-GW Basic Quality Report For Window Opening",
						true, TEST_CHRONO, TEST_DEFAULT_INTERVALS_TO_CALC, true);
				controller.createMultiPage(pageConfig, 1, "basicQualityForWindow", false);
				for(ResultToShow rts: pageConfig.resultsToShow().getAllElements()) {
					if(rts.resultId().getValue().equals("WIN_OPEN_PERDAY")) {
						ValueResourceHelper.setCreate(rts.columnsIntoPast(), 2);
					}
				}
				controller.addMultiPage(pageConfig);
			}
		};

		Button configureMultiGWQualityRexoPage = new Button(page, "configureMultiGWQualityRexoPage",
				"Configure Multi-GW Basic Quality KPI page focused on HR electricity power") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				List<String[]> resultIds = new ArrayList<>();
				resultIds.add(new String[]{"TS_WITH_DATA", "POWER_NUM", "POWER_TIME_REL",
						"OVERALL_GAP_REL", "DURATION_HOURS",  "timeOfCalculation"});
				KPIPageConfig pageConfig = controller.configureTestReport(Arrays.asList(new String[] {"basic-quality_eval_provider"}),
						resultIds, null,
						"Multi-GW Basic Quality Report for Rexometer",
						true, TEST_CHRONO, TEST_DEFAULT_INTERVALS_TO_CALC, true);
				controller.createMultiPage(pageConfig, 2, "basicQualityRexo", false);
				for(ResultToShow rts: pageConfig.resultsToShow().getAllElements()) {
					if(rts.resultId().getValue().equals("DURATION_HOURS")) {
						ValueResourceHelper.setCreate(rts.columnsIntoPast(), 1);
					}
					if(rts.resultId().getValue().equals("POWER_NUM")) {
						ValueResourceHelper.setCreate(rts.columnsIntoPast(), 1);
					}
					if(rts.resultId().getValue().equals("OVERALL_GAP_REL")) {
						ValueResourceHelper.setCreate(rts.columnsIntoPast(), 1);
					}
				}
				controller.addMultiPage(pageConfig);
			}
		};

		Button configureLoadMonitorPage = new Button(page, "configureLoadMonitorPage",
				"Configure Load Monitoring KPI page and start test evaluation (2Gws)") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				List<String[]> resultIds = new ArrayList<>();
				resultIds.add(new String[]{"Average_Power"});
				resultIds.add(new String[]{"Valve_Full_Load_Hours__gaps_filled_"});
				resultIds.add(new String[]{"Points"});
				configureTestReport(Arrays.asList(new String[] {"base_electricity_profile_eval_provider",
						"base_valveHour_provider_gen", "base_competition_status_provider_gen"}),
						resultIds,
						"Load Monitoring Report",
						false);
			}
		};
		Button configurePPHCPage = new Button(page, "configurePPHCPage",
				"Configure Presence-Predictive Heat Control KPI page and start test evaluation (1Gw)") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				List<String[]> resultIds = new ArrayList<>();
				resultIds.add(new String[]{"GAP_TIME_REL", "ONLY_GAP_REL", "Presence_Timeshare", "Absence_Timeshare",
						"TOTAL_HOURS", "DURATION_HOURS"});
				resultIds.add(new String[]{"Presence_Timeshare"});
				resultIds.add(new String[]{"Heating_Timeshare"});
				resultIds.add(new String[]{"True_Positive_Share",
						"False_Positive_Share", "False_Negative_Share", "F_Measure"});
				controller.configureTestReport(Arrays.asList(new String[] {"autoheattime-presence_cleaner_eval_provider",
						"autoheattime-regular_eval_provider",
						"auth-predictive-presence-heatcontrol_eval_provider", "auth-pred_heatcontrol_eval_provider"}),
						resultIds, TEST_GW_IDs,
						"Presence-Predictive Heat Control Status Report (Weeks)",
						false, ChronoUnit.WEEKS,
						TEST_DEFAULT_INTERVALS_TO_CALC, false);
			}
		};
		
		RedirectButton gatewaysToUseButton = new RedirectButton(page, "gatewaysToUseButton",
				"Gateway Configuration", "configurationGws.html");
		gatewaysToUseButton.setDefaultOpenInNewTab(true);
		
		StaticTable configTable = new StaticTable(15, 2);
		configTable.setContent(0, 0, "Select DataProviders to Use").setContent(0, 1, dataProviderSelect);
		configTable.setContent(1, 0, "Write CSV Files of evaluation input (requires Autodetect to be disabled)").
				setContent(1, 1, writeCSV);
		configTable.setContent(2, 0, "Autodetect and complete required Pre-Evaluation data").
				setContent(2, 1, autoPreEval);
		configTable.setContent(3, 0, "Perform Auto-Evaluation").
			setContent(3, 1, checkPerformAutoEval);
		configTable.setContent(4, 0, "Offer short intervals in KPIPages").
		setContent(4, 1, checkShowShortIntervals);
		configTable.setContent(5, 0, "Show backup button in eval start page").
		setContent(5, 1, showBackup);
		int configButtonIdx = 6;
		configTable.setContent(configButtonIdx, 0, "Configure Gateways to Use").
			setContent(configButtonIdx, 1, gatewaysToUseButton);
		
		int i = configButtonIdx+2;
		configTable.setContent(i,  1, configureLoadMonitorPage);
		configTable.setContent(i+1,  1, configurePPHCPage);
		configTable.setContent(i+2,  1, configureMultiGWQualityPage);
		configTable.setContent(i+3,  1, configureMultiGWQualityPageWin);
		configTable.setContent(i+4,  1, configureMultiGWQualityRexoPage);
		//configTable.setContent(11,  1, configureMultiGWHumidityPage);
		configTable.setContent(i+5,  1, configureMultiGWQualityRecPage);
		page.append(configTable);
	}
	
	public static final ChronoUnit TEST_CHRONO = ChronoUnit.DAYS;
	//public static final OverwriteMode TEST_OM = OverwriteMode.ONLY_PROVIDER_REQUESTED;
	public static final String[] TEST_GW_IDs = new String[] {"_16013"};
	public static final String[] TEST_MULTI_GW_IDs = new String[] {"_16009", "_16013"};
	public static final int TEST_DEFAULT_INTERVALS_TO_CALC = 3;
	
	private KPIPageConfig configureTestReport(List<String> providerId, List<String[]> resultIds,
			String configName, boolean configureSubGws) {
		return controller.configureTestReport(providerId, resultIds,
				configureSubGws?TEST_MULTI_GW_IDs:TEST_GW_IDs,
				configName, configureSubGws, TEST_CHRONO, TEST_DEFAULT_INTERVALS_TO_CALC, false);
	}
	
	/** Configure {@link KPIPageConfig} based on a list of EvaluationProviders
	 * and start all evaluations required for this.
	 * 
	 * @param providerId
	 * @param resultIds
	 * @param configName
	 * @param configureSubGws currently only two options for the list of gateways included are supported.
	 * TODO: This has to be changed
	 * TODO: It seems that the evaluation is performed for each gateway and then for all gateways if true
	 * @param chronoUnit currently the evaluation interval is fixed to TEST_DEFAULT_INTERVALS_TO_CALC of
	 * 		chronoUnit
	 * TODO: This has to be changed
	 */
	/*private KPIPageConfig configureTestReport(List<String> providerId, List<String[]> resultIds,
			String configName, boolean configureSubGws, ChronoUnit chronoUnit,
			boolean configOnly) {
		EvalScheduler scheduler = controller.serviceAccess.evalResultMan.getEvalScheduler();
		controller.appConfigData.kpiPageConfigs().create();
		KPIPageConfig testConfig = getOrCreateConfig(configName);
		testConfig.resultsToShow().delete();
		testConfig.resultsToShow().create();

		if(configOnly) {
		for(int i=0; i<providerId.size(); i++) {
			//Note that intervalType etc. may not fit, but we do not care for sub-configs here
			MultiKPIEvalConfiguration comfort = scheduler.getConfig(providerId.get(i),
					OfflineEvaluationControlController.APP_EVAL_SUBID);
			for(String res: resultIds.get(i)) {
				ResultToShow result1 = testConfig.resultsToShow().add();
				result1.resultId().<StringResource>create().setValue(res);
				result1.evalConfiguration().setAsReference(comfort);
			}
		}
		return testConfig;
		}
		
		List<MultiKPIEvalConfiguration> evalsToStart = new ArrayList<>();

		for(int i=0; i<providerId.size(); i++) {
			MultiKPIEvalConfiguration comfort = configureTestReportForProvider(providerId.get(i),
					configName, resultIds.get(i), configureSubGws, chronoUnit, testConfig);
			evalsToStart.add(comfort);
		}


		controller.appConfigData.kpiPageConfigs().activate(true);
		
		
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
			
			controller.startEvaluationViaQueue(config, startEnd[0], startEnd[1], TEST_OM, null, providersDone);
			//scheduler.queueEvalConfig(config , true, null, null, startEnd[0], startEnd[1],
			//		controller.getDataProvidersToUse(), true, TEST_OM, false, null, providersDone);
		}
		return testConfig;
	}
	private MultiKPIEvalConfiguration configureTestReportForProvider(String providerId, String configName,
			String[] resultIds, boolean configureSubGws, ChronoUnit chronoUnit, KPIPageConfig testConfig) {
		MultiKPIEvalConfiguration comfort = configureProvider(providerId,
				configureSubGws?resultIds:null,
				configureSubGws?TEST_MULTI_GW_IDs:TEST_GW_IDs, chronoUnit);
		if(configureSubGws) {
			ValueResourceHelper.setCreate(comfort.queueGatewaysIndividually(), true);
		}
		
		for(String res: resultIds) {
			ResultToShow result1 = testConfig.resultsToShow().add();
			result1.resultId().<StringResource>create().setValue(res);
			result1.evalConfiguration().setAsReference(comfort);
		}
		
		return comfort;
	}*/
	
	/** Configure {@link MultiKPIEvalConfiguration} for a provider with subConfigId "OfflineEvaluationControl"
	 * 
	 * @param providerId
	 * @param resultsRequested if null the result types are not set, which should lead to a calculation
	 * 		of all result types
	 * @return
	 */
	/*private MultiKPIEvalConfiguration configureProvider(String providerId,
			String[] resultsRequested, String[] gwIds, ChronoUnit chronoUnit) {
		EvalScheduler scheduler = controller.serviceAccess.evalResultMan.getEvalScheduler();
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
		if((!(gwIds.length == 0)) && !startConfig.gwIds().exists()) {
			startConfig.gwIds().create();
		}
		startConfig.gwIds().setValues(gwIds);
		startConfig.activate(true);
		return startConfig;
	}*/

}
