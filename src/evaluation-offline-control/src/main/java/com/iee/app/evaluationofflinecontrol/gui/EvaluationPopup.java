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

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.configuration.Configuration;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting.PreEvaluationRequested;
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

public class EvaluationPopup extends Popup{
	
	private static final long serialVersionUID = -6117920971874007816L;
	private final StaticTable table;
	public Label name;
	public Label resultType;
	public Label config;
	public Label preEvalLabel;
	public Label extendedResultLabel;
	public Label intervalAggregationLabel;
	
	private String providerID = "";
	private String resType;
	private String extraConfig;
	private String preEval = "";
	private String extendedResult;
	private String intervalAggregation;
	final OfflineEvaluationControlController controller;
	public EvaluationPopup(WidgetPage<?> page, String id, boolean globalWidget, final OfflineEvaluationControlController app) {
		
		super(page, id, globalWidget);
		this.controller = app;
		
		name = new Label(page,"name_"+id) {

			private static final long serialVersionUID = -7462505753627728568L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				setText(providerID, req);
			}
		};
		
		resultType = new Label(page, "resultType") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				
				setText(resType, req);
			}
			
		};
		
		config = new Label(page, "configLabel") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				setText(extraConfig, req);
			}
			
		};
		
		preEvalLabel = new Label(page, "preEvalLabel") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				setText(preEval, req);
			}
			
		};

		extendedResultLabel = new Label(page, "extendedResultLabel") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				
				setText(extendedResult, req);
			}
			
		};
		
		intervalAggregationLabel = new Label(page, "intervalAggregationLabel") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				
				setText(intervalAggregation, req);
			}
			
		};
		
		setDefaultTitle("Details");
		setDefaultFooterHTML("Offline Evaluation Control");
		
		table = new StaticTable(6,2);
		
		table.setContent(0, 0, "Name/ID").setContent(0, 1, name);
		table.setContent(1, 0, "Result Types").setContent(1, 1, resultType);
		table.setContent(2, 0, "Extra Config's").setContent(2, 1, config);
		table.setContent(3, 0, "PreEvals Requested").setContent(3, 1, preEvalLabel);
		table.setContent(4, 0, "Extended Result").setContent(4, 1, extendedResultLabel);
		table.setContent(5, 0, "Internal Aggregation").setContent(5, 1, intervalAggregationLabel);
		
		PageSnippet snippet = new PageSnippet(page, "snippet_"+id, true);
		snippet.append(table, null);
		this.setBody(snippet, null);
	}

	public void updateTableContent(GaRoSingleEvalProvider eval) {
		this.providerID = eval.id();
		this.resType = " ";
		this.extraConfig=" ";
		this.preEval =" ";
		this.extendedResult = "";
		this.intervalAggregation = " ";
		
		for(ResultType rt : eval.resultTypes()) 
			this.resType += rt.label(OgemaLocale.ENGLISH)+"\n";
		
		for(Configuration<?> config : eval.getConfigurations()) 
			this.extraConfig += config.label(OgemaLocale.ENGLISH)+"\n";
		
		if (eval instanceof GaRoSingleEvalProviderPreEvalRequesting) {
			GaRoSingleEvalProviderPreEvalRequesting gaRoEval = (GaRoSingleEvalProviderPreEvalRequesting)eval;
			for(ResultType rt : eval.resultTypes())
				this.intervalAggregation += gaRoEval.getResultAggregationMode(rt).name()+"\n";
			
			if(gaRoEval.preEvaluationsRequested() != null) for(PreEvaluationRequested preEvalRequested : gaRoEval.preEvaluationsRequested())
				this.preEval += preEvalRequested.getSourceProvider()+"\n";
		}
		
		if(eval.extendedResultDefinition() != null) {
			this.extendedResult += eval.extendedResultDefinition().getSimpleName()+"\n";
		}else {
			this.extendedResult += "no";
		}	
	}
	
	void trigger(final OgemaWidget widget) {
		widget.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		this.triggerAction(this, TriggeringAction.GET_REQUEST, TriggeredAction.SHOW_WIDGET);
		this.triggerAction(name, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST); 
		this.triggerAction(resultType, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST); 
		this.triggerAction(config, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST); 
		this.triggerAction(preEvalLabel, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST); 
		this.triggerAction(extendedResultLabel, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST); 
		this.triggerAction(intervalAggregationLabel, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST); 
	}
}
