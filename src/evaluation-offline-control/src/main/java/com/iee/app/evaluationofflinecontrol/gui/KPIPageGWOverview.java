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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.util.directresourcegui.kpi.KPIMonitoringReport;
import org.ogema.util.directresourcegui.kpi.KPIResultType;
import org.ogema.util.directresourcegui.kpi.KPIStatisticsManagement;
import org.ogema.util.jsonresult.management.EvalResultManagementStd;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;
import com.iee.app.evaluationofflinecontrol.config.KPIPageConfig;
import com.iee.app.evaluationofflinecontrol.config.ResultToShow;

import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.extended.resource.DefaultResourceTemplate;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.Linebreak;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;

/** This KPI page allows to show a number of evaluation results for all gateways evaluated.
 * See {@link KPIShowConfigurationPage} for a documentation regarding the configuration of the page.
 * Note that a configuration can be used for both types of configuration-based pages.
 * Note that a gateway overview requires that KPIs are calculated separately for each gateway.
 * 
 * @author dnestle
 *
 */
public class KPIPageGWOverview extends KPIMonitoringReport {
	public static final Integer[] INTERVALS_OFFERED =
			new Integer[]{AbsoluteTiming.DAY, AbsoluteTiming.WEEK, AbsoluteTiming.MONTH};
	public static final List<Integer> INTERVALS_OFFERED_WSHORT =
			Arrays.asList(new Integer[]{AbsoluteTiming.DAY, AbsoluteTiming.WEEK, AbsoluteTiming.MONTH,
					AbsoluteTiming.HOUR, AbsoluteTiming.MINUTE});

	protected final OfflineEvaluationControlController app;
	private TemplateDropdown<KPIPageConfig> configDrop;
	private TemplateDropdown<String> resultDrop;
	
	@Override
	protected List<Integer> getIntervalTypes(OgemaHttpRequest req) {
		if(app.showShortIntervalsInKPIs) return INTERVALS_OFFERED_WSHORT;
		else return super.getIntervalTypes(req);
	}
	
	public KPIPageGWOverview(WidgetPage<?> page, OfflineEvaluationControlController app) {
		this(page, app, true);
	}
	public KPIPageGWOverview(WidgetPage<?> page, OfflineEvaluationControlController app, boolean autoBuildPage) {
		super(page, app.appMan, app.serviceAccess.evalResultMan().getEvalScheduler(),
				Arrays.asList(INTERVALS_OFFERED),
				3, true, EvalResultManagementStd.STANDARD_MULTIEVAL_INTERVAL_STEP, false,
				Arrays.asList(new String[]{"Gateway"}), autoBuildPage);
		if(scheduler == null) throw new IllegalStateException("We need an implementation with scheduler here!");
		this.app = app;
		retardationOnGET = 1000;
	}

	protected Header getHeader(WidgetPage<?> page) {
		return new Header(page, "header", "KPIs per gateway");		
	}

	@Override
	public Collection<KPIResultType> getObjectsInTable(OgemaHttpRequest req) {
		KPIPageConfig entrRes = configDrop.getSelectedItem(req);
		String resultId = resultDrop.getSelectedItem(req);
		if(entrRes == null || resultId == null) return Collections.emptyList();
		
		ResultToShow resToShow = getResultToShow(entrRes, resultId);
		if(resToShow == null) return Collections.emptyList();
		List<KPIResultType> result = new ArrayList<>();
		for(String gwId: resToShow.evalConfiguration().gwIds().getValues()) {
			KPIStatisticsManagementI ksm = getKPIManagementSingle(resToShow.evalConfiguration(),
					resultId, gwId);
			result .add(new KPIResultType(ksm, gwId));
		}
		KPIStatisticsManagementI ksm = getKPIManagementSingle(resToShow.evalConfiguration(),
				resultId, null);
		result.add(new KPIResultType(ksm, "Overall"));
		return result;
	}

