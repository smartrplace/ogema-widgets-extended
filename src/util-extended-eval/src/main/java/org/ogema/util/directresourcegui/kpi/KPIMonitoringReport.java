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
package org.ogema.util.directresourcegui.kpi;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.util.evalcontrol.EvalScheduler;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.Linebreak;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;

public abstract class KPIMonitoringReport extends ObjectGUITablePage<KPIResultType, Resource> {
	//protected List<OgemaWidget> additionalGovernors() { return Collections.emptyList();}
	
	protected Header header;
	protected IntervalTypeDropdown intervalDrop;
	private StaticTable footerTable;
	protected Button updateButton;
	protected Label dateOfReport;
	protected Alert alert;
	
	protected final List<Integer> intervalTypesNP;
	protected List<Integer> getIntervalTypes(OgemaHttpRequest req) {
		return intervalTypesNP;
	}
	protected final Integer standardInterval; //may be null
	protected final int pastColumnNum;
	protected final boolean registerDependentWidgets;
	
	protected final EvalScheduler scheduler;
	protected final boolean autoHeader;
	protected final List<String> additionalColumns;
	
	//TODO: This is not thread-safe. Every session should have its own currentTime, but
	//this requires a special widget data
	protected long currentTime;
	
	/** Overwrite this method to perform data update when "Update All" button is pressed
	 */
	protected void updateAll(OgemaHttpRequest req) {};
	
	public KPIMonitoringReport(WidgetPage<?> page, ApplicationManager appMan, EvalScheduler scheduler,
			List<Integer> intervalTypes, int pastColumnNum, boolean registerDependentWidgets,
			Integer standardInterval) {
		this(page, appMan, scheduler, intervalTypes, pastColumnNum, registerDependentWidgets,
				standardInterval, true, null, true);
	}
	/** Constructor
	 * 
	 * @param page
	 * @param appMan
	 * @param scheduler
	 * @param intervalTypes intervalTypes that shall be offered in the interval dropdown
	 * @param pastColumnNum number of intervals we want to look into the past, if pastColumnNum is
	 * 		given as 2 then e.g. the current day, yesterday and the day before yesterday will be
	 * 		shown if the interval type day is selected
	 * @param registerDependentWidgets if true registerDependentWidgets is used, otherwise triggerOnPost
	 * @param standardInterval if not null this must be one option defined by intervalTypes
	 * @param autoHeader if false no header is generated and the widgets dataOfReport and updateButton
	 * 		are not generated automatically in the method {@link #addWidgetsAboveTable()}.
	 * 		This should thus be done in an overwritten method addWidgetsAboveTable. Default is true.
	 * @param additionalColumns give columns here that shall be generated as optional, currently
	 * 		supported:<br>
	 * 		<li> Current </li>
	 * 		<li> Gateway </li>
	 * 		May be null if no additional columns are needed.
	 * 		Note that each type requires special implementation in {@link #addWidgets(KPIResultType, ObjectResourceGUIHelper, String, OgemaHttpRequest, Row, ApplicationManager)}
	 */
	public KPIMonitoringReport(WidgetPage<?> page, ApplicationManager appMan, EvalScheduler scheduler,
			List<Integer> intervalTypes, int pastColumnNum, boolean registerDependentWidgets,
			Integer standardInterval, boolean autoHeader, List<String> additionalColumns,
			boolean autoBuildPage) {
		super(page, appMan, null, false, registerDependentWidgets);
		this.scheduler = scheduler;
		this.intervalTypesNP = intervalTypes;
		this.standardInterval = standardInterval;
		this.pastColumnNum = pastColumnNum;
		this.registerDependentWidgets = registerDependentWidgets;
		this.autoHeader = autoHeader;
		this.additionalColumns = additionalColumns;
		if(autoBuildPage) triggerPageBuild();
	}
	
	public KPIMonitoringReport(WidgetPage<?> page, ApplicationManagerMinimal appManMin, EvalScheduler scheduler,
			List<Integer> intervalTypes, int pastColumnNum, boolean registerDependentWidgets,
			Integer standardInterval) {
		this(page, appManMin, scheduler, intervalTypes, pastColumnNum, registerDependentWidgets,
				standardInterval, true, null);	
	}

