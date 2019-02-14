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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.model.jsonresult.JSONResultFileData;
import org.ogema.model.jsonresult.JsonOGEMAFileData;
import org.ogema.model.jsonresult.JsonOGEMAWorkspaceData;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper.LongProvider;
import org.smartrplace.util.directresourcegui.DeleteButton;
import org.smartrplace.util.directresourcegui.LabelLongValue;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;
import org.smartrplace.util.directresourcegui.ResourceGUITablePage;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;

import de.iwes.timeseries.eval.api.extended.MultiResult;
import de.iwes.timeseries.eval.api.extended.util.AbstractSuperMultiResult;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.Linebreak;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.template.DisplayTemplate;

public class JSONResultPage extends ResourceGUITablePage<JSONResultFileData> {
	private final OfflineEvaluationControlController controller;
	private final EvalResultManagement evalResultMan;
	
	private Header header;
	
	//We are using the providerId here as the provider class or object may not be
	//accessible
	private TemplateDropdown<String> providerIdDrop;
	private TemplateDropdown<JsonOGEMAWorkspaceData> workspaceDrop;

	public JSONResultPage(WidgetPage<?> page, OfflineEvaluationControlController controller) {
		super(page, controller.appMan,  JSONResultFileData.class);
		this.controller = controller;
		this.evalResultMan = controller.serviceAccess.evalResultMan();
		this.retardationOnGET = 2000;
		
		Button updateButton = new Button(page, "updateButtonJS", "update all KPIs for default subConfigId") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				for(JSONResultFileData object: getResourcesInTable(req)) {
					/*JsonOGEMAWorkspaceData ws = workspaceDrop.getSelectedItem(req);
					String filePath = evalResultMan.getFilePath(ws.getName(), object.status().getValue(), object.workSpaceRelativePath().getValue());
					File file = new File(filePath);*/
					String evalId = object.evaluationProviderId().getValue();
					GaRoSingleEvalProvider eval = evalResultMan.getEvalScheduler().getProvider(evalId);
					if(eval == null) {
						System.out.println("Warning: Evaluation provider "+object.evaluationProviderId().getValue()+" not found!");
						continue;
					}
					int baseInterval = AbsoluteTimeHelper.getIntervalTypeFromStandardInterval(object.stepSize().getValue());
					int[] additionalIntervalTypes = MainPage.getKPIPageIntervals();
					MultiKPIEvalConfiguration evalConfig = evalResultMan.getEvalScheduler().getOrCreateConfig(
							evalId, null, baseInterval, additionalIntervalTypes, false);
					AbstractSuperMultiResult<MultiResult> result = evalResultMan.importFromJSON(object);
					//List<KPIStatisticsManagement> kpiUtils =
					List<KPIStatisticsManagementI> kpis = evalResultMan.getEvalScheduler().calculateKPIs(
							evalConfig, eval, result , baseInterval , null, null);
					System.out.println("Created/updated KPIs for file "+evalResultMan.getFilePathInCurrentWorkspace(object)+
							" baseInterval:"+baseInterval+" #kpis:"+kpis.size());
				}
			}
		};
		page.append(updateButton);
	}

	@Override
	public void addWidgets(JSONResultFileData object, ResourceGUIHelper<JSONResultFileData> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {

		vh.stringLabel("Path", id, object.workSpaceRelativePath(), row);
		vh.stringLabel("Status", id, ""+object.status().getValue(), row);
		vh.stringLabel("GW#", id, getNumString(object.gatewaysIncluded()), row);
		vh.stringLabel("Class Name", id, ""+getSimpleName(object.resultClassName().getValue()), row);
		vh.timeLabel("Start", id, object.startTime(), row, 0);
		vh.timeLabel("End", id, object.endTime(), row, 0);
		vh.timeLabel("Stepsize", id, object.stepSize(), row, 1);
		vh.stringLabel("Interval#", id, ""+object.timeIntervalNum().getValue(), row);
		vh.stringLabel("PreEvals1", id, getArrayString(object.preEvaluationFilesUsed(), true), row);
		vh.stringLabel("PreEvals2", id, getArrayString(object.preEvaluationsUsed().getAllElements(), true), row);
		if(req != null) {
			JsonOGEMAWorkspaceData ws = workspaceDrop.getSelectedItem(req);
			String filePath = evalResultMan.getFilePath(ws.getName(), object.status().getValue(), object.workSpaceRelativePath().getValue());
			File file = new File(filePath);
			vh.fileSizeLabel("Size", id, null, row, new LongProvider() {

				@Override
				public LabelLongValue getValue(OgemaHttpRequest req) {
					return new LabelLongValue(file.length());
				}
				
			});
			vh.timeLabel("Created", id, file.lastModified(), row, 0);
			DeleteButton<JSONResultFileData> deleteButton = new DeleteButton<JSONResultFileData>(null, object, mainTable, id, alert, row, vh, req) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					if(file.isFile()) file.delete();
					object.delete();
				}
			};
			row.addCell("delete", deleteButton);
		} else {
			vh.registerHeaderEntry("Size");
			vh.registerHeaderEntry("Created");
			vh.registerHeaderEntry("delete");
		}
		//GUIHelperExtension.addDeleteButton(null, object, mainTable, id, alert, row, vh, req);
	}
	
	public static String getSimpleName(String className) {
		if(className == null) return null;
		String[] els = className.split("\\.");
		return els[els.length-1];
	}
	
	public static String getArrayString(StringArrayResource arrRes, boolean addNum) {
		if(arrRes.getValues().length == 0) return (addNum?getNumString(arrRes):"");
		String result = null;
		for(String s: arrRes.getValues()) {
			if(result == null) result = s;
			else result += ", "+s;
		}
		return (addNum?getNumString(arrRes)+" : ":"")+result;
	}

	public static String getArrayString(List<JSONResultFileData> arrRes, boolean addNum) {
		if(arrRes.isEmpty()) return (addNum?getNumString(arrRes):"");
		String result = null;
		for(JSONResultFileData s: arrRes) {
			if(result == null) result = s.workSpaceRelativePath().getValue();
			else result += ", "+s.workSpaceRelativePath().getValue();
		}
		return (addNum?getNumString(arrRes)+" : ":"")+result;
	}

	public static String getNumString(StringArrayResource arrRes) {
		return "El#:"+arrRes.getValues().length;
	}
	public static String getNumString(List<?> arrRes) {
		return "El#:"+arrRes.size();
	}

	@Override
	public void addWidgetsAboveTable() {
		this.workspaceDrop = new TemplateDropdown<JsonOGEMAWorkspaceData>(page, "workspaceDrop") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				List<JsonOGEMAWorkspaceData> wsList = evalResultMan.getWorkspaces();
				update(wsList, req);
				if(!wsList.isEmpty()) selectItem(wsList.get(0), req);
			}			
		};
		workspaceDrop.setTemplate(new DisplayTemplate<JsonOGEMAWorkspaceData>() {

			@Override
			public String getId(JsonOGEMAWorkspaceData object) {
				return object.getLocation();
			}

			@Override
			public String getLabel(JsonOGEMAWorkspaceData object, OgemaLocale locale) {
				return object.getName()+"("+object.fileData().size()+")";
			}
		});
		this.providerIdDrop = new TemplateDropdown<String>(page, "singleTimeInterval") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				JsonOGEMAWorkspaceData ws = workspaceDrop.getSelectedItem(req);
				Set<String> result = new HashSet<>();
				result.add("All");
				for(JsonOGEMAFileData data: ws.fileData().getAllElements()) {
					result.add(data.evaluationProviderId().getValue());
				}
				update(result, req);
			}
		};
		providerIdDrop.setTemplate(new DisplayTemplate<String>() {

			@Override
			public String getId(String object) {
				return object;
			}

			@Override
			public String getLabel(String object, OgemaLocale locale) {
				GaRoSingleEvalProvider prov = controller.serviceAccess.evalResultMan().getEvalScheduler().getProvider(object);
				if(prov == null) return object;
				else return prov.label(locale);
			}
		});
		triggerOnPost(workspaceDrop, providerIdDrop);
		triggerOnPost(providerIdDrop, mainTable);
		
		this.header = new Header(page, "header", "JSON Result Descriptor Overview");
		page.append(header);
		page.append(Linebreak.getInstance());
		page.append(workspaceDrop);
		page.append(providerIdDrop);
	}

	@Override
	public List<JSONResultFileData> getResourcesInTable(OgemaHttpRequest req) {
		JsonOGEMAWorkspaceData ws = workspaceDrop.getSelectedItem(req);
		String providerId = providerIdDrop.getSelectedItem(req);
		List<JSONResultFileData> result = new ArrayList<>();
		try {
		//TODO: ws should never be null as it is set in upper widget dependency. It has no negative effect
		//at the moment, though
		if(ws != null) for(JsonOGEMAFileData data: ws.fileData().getAllElements()) {
			if(providerId.equals("All") || data.evaluationProviderId().getValue().equals(providerId) && (data instanceof JSONResultFileData))
				result.add((JSONResultFileData) data);
		}
		//TODO
		} catch(NullPointerException e) {
			e.printStackTrace();
		}
		return result;
	}
}
