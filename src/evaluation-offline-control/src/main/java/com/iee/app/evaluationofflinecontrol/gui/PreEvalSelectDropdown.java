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
import java.util.ArrayList;
import java.util.List;

import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.template.DisplayTemplate;

public class PreEvalSelectDropdown extends TemplateDropdown<File> {
	public static final String FILE_PATH = System.getProperty("de.iwes.tools.timeseries-multieval.resultpath", "../evaluationresults");


	private static final long serialVersionUID = 1L;
	//final private List<String> jsonList;
	final TemplateDropdown<GaRoSingleEvalProvider> selectProvider;
	final Label selectPreEvalCount;
	final int preEvalIdx;
	
	public PreEvalSelectDropdown(WidgetPage<?> page, String id, //List<String> jsonList,
			final TemplateDropdown<GaRoSingleEvalProvider> selectProvider,
			final Label selectPreEvalCount, int preEvalIdx) {
		super(page, id);
		//this.jsonList = jsonList;
		this.selectProvider = selectProvider;
		this.selectPreEvalCount = selectPreEvalCount;
		this.preEvalIdx = preEvalIdx;
		setTemplate(new DisplayTemplate<File>() {
			
			@Override
			public String getLabel(File object, OgemaLocale locale) {
				return object.getName();
			}
			
			@Override
			public String getId(File object) {
				return object.getAbsolutePath();
			}
		});
	}
		
	private List<File> searchDir(String directory, GaRoSingleEvalProviderPreEvalRequesting gaRoEval) {
		File folder = new File(directory);
		File[] allFiles = folder.listFiles();
		List<File> result = new ArrayList<>();
		if(allFiles != null) for(int i = 0; i < allFiles.length; i++) {
			if(allFiles[i].isFile() && allFiles[i].getName()
					.contains(gaRoEval.preEvaluationsRequested().get(preEvalIdx).getSourceProvider())) {
				
				result .add(allFiles[i]);
			}
		}
		return result;
	}
	
	public void onGETRemote(boolean autoPreEval, OgemaHttpRequest req) {
		//int count = 0;
		List<File> jsonList = new ArrayList<>();
		//jsonList.clear();
		String preEval1 = "";
		//File folder = new File(OfflineEvaluationControl.FILE_PATH);
		//File[] allFiles = folder.listFiles();
		
		GaRoSingleEvalProvider eval =  selectProvider.getSelectedItem(req);
		boolean used = false;
		if (eval instanceof GaRoSingleEvalProviderPreEvalRequesting && (!autoPreEval)) {
			GaRoSingleEvalProviderPreEvalRequesting gaRoEval = (GaRoSingleEvalProviderPreEvalRequesting)eval;
			if((gaRoEval.preEvaluationsRequested() != null) && gaRoEval.preEvaluationsRequested().size() > preEvalIdx) {
				preEval1 = gaRoEval.preEvaluationsRequested().get(preEvalIdx).getSourceProvider();
				jsonList.addAll(searchDir(FILE_PATH, gaRoEval));
				jsonList.addAll(searchDir(FILE_PATH+"/DefaultWS/experiment", gaRoEval));
				jsonList.addAll(searchDir(FILE_PATH+"/DefaultWS/major", gaRoEval));
				/*for(int i = 0; i < allFiles.length; i++) {
					if(allFiles[i].isFile() && allFiles[i].getName()
							.contains(gaRoEval.preEvaluationsRequested().get(preEvalIdx).getSourceProvider())) {
						
						jsonList.add(allFiles[i].getName());
						//count += 1;
					}
				}*/
			
				update(jsonList, req);
				selectPreEvalCount.setText("PreEval"+preEvalIdx+": ("+jsonList.size()+") "+preEval1, req);
				setWidgetVisibility(true, req);
				used = true;
			}
		}
		if(!used) {
			jsonList.clear();
			preEval1 = "";
			//count = 0;
			update(jsonList, req);
			//selectPreEvalCount.setText("--"+preEval1, req);
			setWidgetVisibility(false, req);
		}
	}
}
