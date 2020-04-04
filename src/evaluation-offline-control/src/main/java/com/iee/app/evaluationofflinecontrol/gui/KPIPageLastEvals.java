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
import org.ogema.model.jsonresult.MultiEvalStartConfiguration;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.util.directresourcegui.kpi.KPIMonitoringReport;
import org.ogema.util.directresourcegui.kpi.KPIResultType;
import org.ogema.util.jsonresult.management.EvalResultManagementStd;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;

import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.Linebreak;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.template.DefaultDisplayTemplate;

/** This KPI page allows to select a {@link MultiEvalStartConfiguration} and show the available KPIs for the
 * last setting of the configuration meaning the last start of the respective EvaluationProvider. The
 * page {@link KPIPage} shows the KPIs calculated via the Auto-Calculation mechanism of the MainPage.
 * 
 * @author dnestle
 *
 */
public class KPIPageLastEvals extends KPIMonitoringReport {
	//private final OfflineEvaluationControlController app;
	private TemplateDropdown<MultiKPIEvalConfiguration> configDrop;
	
	public KPIPageLastEvals(WidgetPage<?> page, OfflineEvaluationControlController app, EvalResultManagement evalResultMan) {
		super(page, app.appMan, evalResultMan.getEvalScheduler(),
				Arrays.asList(KPIPageGWOverview.INTERVALS_OFFERED),
				3, true, EvalResultManagementStd.STANDARD_MULTIEVAL_INTERVAL_STEP, false, null, true);
		if(scheduler == null) throw new IllegalStateException("We need an implementation with scheduler here!");
		//this.app = app;
		retardationOnGET = 1000;
	}

	@Override
	public Collection<KPIResultType> getObjectsInTable(OgemaHttpRequest req) {
		MultiKPIEvalConfiguration entrRes = configDrop.getSelectedItem(req);
		if(entrRes == null) return Collections.emptyList();
		
		List<KPIResultType> result = new ArrayList<>();
		List<KPIStatisticsManagementI> evalsP = getKPIManagement(entrRes);
		for(KPIStatisticsManagementI ksm: evalsP) {
			result.add(new KPIResultType(ksm));
		}
		return result;
	}

	private List<KPIStatisticsManagementI> getKPIManagement(MultiKPIEvalConfiguration startConfig) {
		String providerId = startConfig.evaluationProviderId().getValue();
		GaRoSingleEvalProvider eval = scheduler.getProvider(providerId);
		return scheduler.configureKPIManagement(startConfig, eval );
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		configDrop = new TemplateDropdown<MultiKPIEvalConfiguration>(page, "configDrop") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				List<MultiKPIEvalConfiguration> items = scheduler.getConfigs(null);
				update(items, req);
			}
		};
		configDrop.setTemplate(new DefaultDisplayTemplate<MultiKPIEvalConfiguration>() {
			@Override
			public String getLabel(MultiKPIEvalConfiguration object, OgemaLocale locale) {
				String sub = object.subConfigId().getValue();
				return object.evaluationProviderId().getValue()+
						(object.subConfigId().isActive() && (!sub.isEmpty())?("-"+sub):"");
			}
		});
		//page.append(configDrop);

		this.dateOfReport = new Label(page, "dateOfReport") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				MultiKPIEvalConfiguration item = configDrop.getSelectedItem(req);
				List<KPIStatisticsManagementI	> evalsP = null;
				if(item != null) evalsP = getKPIManagement(item);
				if(item != null && (!evalsP.isEmpty())) {
					int intervalType = intervalDrop.getSelectedItem(req);
					SampledValue val = evalsP.get(0).getIntervalSchedule(intervalType).getPreviousValue(Long.MAX_VALUE);
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
		triggerOnPost(configDrop, mainTable);

		StaticTable topTable = new StaticTable(1, 2);
		topTable.setContent(0, 0, configDrop).setContent(0, 1, intervalDrop);
		
		this.header = new Header(page, "header", "KPIs of last evaluations started");
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
