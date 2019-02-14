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

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;

import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.popup.Popup;

public class ResultTypesPopup extends Popup {

	private static final long serialVersionUID = 1L;
	private final StaticTable table;
	private final Label resultType;
	private final Label descLabel;
	
	private String resType;
	private String descType;

	public ResultTypesPopup(WidgetPage<?> page, String id, boolean globalWidget, OfflineEvaluationControlController app) {
		super(page, id, globalWidget);
		
		resultType = new Label(page, "resultTypes") {
			
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				setText(resType, req);
			}
		};
		descLabel = new Label(page, "descLabel") {
			
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				setText(descType, req);
			}
		};
		
		setDefaultTitle("Result Types");
		setDefaultFooterHTML("Offline Evaluation Control");

		table = new StaticTable(1,3);
		
		table.setContent(0, 0, "Result Types");
		table.setContent(0, 1, resultType);
		table.setContent(0, 2, descLabel);
		PageSnippet snippet = new PageSnippet(page, "snippet_"+id, true);
		snippet.append(table, null);
		this.setBody(snippet, null);
		
	}
	
	
	//TODO: This is not session-safe!
	public void showResultTypes(GaRoSingleEvalProvider eval, boolean useDescription,
			OgemaLocale locale) {
		//this.resType = null;
		String resType = null;
		int i = 0;
		while(i < eval.resultTypes().size()) {
			if(resType == null)
				resType = " ";
			else resType += ", ";
			if(!useDescription)
				resType += eval.resultTypes().get(i).id(); //"\n";
			else
				resType += "\""+eval.resultTypes().get(i).description(locale)+"\""; //"\n";
			i++;
		}
		if(useDescription) descType = resType;
		else this.resType = resType;
	}
	
	void triggerResult(final OgemaWidget widget) {
		widget.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		this.triggerAction(this, TriggeringAction.GET_REQUEST, TriggeredAction.SHOW_WIDGET);
		this.triggerAction(resultType, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST); 
		this.triggerAction(descLabel, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST); 
	}

}