	private ResultToShow getResultToShow(KPIPageConfig entrRes, String resultId) {
		//for(ResultToShow tabEntry: entrRes.resultsToShow().getAllElements()) {
		for(ResultToShow tabEntry: entrRes.resultsToShow().getAllElements()) {
			if(resultId.equals(tabEntry.resultId().getValue())) {
				return tabEntry;
			}
		}
		return null;
	}
	
	private List<KPIStatisticsManagementI> getKPIManagement(MultiKPIEvalConfiguration startConfig,
			String subGateway) {
		String providerId = startConfig.evaluationProviderId().getValue();
		GaRoSingleEvalProvider eval = scheduler.getProvider(providerId);
		return scheduler.configureKPIManagement(startConfig, eval, subGateway, null);
	}
	
	/** Get the {@link KPIStatisticsManagement} for a certain resultType
	 * 
	 * @param startConfig
	 * @param resultId
	 * @param gwSubId if null the normal evaluation over all gateways is addressed
	 * @return
	 */
	private KPIStatisticsManagementI getKPIManagementSingle(MultiKPIEvalConfiguration startConfig,
			String resultId, String gwSubId) {
		List<KPIStatisticsManagementI> result = getKPIManagement(startConfig, gwSubId);
		for(KPIStatisticsManagementI kpi: result) {
			if(kpi.resultTypeId().equals(resultId)) return kpi;
		}
		return null;
	}
	
	protected void addWidgetsAboveTableSuper() {
		super.addWidgetsAboveTable();
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		configDrop = new TemplateDropdown<KPIPageConfig>(page, "configDrop") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				List<KPIPageConfig> items = app.appConfigData.kpiPageConfigs().getAllElements();
				update(items, req);
			}
		};
		configDrop.setTemplate(new DefaultResourceTemplate<KPIPageConfig>());
		//page.append(configDrop);
		
		resultDrop = new TemplateDropdown<String>(page, "resultDrop") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				KPIPageConfig itemUp = configDrop.getSelectedItem(req);
				if(itemUp == null) {
					update(Collections.emptyList(), req);
					return;
				}
				List<String> results = new ArrayList<>();
				for(ResultToShow c: itemUp.resultsToShow().getAllElements()) {
					results.add(c.resultId().getValue());
				}
				update(results , req);
			}
		};

		this.dateOfReport = new Label(page, "dateOfReport") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				KPIPageConfig itemUp = configDrop.getSelectedItem(req);
				String resultId = resultDrop.getSelectedItem(req);
				ResultToShow item = null;
				if(itemUp != null && resultId != null) item  = getResultToShow(itemUp, resultId);
				KPIStatisticsManagementI ksm = null;
				if(item != null) ksm = getKPIManagementSingle(
						item.evalConfiguration(), resultId, null);
				if(ksm != null) {
					int intervalType = intervalDrop.getSelectedItem(req);
					SampledValue val = ksm.getIntervalSchedule(intervalType).getPreviousValue(Long.MAX_VALUE);
					if(val != null)
						currentTime = val.getTimestamp();
					else
						currentTime = appMan.getFrameworkTime();					
				} else
					currentTime = appMan.getFrameworkTime();
				String timeOfReport = "Time of Report: " + TimeUtils.getDateAndTimeString(currentTime);
				setText(timeOfReport, req);
			}
		};
		triggerOnPost(dateOfReport, intervalDrop);
		triggerOnPost(configDrop, dateOfReport);
		triggerOnPost(configDrop, resultDrop);
		triggerOnPost(configDrop, mainTable);
		triggerOnPost(resultDrop, mainTable);

		StaticTable topTable = new StaticTable(1, 3);
		topTable.setContent(0, 0, configDrop).setContent(0, 1, resultDrop).setContent(0, 2, intervalDrop);
		
		this.header = getHeader(page);
		page.append(header);
		page.append(Linebreak.getInstance());
		page.append(dateOfReport);
		
		page.append(topTable);
	}
	
	@Override
	protected void addWidgetsBelowTable() {
		//do nothing here
	}

}
