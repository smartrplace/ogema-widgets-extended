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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.util.evalcontrol.EvalScheduler;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;

import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting;
import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;


/**
 * An HTML page, generated from the Java code.
 */
public class MainPage {
	
	public final long UPDATE_RATE = 5*1000;
	private final EvaluationPopup popup;
	private final ResultTypesPopup resTypePopup;
	private final WidgetPage<?> page; 
	private final OfflineEvaluationControlController controller;
	private final DynamicTable<GaRoSingleEvalProvider> table;
	public DynamicTable<GaRoSingleEvalProvider> popupTable; 
	public int buttonID = 0;
	public  int preevalsID = 0;

	public MainPage(final WidgetPage<?> page, final OfflineEvaluationControlController app) {
		
		this.page = page;
		this.controller = app;
		getHeader();
		
		popup = new EvaluationPopup(page, "popup"+preevalsID++, true, app); 
		resTypePopup = new ResultTypesPopup(page, "resTypePopup"+preevalsID++, true, app);
		
		table = new DynamicTable<GaRoSingleEvalProvider>(page, "evalviewtable") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				Collection<GaRoSingleEvalProvider> providers = controller.serviceAccess.getEvaluations().values()
						.stream().filter(provider -> provider instanceof GaRoSingleEvalProvider)
						.map(provider -> (GaRoSingleEvalProvider) provider)
						.collect(Collectors.toList());
				updateRows(providers, req);
				
			}
		};
		
		table.setRowTemplate(new RowTemplate<GaRoSingleEvalProvider>() {

			@Override
			public Row addRow(GaRoSingleEvalProvider eval, OgemaHttpRequest req) {
				buttonID++;
				Row row = new Row();
				return addBasicEvalProvider(row, eval, req);
			}

			private Row addBasicEvalProvider(Row row, final GaRoSingleEvalProvider eval, OgemaHttpRequest req) {
				String name = eval.id();
				String description = eval.description(OgemaLocale.ENGLISH);
				String resType = "";
				if(eval.resultTypes() != null)
					resType = Integer.toString(eval.resultTypes().size());
				Button showResultTypes, showpreevals, details;
				RedirectButton configpage;
				BooleanResourceCheckbox autoEvalCheck;
				String extraConfig = Integer.toString(eval.getConfigurations().size());
				String extendedResult = "";
				String roomtypes = "";
				String preevalsequested = "";
				
				row.addCell("name", name);
				row.addCell("description", description);
				row.addCell("resulttypes", resType);
				row.addCell("extraconfig", extraConfig);
					
				showResultTypes = new Button(table, "id"+buttonID, "Show Res. Types", req) {
					private static final long serialVersionUID = 1L;

						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							resTypePopup.showResultTypes(eval, true, req.getLocale());									
							resTypePopup.showResultTypes(eval, false, req.getLocale());
						}
				};
				
				resTypePopup.triggerResult(showResultTypes);
				row.addCell("showresulttypes", showResultTypes);
				showResultTypes.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);

				if(eval.extendedResultDefinition() != null) {
					extendedResult = "yes";
					row.addCell("extendedresult", extendedResult);
				}else {
					extendedResult = "no";
					row.addCell("extendedresult", extendedResult);
				}
				
				if(eval.getRoomTypes() == null) {
					roomtypes ="all";
					row.addCell("roomtypes", roomtypes);
				}else {
					roomtypes = Integer.toString(eval.getRoomTypes().length);
					row.addCell("roomtypes", roomtypes);
				}
				
				if(eval instanceof GaRoSingleEvalProviderPreEvalRequesting) {
					GaRoSingleEvalProviderPreEvalRequesting garoPre = (GaRoSingleEvalProviderPreEvalRequesting)eval;
	
					if(garoPre.preEvaluationsRequested() != null)
						preevalsequested = Integer.toString(garoPre.preEvaluationsRequested().size());
					else
						preevalsequested = "0";
					row.addCell("preevalsequested", preevalsequested);
					
				}else {
					row.addCell("preevalsequested", "0");
				}
								
				showpreevals = new Button(table, "idpreevals"+buttonID, "Show PreEvals", req) {

					private static final long serialVersionUID = 1L;
					
					@Override
					public void onPrePOST(String data, OgemaHttpRequest req) {

						super.onPrePOST(data, req);
					}
				};
				
				row.addCell("showpreevals", showpreevals);

				autoEvalCheck = new BooleanResourceCheckbox(table, "autoEvalCheck"+buttonID, "", req) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onGET(OgemaHttpRequest req) {
						MultiKPIEvalConfiguration config = controller.getAutoEvalConfig(eval.id());
						if(config == null) {
							disable(req);
						} else {
							enable(req);
							selectItem(config.performAutoQueuing(), req);
						}
					}
				};

				/*BooleanResource autoEvalselected = app.getCreateEvalPersistentData(eval.id()).includeIntoStandardEval();
				autoEvalCheck = new BooleanResourceCheckbox(page, "autoEvalCheck"+buttonID, "") {

					private static final long serialVersionUID = 1L;
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						selectItem(autoEvalselected, req);
					};
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						BooleanResource item = getSelectedItem(req);
						if(item.getValue()) registerProviderForKPI(eval, controller.serviceAccess.evalResultMan());
						else unregisterProviderForKPI(eval, controller.serviceAccess.evalResultMan());
					};
				};*/
								
				row.addCell("autoEval", autoEvalCheck);

				configpage = new RedirectButton(table, "idconfig"+buttonID, "Start", "", req) {

					private static final long serialVersionUID = 1L;
					
					@Override
					public void onPrePOST(String data, OgemaHttpRequest req) {
						
						setUrl("OfflineEvaluationControl.html?configId=" + eval.id(), req);	
					}
				};
								
				row.addCell("configpage", configpage);
				configpage.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);
				
				details = new Button(table, "iddetails"+buttonID++, "Details", req) {

					private static final long serialVersionUID = 1L;
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						
						popup.updateTableContent(eval);
					}
				};
		
				popup.trigger(details); 
				row.addCell("details", details);
				details.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);
				
				return row;
				
			}
			@Override
			public String getLineId(GaRoSingleEvalProvider object) {
				return object.id();
			}

			@Override
			public Map<String, Object> getHeader() {
				final Map<String, Object> header = new LinkedHashMap<>();
				header.put("name", "Name/ID");
				header.put("description", "Description");
				header.put("resulttypes", "Result Types");
				header.put("showresulttypes", "Show");
				header.put("extraconfig", "Extra Config's");
				header.put("extendedresult", "Extended Result");
				header.put("roomtypes", "Room Types");
				header.put("preevalsequested", "PreEvals Requested");
				header.put("autoEval", "Auto-Eval");
				header.put("configpage", "Config Page");
				header.put("details", "Details");
				return header;
			}
			
		});

		page.append(table).linebreak();	
		page.append(popup);
		page.append(resTypePopup);
		
	}
	
	//Header
	private void getHeader() {
		Header header = new Header(page, "header", "Evaluation Provider Overview");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
	}

	public WidgetPage<?> getPage() {
		return page;
	}
	
	public static MultiKPIEvalConfiguration registerProviderForKPI(GaRoSingleEvalProvider eval, EvalResultManagement evalResultMan) {
		EvalScheduler scheduler = evalResultMan.getEvalScheduler();
		if(scheduler == null) throw new IllegalStateException("We need an implementation with scheduler here!");
		
		//TODO: For testing only
		String[] gwIds = new String[] {"_16013"};
		
		int[] intarray = getKPIPageIntervals();
		MultiKPIEvalConfiguration result = scheduler.registerProviderForKPI(eval,
				OfflineEvaluationControlController.APP_EVAL_SUBID, true, intarray , true, gwIds);
		
		return result;
	}
	public static MultiKPIEvalConfiguration unregisterProviderForKPI(GaRoSingleEvalProvider eval, EvalResultManagement evalResultMan) {
		EvalScheduler scheduler = evalResultMan.getEvalScheduler();
		if(scheduler == null) throw new IllegalStateException("We need an implementation with scheduler here(2)!");
		return scheduler.unregisterProviderForKPI(eval, null, true);
	}
	
	public static int[] getKPIPageIntervals() {
		int[] intarray = new int[KPIPage.INTERVALS_OFFERED.length];
		for(int i=0; i<KPIPage.INTERVALS_OFFERED.length; i++) {
			intarray[i] = KPIPage.INTERVALS_OFFERED[i];
		}
		return intarray;
	}
}
