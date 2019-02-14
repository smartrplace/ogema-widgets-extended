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
import java.util.List;

import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.util.directresourcegui.kpi.KPIMonitoringReport;
import org.ogema.util.directresourcegui.kpi.KPIResultType;
import org.ogema.util.evalcontrol.EvalScheduler.OverwriteMode;
import org.ogema.util.jsonresult.management.EvalResultManagementStd;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;
import com.iee.app.evaluationofflinecontrol.config.ProviderEvalOfflineConfig;

import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class KPIPage extends KPIMonitoringReport {
	public static final Integer[] INTERVALS_OFFERED =
			new Integer[]{AbsoluteTiming.DAY, AbsoluteTiming.WEEK, AbsoluteTiming.MONTH};
	public static final List<Integer> INTERVALS_OFFERED_WSHORT =
			Arrays.asList(new Integer[]{AbsoluteTiming.DAY, AbsoluteTiming.WEEK, AbsoluteTiming.MONTH,
					AbsoluteTiming.HOUR, AbsoluteTiming.MINUTE});
	private final OfflineEvaluationControlController app;
	
	public KPIPage(WidgetPage<?> page, OfflineEvaluationControlController app, EvalResultManagement evalResultMan) {
		super(page, app.appMan, evalResultMan.getEvalScheduler(),
				Arrays.asList(INTERVALS_OFFERED),
				3, true, EvalResultManagementStd.STANDARD_MULTIEVAL_INTERVAL_STEP);
		if(scheduler == null) throw new IllegalStateException("We need an implementation with scheduler here!");
		this.app = app;
		retardationOnGET = 1000;
	}

	@Override
	protected String getHeaderText() {
		return "Auto-Scheduling KPIs";
	}
	
	@Override
	public Collection<KPIResultType> getObjectsInTable(OgemaHttpRequest req) {
		List<KPIResultType> result = new ArrayList<>();
		for(ProviderEvalOfflineConfig known: app.appConfigData.knownProviders().getAllElements()) {
			if(known.includeIntoStandardEval().getValue()) {
				List<KPIStatisticsManagementI> evalsP = scheduler.configureKPIManagement(known.providerId().getValue());
				for(KPIStatisticsManagementI ksm: evalsP) {
					result.add(new KPIResultType(ksm));
				}
			}
		}
		return result;
	}

	@Override
	protected void updateAll(OgemaHttpRequest req) {
		String alertTxt = null;
		int count = 0;
		int intervalType = intervalDrop.getSelectedItem(req);
		long alignedNow = AbsoluteTimeHelper.getIntervalStart(currentTime, intervalType);
		long startTime = AbsoluteTimeHelper.addIntervalsFromAlignedTime(alignedNow, -pastColumnNum, intervalType);
		for(ProviderEvalOfflineConfig known: app.appConfigData.knownProviders().getAllElements()) {
			if(known.includeIntoStandardEval().getValue()) {
				MultiKPIEvalConfiguration config = scheduler.getConfig(known.providerId().getValue(), null);
				if(config != null) {
					scheduler.queueEvalConfig(config, config.saveJsonResults().getValue(), null,
							startTime, alignedNow, app.getDataProvidersToUse(), true, OverwriteMode.ONLY_PROVIDER_REQUESTED, false);
					count++;
				}
				else if(alertTxt == null) alertTxt = "Could not process "+known.providerId().getValue();
				else alertTxt += ", "+known.providerId().getValue();
			}
		}
		if(alertTxt == null) alertTxt = "Finished "+count+" evaluations";
		alert.showAlert(alertTxt, alertTxt.startsWith("Finished "), Long.MAX_VALUE, req);
	}
}
