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

import java.util.List;

import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;
import com.iee.app.evaluationofflinecontrol.config.KPIPageConfig;
import com.iee.app.evaluationofflinecontrol.config.ResultToShow;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;

/** This KPI page allows to select a {@link KPIPageConfig} and show the available KPIs for the
 * last setting of the configuration meaning the last start of the respective EvaluationProvider.
 * 
 * This is almost the same page as {@link KPIShowConfigurationPage} but provides a label
 * containing the gateways as additional information.
 * 
 * @author dnestle
 *
 */
public class KPIShowAlgoAssessmentPage extends KPIShowConfigurationPage {

	public KPIShowAlgoAssessmentPage(WidgetPage<?> page, OfflineEvaluationControlController app, EvalResultManagement evalResultMan) {
		super(page, app, evalResultMan);
	}

	@Override
	protected Header getHeader(WidgetPage<?> page) {
		return new Header(page, "header", "KPIs of special configurations plus");		
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		
		StaticTable configTopTable = new StaticTable(1, 4);
		Label gwLabel = new Label(page, "gwLabel") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				KPIPageConfig cfg = configDrop.getSelectedItem(req);
				if(cfg == null) {
					setText("N/A", req);
					return;					
				}
				List<ResultToShow> els = cfg.resultsToShow().getAllElements();
				if(els.isEmpty()) {
					setText("n/a", req);
					return;
				}
				MultiKPIEvalConfiguration config = els.get(0).evalConfiguration();
				String text = getStringArrayAsString(config.gwIds().getValues(), 3);
				setText(text, req);
			}
		};
		configTopTable.setContent(0, 1, "Gateways").setContent(0, 2, gwLabel);
		page.append(configTopTable);
		
		triggerOnPost(configDrop, gwLabel);
	}
	
	/** Convert String array to a single string containing a comma-separated list
	 * of the entries of the String array
	 * TODO: Move this to util package or find existing replacement
	 * @param arr
	 * @param limitElsToPrint
	 * @return
	 */
	public String getStringArrayAsString(String[] arr, Integer limitElsToPrint) {
		if(limitElsToPrint != null && (arr.length > limitElsToPrint)) {
			return String.format("%d", arr.length);
		}
		String resType = null;
		int i = 0;
		while(i < arr.length) {
			if(resType == null)
				resType = " ";
			else resType += ", ";
			resType += arr[i]; //"\n";
			i++;
		}
		return resType;
	}

}
