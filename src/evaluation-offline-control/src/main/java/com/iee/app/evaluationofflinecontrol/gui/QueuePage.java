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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.util.evalcontrol.EvalScheduler;
import org.ogema.util.evalcontrol.EvalScheduler.MultiEvalRunI;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;

import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;

public class QueuePage extends ObjectGUITablePage<MultiEvalRunI, Resource> {
	public static final Integer[] INTERVALS_OFFERED =
			new Integer[]{AbsoluteTiming.DAY, AbsoluteTiming.WEEK, AbsoluteTiming.MONTH};
	//private final OfflineEvaluationControlController app;
	private final EvalScheduler scheduler;
	
	public QueuePage(WidgetPage<?> page, OfflineEvaluationControlController app, EvalResultManagement evalResultMan) {
		super(page, app.appMan, null);
		scheduler = app.serviceAccess.evalResultMan().getEvalScheduler();
		if(scheduler == null) throw new IllegalStateException("We need an implementation with scheduler here!");
		//this.app = app;
		retardationOnGET = 1000;
	}

	@Override
	public void addWidgets(MultiEvalRunI object, ObjectResourceGUIHelper<MultiEvalRunI, Resource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if(req != null) {
			vh.stringLabel("Eval Provider", id, object.getConfig().evaluationProviderId().getValue(), row);
			vh.stringLabel("SubId", id, object.getConfig().subConfigId().getValue(), row);
			vh.timeLabel("Start", id, object.getStartTime(), row, 0);
			vh.timeLabel("End", id, object.getEndTime(), row, 0);
			vh.stringLabel("Save JSON", id, object.getSaveJsonResult()?"yes":"no", row);
			vh.stringLabel("GW#", id, ""+object.getConfig().gwIds().getValues().length, row);
		} else {
			vh.registerHeaderEntry("Eval Provider");
			vh.registerHeaderEntry("SubId");
			vh.registerHeaderEntry("Start");
			vh.registerHeaderEntry("End");
			vh.registerHeaderEntry("Save JSON");
			vh.registerHeaderEntry("GW#");
		}
	}

	@Override
	public Resource getResource(MultiEvalRunI object, OgemaHttpRequest req) {
		throw new IllegalStateException("Resource not supported");
	}

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "header", "Queued Evaluation Run Overview");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
	}

	@Override
	public Collection<MultiEvalRunI> getObjectsInTable(OgemaHttpRequest req) {
		MultiEvalRunI current = scheduler.getElementCurrentlyExecuted();
		if(current == null) return Collections.emptyList();
		List<MultiEvalRunI> result = new ArrayList<>();
		result.add(current);
		result.addAll(scheduler.getQueueElements());
		return result;
	}

}
