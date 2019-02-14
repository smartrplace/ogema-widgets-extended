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
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;
import com.iee.app.evaluationofflinecontrol.config.KPIPageConfig;
import com.iee.app.evaluationofflinecontrol.util.GatewayConfigData;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.Linebreak;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.resource.widget.init.ResourceInitSingleEmpty;

public class GatewayConfigPage extends ObjectGUITablePage<GatewayConfigData, Resource> {
	private final OfflineEvaluationControlController controller;
	
	private Header header;
	private ResourceInitSingleEmpty<KPIPageConfig> init;

	public GatewayConfigPage(WidgetPage<?> page, OfflineEvaluationControlController controller) {
		super(page, controller.appMan,  new GatewayConfigData(), false);
		this.controller = controller;
		this.retardationOnGET = 2000;
		triggerPageBuild();
	}
	
	private void setSelected(int type, String gw, boolean select, OgemaHttpRequest req) {
		List<String> toUse;
		KPIPageConfig gwsShown = null;
		switch(type) {
		case 0:
			toUse = new ArrayList<>(getGwsToUse(controller));
			break;
		case 1:
			toUse = new ArrayList<>(getGwsToShow(controller, null));
			break;
		case 2:
			gwsShown = init.getSelectedItem(req);
			toUse = new ArrayList<>(getGwsToShow(controller, gwsShown));
			break;
		default:
			throw new IllegalStateException("unknown type:"+type);
		}
		if(select && (!toUse.contains(gw)))
			toUse.add(gw);
		else if((!select) && toUse.contains(gw))
			toUse.remove(gw);
		
		switch(type) {
		case 0:
			ValueResourceHelper.setCreate(controller.appConfigData.gatewaysToUse(), toUse.toArray(new String[0]));
			break;
		case 1:
			ValueResourceHelper.setCreate(controller.appConfigData.gatewaysToShow(), toUse.toArray(new String[0]));
			break;
		case 2:
			ValueResourceHelper.setCreate(gwsShown.gatewaysToShow(), toUse.toArray(new String[0]));
			break;
		}
		
	}
	
	private void addCheckbox(String columnId, String id, int type,
			GatewayConfigData object, ObjectResourceGUIHelper<GatewayConfigData, Resource> vh,
			Row row, OgemaHttpRequest req) {
		if(req == null)
			vh.registerHeaderEntry(columnId);
		else {
			if(type == 2 && object.isShownPageSpec == null) return;
			SimpleCheckbox myField = new SimpleCheckbox(vh.getParent(), columnId+id, "", req) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					switch(type) {
					case 0:
						setValue(object.isUsed,req);
						break;
					case 1:
						setValue(object.isShown,req);
						break;
					case 2:
						setValue(object.isShownPageSpec,req);
						break;
					}
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					boolean val = getValue(req);
					setSelected(type, object.name, val, req);
					switch(type) {
					case 0:
						object.isUsed = val;
						break;
					case 1:
						object.isShown = val;
						break;
					case 2:
						object.isShownPageSpec = val;
						break;
					}
				}
			};
			row.addCell(columnId, myField);
		}
	}
	
	@Override
	public void addWidgets(GatewayConfigData object, ObjectResourceGUIHelper<GatewayConfigData, Resource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {

		vh.stringLabel("Gateway", id, object.name, row);
		addCheckbox("isUsed", id, 0, object, vh, row, req);
		addCheckbox("isShown", id, 1, object, vh, row, req);
		addCheckbox("isShownPageSpec", id, 2, object, vh, row, req);
	}

	@Override
	public void addWidgetsAboveTable() {
		init = new ResourceInitSingleEmpty<KPIPageConfig>(page, "init", true, controller.appMan);
		
		this.header = new Header(page, "header", "Gateway Configuration page") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				KPIPageConfig gwsShown = init.getSelectedItem(req);
				if(gwsShown == null) setText("Gateway Configuration page", req);
				else setText("Select gatewayes to show for "+gwsShown.name().getValue(), req);
			}
		};
		page.append(header);
		//page.append(saveButton);
		page.append(Linebreak.getInstance());
	}
	
	@Override
	protected void addWidgetsBelowTable() {
		super.addWidgetsBelowTable();
		init.registerDependentWidget(mainTable);
	}

	public static Collection<String> getGwsToUse(OfflineEvaluationControlController controller) {
		Set<String> allGws = controller.getGatewayIds();
		if(!controller.appConfigData.gatewaysToUse().isActive() || (controller.appConfigData.gatewaysToUse().size() == 0))
			return allGws;
		else
			return Arrays.asList(controller.appConfigData.gatewaysToUse().getValues());
	}
	private static Collection<String> getGwsToShow0(OfflineEvaluationControlController controller) {
		if(!controller.appConfigData.gatewaysToShow().isActive() || (controller.appConfigData.gatewaysToShow().size() == 0)) {
			Set<String> allGws = controller.getGatewayIds();
			return allGws;
		} else
			return Arrays.asList(controller.appConfigData.gatewaysToShow().getValues());
	}
	public static Collection<String> getGwsToShow(OfflineEvaluationControlController controller, KPIPageConfig pageConfig) {
		if(pageConfig == null) return getGwsToShow0(controller);
		if(!pageConfig.gatewaysToShow().isActive() || (pageConfig.gatewaysToShow().size() == 0)) {
			return getGwsToShow0(controller);
		} else
			return Arrays.asList(pageConfig.gatewaysToShow().getValues());
	}
	
	//List<GatewayConfigData> objectsInTable = null;
	@Override
	public List<GatewayConfigData> getObjectsInTable(OgemaHttpRequest req) {
		List<GatewayConfigData> objectsInTable = new ArrayList<>();
		List<String> allGws = new ArrayList<String>(controller.getGatewayIds()); //Arrays.asList((String[]) controller.getGatewayIds().toArray());
		Collection<String> toUse = getGwsToUse(controller);
		
		final Collection<String> toShow = getGwsToShow(controller, null);
		final KPIPageConfig gwsShown = init.getSelectedItem(req);
		final Collection<String> toShowPageSpec;
		if(gwsShown != null) {
			toShowPageSpec = getGwsToShow(controller, gwsShown);
		} else toShowPageSpec = null;
		allGws.sort(null);
		for(String gw: allGws) {
			GatewayConfigData newEl = new GatewayConfigData();
			newEl.isUsed = toUse.contains(gw);
			newEl.isShown = toShow.contains(gw);
			newEl.name = gw;
			if(gwsShown != null) {
				newEl.isShownPageSpec = toShowPageSpec.contains(gw);
			}
			objectsInTable.add(newEl);
		}
		return objectsInTable;
	}

	@Override
	public Resource getResource(GatewayConfigData object, OgemaHttpRequest req) {
		//should not be used
		return null;
	}
	
	@Override
	public String getLineId(GatewayConfigData object) {
		return ResourceUtils.getValidResourceName(object.name);
	}
}