	/** Version of constructor supporting ApplicationManagerMinimal instead of ApplicationManager*/
	public KPIMonitoringReport(WidgetPage<?> page, ApplicationManagerMinimal appManMin, EvalScheduler scheduler,
			List<Integer> intervalTypes, int pastColumnNum, boolean registerDependentWidgets,
			Integer standardInterval, boolean autoHeader, List<String> additionalColumns) {
		super(page, null, appManMin, null, false, registerDependentWidgets);
		this.scheduler = scheduler;
		this.intervalTypesNP = intervalTypes;
		this.standardInterval = standardInterval;
		this.pastColumnNum = pastColumnNum;
		this.registerDependentWidgets = registerDependentWidgets;
		this.autoHeader = autoHeader;
		this.additionalColumns = additionalColumns;
		triggerPageBuild();
	}	
	@Override
	protected KPIResultType getHeaderObject() {
		return new KPIResultType(true);
	}
	@Override
	protected String getHeaderText(String columnId, final ObjectResourceGUIHelper<KPIResultType, Resource> vh, OgemaHttpRequest req) {
		vh.setDoRegisterDependentWidgets(registerDependentWidgets);
		return null;
	}
	protected String getHeaderText(String columnId, OgemaHttpRequest req) {
		if(!columnId.startsWith("Minus")) return columnId;
		int intervalType = intervalDrop.getSelectedItem(req);
		String intervalName = IntervalTypeDropdown.getIntervalTypeName(intervalType, req.getLocale());
		if(intervalName.endsWith("s")) intervalName = intervalName.substring(0, intervalName.length()-1);
		return intervalName+"-"+columnId.substring(5);
	}
	@Override
	protected OgemaWidget getHeaderWidget(String columnId, ObjectResourceGUIHelper<KPIResultType, Resource> vh,
			OgemaHttpRequest req) {
		Label label = new Label(mainTable, "Header_"+columnId, req) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				setText(getHeaderText(columnId, req), req);
			}
		};
		vh.triggerOnPost(intervalDrop, label);
		return label;
	}
	
	@Override
	public void addWidgets(KPIResultType object, ObjectResourceGUIHelper<KPIResultType, Resource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		ResultType r = null;
		final GaRoDataTypeI dataType;
		if(req != null) {
			GaRoSingleEvalProvider prov = scheduler.getProvider(object.providerId());
			if(prov != null ) {
				vh.stringLabel("Provider", id, prov.label(req.getLocale()), row);
				r = getResultTypeById(prov, object.resultTypeId());
			} else 
				vh.stringLabel("Provider", id, object.providerId(), row);
			if(r != null) {
				vh.stringLabel("Result", id, r.description(req.getLocale()), row);
			} else
				vh.stringLabel("Result", id, object.resultTypeId(), row);
		} else {
			vh.registerHeaderEntry("Provider");
			vh.registerHeaderEntry("Result");
		}
		if(additionalColumns != null && additionalColumns.contains("Gateway")) {
			if(req != null)
				vh.stringLabel("Gateway", id, object.specialLindeId, row);
			else
				vh.registerHeaderEntry("Gateway");
		}
		if(r != null && r instanceof GaRoDataTypeI) dataType = (GaRoDataTypeI) r;
		else dataType = null;
		for(int pastIdx = pastColumnNum; pastIdx > 0; pastIdx--) {
			if(req == null) {
				vh.registerHeaderEntry("Minus"+pastIdx);
				continue;
			}
			final int localPastIdx = pastIdx;
			Label valueLabel = new Label(mainTable, "Minus"+pastIdx+"_"+id, req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					int intervalType = intervalDrop.getSelectedItem(req);
					SampledValue sv = object.getValueNonAligned(intervalType, currentTime, localPastIdx);
					String text = formatValue(sv, dataType);
					setText(text, req);
				}
			};
			row.addCell("Minus"+pastIdx, valueLabel);
			vh.triggerOnPost(intervalDrop, valueLabel);
		}
		if(additionalColumns != null && additionalColumns.contains("Current")) {
			if(req == null) {
				vh.registerHeaderEntry("Current");
			} else {
				Label valueLabel = new Label(mainTable, "Current_"+id, req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						int intervalType = intervalDrop.getSelectedItem(req);
						SampledValue sv = object.getValueNonAligned(intervalType, currentTime, 0);
						String text = formatValue(sv, dataType);
						setText(text, req);
					}
				};
				row.addCell("Current", valueLabel);
				vh.triggerOnPost(intervalDrop, valueLabel);
			}
		}
		if(req == null) {
			vh.registerHeaderEntry("Size");
		} else {
			Label valueLabel = new Label(mainTable, "Size_"+id, req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					int intervalType = intervalDrop.getSelectedItem(req);
					ReadOnlyTimeSeries sched = object.getIntervalSchedule(intervalType);
					String text;
					if(sched != null) {
						int size = sched.size();
						text = ""+size;
					}
					else text = "n/a*";
					setText(text, req);
				}
			};
			row.addCell("Size", valueLabel);
			vh.triggerOnPost(intervalDrop, valueLabel);
		}
	}

	@Override
	public Resource getResource(KPIResultType object, OgemaHttpRequest req) {
		return null;
		//throw new IllegalStateException("should not be used!");
	}

	protected String getHeaderText() {
		return "KPI Standard Overview";
	}
	
	@Override
	public void addWidgetsAboveTable() {
		this.alert = new Alert(page, "alert", "");
		alert.setDefaultAllowDismiss(true);
		page.append(alert);
		this.intervalDrop = new IntervalTypeDropdown(page, "singleTimeInterval", false, intervalTypesNP, standardInterval) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				update(getIntervalTypes(req), req);
			}			
		};
		
		if(autoHeader) {
			this.dateOfReport = new Label(page, "dateOfReport") {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					currentTime = appMan.getFrameworkTime();
					String timeOfReport = "Time of Report: " + TimeUtils.getDateAndTimeString(currentTime);
					setText(timeOfReport, req);
				}
			};
			triggerOnPost(dateOfReport, intervalDrop);
			this.header = new Header(page, "header", getHeaderText());
			page.append(header);
			page.append(Linebreak.getInstance());
			page.append(dateOfReport);
			page.append(intervalDrop);
			
			this.updateButton = new Button(page, "updateButton", "Update All") {
				private static final long serialVersionUID = 1L;
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					updateAll(req);
				}
			};
			triggerOnPost(updateButton, alert);
			footerTable = new StaticTable(1, 3);
			footerTable.setContent(0, 0, updateButton);
			footerTable.setContent(0, 1, "");
			footerTable.setContent(0, 2, "");
		}
	}
	@Override
	protected void addWidgetsBelowTable() {
		page.append(footerTable);
	}
	
	int lineIdCounter = 0;
	@Override
	public String getLineId(KPIResultType object) {
		if((headerObject != null) && object.toString().equals(headerObject.toString())) {
			return DynamicTable.HEADER_ROW_ID;
		}
		String li = super.getLineId(object);
		lineIdCounter++;
		return String.format("%d_",  lineIdCounter)+li;
	}
	
	@SuppressWarnings("deprecation")
	public void triggerOnPost(OgemaWidget governor, OgemaWidget target) {
		if(registerDependentWidgets) governor.registerDependentWidget(target);
		else governor.triggerAction(target, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
	public static ResultType getResultTypeById(GaRoSingleEvalProvider prov, String resultId) {
		for(ResultType r: prov.resultTypes()) {
			if(r.id().equals(resultId)) return r;
		}
		return null;
	}
	
	public static String formatValue(SampledValue sv, GaRoDataTypeI dataType) {
		if(sv == null) return "n/a*";

		if(dataType != null && TemperatureResource.class.isAssignableFrom(dataType.representingResourceType()))
				return String.format("%.2f°C", sv.getValue().getFloatValue()-273.15);
		return String.format("%.2f", sv.getValue().getFloatValue());
	}
}
