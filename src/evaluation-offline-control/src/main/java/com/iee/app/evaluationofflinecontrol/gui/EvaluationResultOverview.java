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
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;

import de.iwes.timeseries.eval.api.extended.util.MultiEvaluationUtils;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.label.Header;

@Deprecated
/**
 * @deprecated refers to old standard directory. Use JSONResultPage instead.
 */
public class EvaluationResultOverview {
	public final long UPDATE_RATE = 5*1000;
	//private final WidgetPage<?> page; 
	private final OfflineEvaluationControlController controller;
	private final ResultTypesPopup resultTypePopup;
	private final DynamicTable<GaRoSingleEvalProvider> table;
	private final String FILE_PATH = System.getProperty("de.iwes.tools.timeseries-multieval.resultpath", "../evaluationresults");
	private int id = 0;
	public EvaluationResultOverview(final WidgetPage<?> page, final OfflineEvaluationControlController app) {
		
		//this.page = page;
		this.controller = app;

		final Collection<GaRoSingleEvalProvider> evalprovider = controller.serviceAccess.getEvaluations().values()
				.stream().filter(provider -> provider instanceof GaRoSingleEvalProvider)
				.map(provider -> (GaRoSingleEvalProvider) provider)
				.collect(Collectors.toList());	//controller.serviceAccess.getEvaluations().values();
		
		Header header = new Header(page, "header", "Evaluation Result Overview");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
		
		resultTypePopup = new ResultTypesPopup(page, "resTypePopup"+id++, true, app);
		table = new DynamicTable<GaRoSingleEvalProvider>(page, "resultoverview") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				updateRows(evalprovider, req);
			}
		};
		
		table.setRowTemplate(new RowTemplate<GaRoSingleEvalProvider>() {

			@Override
			public Row addRow(GaRoSingleEvalProvider eval, OgemaHttpRequest req) {
				id++;
				Row row = new Row();
				return addBasicEvalProvider(row, eval);
			}

			private Row addBasicEvalProvider(Row row, GaRoSingleEvalProvider eval) {
				
				String providerName = "";
				String fileName = "";
				String fileTime ="";
				String startTime = "";
				String endTime = "";
				String resTypes = "";
				String gws = "";
				Button showResultTypes = null;
				
				SimpleDateFormat date = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
				fileName = eval.getClass().getSimpleName()+"Result.json";
				File file = new File (FILE_PATH+"/"+fileName);
				Path path = Paths.get(FILE_PATH+"/"+fileName);
				
				if(Files.isRegularFile(path)) {
					GaRoSuperEvalResultDeser evalGaro = 
							MultiEvaluationUtils.importFromJSON(file.toString(), GaRoSuperEvalResultDeser.class);
					
					providerName = eval.getClass().getSimpleName();
					startTime = date.format(evalGaro.getStartTime());
					endTime = date.format(evalGaro.getEndTime());
					fileTime = date.format(file.lastModified());
					
					if(evalGaro.intervalResults != null)
						gws = Integer.toString(evalGaro.intervalResults.size());
					else
						gws = "0";
						
					resTypes = Integer.toString(eval.resultTypes().size());
					showResultTypes = new Button(page, "showResultTypes"+id++, "Show Res. Types") {
						private static final long serialVersionUID = 1L;
						
						public void onPrePOST(String data, OgemaHttpRequest req) {
								resultTypePopup.showResultTypes(eval, false, req.getLocale());
								resultTypePopup.showResultTypes(eval, true, req.getLocale());
						};
					};
						
					resultTypePopup.triggerResult(showResultTypes);
					showResultTypes.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);
						
					row.addCell("name", providerName);
					row.addCell("file", fileName);
					row.addCell("resulttypes", resTypes);
					row.addCell("showresulttypes", showResultTypes);
					row.addCell("filetime", fileTime);
					row.addCell("start", startTime);
					row.addCell("end", endTime);
					row.addCell("gws", gws);
						
					return row;
				}
				return null;
			}

			@Override
			public String getLineId(GaRoSingleEvalProvider object) {
				return object.id();
			}

			@Override
			public Map<String, Object> getHeader() {
				final Map<String, Object> header = new LinkedHashMap<>();
				header.put("name", "Provider Name");
				header.put("file", "File");
				header.put("resulttypes", "Result Types");
				header.put("showresulttypes", "Show");
				header.put("filetime", "File Time");
				header.put("start", "Start");
				header.put("end", "End");
				header.put("gws", "Timesteps");
				return header;
			}
			
		});
		
		page.append(table);
		page.append(resultTypePopup);
		
	}

}
