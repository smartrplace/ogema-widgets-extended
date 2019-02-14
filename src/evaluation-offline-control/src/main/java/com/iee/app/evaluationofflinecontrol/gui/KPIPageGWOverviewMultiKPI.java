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

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.externalviewer.extensions.DefaultDedicatedTSSessionConfiguration;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.ogema.externalviewer.extensions.DefaultTimeSeriesFilterExtended;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.util.directresourcegui.kpi.KPIResultType;
import org.ogema.util.directresourcegui.kpi.KPIStatisticsManagement;
import org.ogema.util.directresourcegui.kpi.KPIStatisticsUtil;
import org.ogema.util.jsonresult.management.EvalResultManagementStd;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;
import com.iee.app.evaluationofflinecontrol.config.KPIPageConfig;
import com.iee.app.evaluationofflinecontrol.config.ResultToShow;
import com.iee.app.evaluationofflinecontrol.util.ScheduleViewerConfigProvEvalOff;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.Linebreak;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.resource.widget.init.ResourceRedirectButton;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationBuilder;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;
import de.iwes.widgets.template.DefaultDisplayTemplate;

/** This KPI page allows to show a number of evaluation results for all gateways evaluated.
 * The KPIs are shown in columns, so the KPIs and the number of results shown are fixes.
 * The page shall be able to auto-queue the respective evaluations. It may generate a KPIPageConfig, but
 * it does not need a dropdown to select it.
 * 
 * @author dnestle
 *
 */
public class KPIPageGWOverviewMultiKPI extends KPIPageGWOverview {
	private TemplateDropdown<Long> showTimeDrop;
	
	public static class KPIColumn {
		public int pastColumnNum;
		public String resultId;
	}
	protected final List<KPIColumn> columns;
	protected final KPIPageConfig entrRes;
	
	protected static class KPIResultTypeMultiKPI extends KPIResultType {
		/**
		 * 
		 * @param specialLindeId
		 * @param ksmList
		 * @param additionalColumns may be null
		 */
		public KPIResultTypeMultiKPI(String specialLindeId,
				List<KPIStatisticsManagementI> ksmList,
				Map<String, String> additionalColumns) {
			super(ksmList.get(0), specialLindeId);
			this.ksmList = ksmList;
			this.additionalColumns = additionalColumns;
		}

		//The list must have corresponding indecies with List columns
		public final List<KPIStatisticsManagementI> ksmList;
		
		public final Map<String, String> additionalColumns;
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public List<KPIStatisticsManagementI> ksmList() {
			return (List)ksmList;
		}
	}
	
	public static class PageConfig {
		public PageConfig() {}
		public PageConfig(boolean showGatewayConfigButton, boolean showEvalStartButton) {
			this.showGatewayConfigButton = showGatewayConfigButton;
			this.showEvalStartButton = showEvalStartButton;
		}
		public boolean showGatewayConfigButton = true;
		public boolean showEvalStartButton = true;
	}
	protected final PageConfig config;

	public KPIPageGWOverviewMultiKPI(WidgetPage<?> page, OfflineEvaluationControlController app,
			final List<KPIColumn> columns, KPIPageConfig entrRes,
			PageConfig config) {
		super(page, app, false);
		this.columns = columns;
		this.entrRes = entrRes;
		if(config == null) this.config = new PageConfig();
		else this.config = config;
		triggerPageBuild();
	}

	protected Header getHeader(WidgetPage<?> page) {
		return new Header(page, "header", "MultiKPIs: "+entrRes.name().getValue());		
	}

	@Override
	public Collection<KPIResultType> getObjectsInTable(OgemaHttpRequest req2) {
		final ResultToShow firstResToShow = getFirstResultToShow();
		if(firstResToShow == null) return Collections.emptyList();
		List<KPIResultType> result = new ArrayList<>();
		MultiKPIEvalConfiguration evalConfig = firstResToShow.evalConfiguration();
		final Collection<String> gws = scheduler.getIndividualGatewayList(evalConfig);
		/*if(evalConfig.individualResultKPIs().size() > 0) {
			gws = new ArrayList<>();
			for(IndividualGWResultList s: evalConfig.individualResultKPIs().getAllElements()) gws.add(s.getName());
		} else gws = Arrays.asList(evalConfig.gwIds().getValues());*/
		Collection<String> toShow = GatewayConfigPage.getGwsToShow(app, entrRes);
		List<String> toRemove = new ArrayList<>();
		for(String gw: gws) {
			if(!toShow.contains(gw)) toRemove.add(gw);
		}
		gws.removeAll(toRemove);
		for(String gwId: gws) {
			List<KPIStatisticsManagementI> ksmList = new ArrayList<>();
			Map<String, String> addCols = null;
			for(KPIColumn col: columns) {
				KPIStatisticsManagementI ksm;
				if(col.resultId.equals("timeOfCalculation")) {
					if(ksmList.isEmpty()) continue; //cannot be first element
					ksm = ksmList.get(0);
				}
				else ksm = getKPIManagementSingle(getResultToShow(col.resultId).evalConfiguration(),
						col.resultId, gwId);
				if(ksm == null && ksmList.isEmpty())
					break; //First element must be non-null
				if(ksm != null) {
					ksm.setScheduler(scheduler);
					ksm.setBaseInterval(EvalResultManagementStd.STANDARD_MULTIEVAL_INTERVAL_STEP);
				}
				ksmList.add(ksm);
			}
			if(!ksmList.isEmpty())
				result.add(new KPIResultTypeMultiKPI(gwId, ksmList, addCols));
		}
		if(!(entrRes.hideOverallLine().isActive() && entrRes.hideOverallLine().getValue())) {
			List<KPIStatisticsManagementI> ksmList = new ArrayList<>();
			for(KPIColumn col: columns) {
				KPIStatisticsManagementI ksm = getKPIManagementSingle(firstResToShow.evalConfiguration(),
						col.resultId, null);
				if(ksm == null && ksmList.isEmpty())
					break; //First element must be non-null
				if(ksm != null) {
					ksm.setScheduler(scheduler);
					ksm.setBaseInterval(EvalResultManagementStd.STANDARD_MULTIEVAL_INTERVAL_STEP);
				}
				ksmList.add(ksm);
			}
			if(!ksmList.isEmpty())
				result.add(new KPIResultTypeMultiKPI("Overall", ksmList, null));
		}
		for(KPIResultType k: result) {
			k.setScheduler(scheduler);
			k.setBaseInterval(EvalResultManagementStd.STANDARD_MULTIEVAL_INTERVAL_STEP);
		}
		return result;
	}

	private ResultToShow getFirstResultToShow() {
		if(entrRes == null || entrRes.resultsToShow().size()==0) return null;
		ResultToShow resToShow = entrRes.resultsToShow().getAllElements().get(0);
		return resToShow;
	}
	private ResultToShow getResultToShow(String resultId) {
		if(entrRes == null || entrRes.resultsToShow().size()==0) return null;
		for(ResultToShow rts:  entrRes.resultsToShow().getAllElements()) {
			if(rts.resultId().getValue().equals(resultId)) return rts;
		}
		return null;
	}
	private KPIStatisticsManagementI getSampleOverallKsm() {
		ResultToShow resToShow = getFirstResultToShow();
		if(resToShow == null) return null;
		KPIStatisticsManagementI ksm = getKPIManagementSingle(resToShow.evalConfiguration(),
				columns.get(0).resultId, null);
		return ksm;
	}
	
	private List<KPIStatisticsManagementI> getKPIManagement(MultiKPIEvalConfiguration startConfig,
			String subGateway) {
		String providerId = startConfig.evaluationProviderId().getValue();
		GaRoSingleEvalProvider eval = scheduler.getProvider(providerId);
		return scheduler.configureKPIManagement(startConfig, eval, subGateway, null);
	}
	
	/** Get the {@link KPIStatisticsManagement} for a certain resultType
	 * 
	 * @param startConfig
	 * @param resultId
	 * @param gwSubId if null the normal evaluation over all gateways is addressed
	 * @return
	 */
	private KPIStatisticsManagementI getKPIManagementSingle(MultiKPIEvalConfiguration startConfig,
			String resultId, String gwSubId) {
		List<KPIStatisticsManagementI> result = getKPIManagement(startConfig, gwSubId);
		for(KPIStatisticsManagementI kpi: result) {
			if(kpi.resultTypeId().equals(resultId)) return kpi;
		}
		return null;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTableSuper();

		showTimeDrop = new TemplateDropdown<Long>(page, "showTime") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				KPIStatisticsManagementI ksm = getSampleOverallKsm();
				List<Long> vals = new ArrayList<>();
				Long autoSelect = null;
				if(ksm != null) {
					int intervalType = intervalDrop.getSelectedItem(req);
					List<SampledValue> svv = ksm.getIntervalSchedule(intervalType).getValues(0);
					if(!svv.isEmpty()) {
						long shiftCurrent = (long) (1.5*AbsoluteTimeHelper.getStandardInterval(intervalType));
						for(SampledValue val: svv) {
							long ts = val.getTimestamp()+shiftCurrent;
							vals.add(ts);
							if(!Float.isNaN(val.getValue().getFloatValue())) autoSelect = ts;
						}
					} else
						vals.add(appMan.getFrameworkTime());					
				} else
					vals.add(appMan.getFrameworkTime());
				Long preSel = getSelectedItem(req);
				update(vals, req);
				Long nowSel = getSelectedItem(req);
				if(((nowSel == null) || !nowSel.equals(preSel)) &&
						(autoSelect != null)) {
					selectItem(autoSelect, req);
				}
			}
		};
		showTimeDrop.setTemplate(new DefaultDisplayTemplate<Long>() {
			@Override
			public String getLabel(Long object, OgemaLocale locale) {
				return TimeUtils.getDateAndTimeString(object);
			}
		});
		
		this.dateOfReport = new Label(page, "dateOfReport") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				Long st = showTimeDrop.getSelectedItem(req);
				if(st != null) currentTime = st;
				else {
					KPIStatisticsManagementI ksm = getSampleOverallKsm();
					if(ksm != null) {
						int intervalType = intervalDrop.getSelectedItem(req);
						SampledValue val = ksm.getIntervalSchedule(intervalType).getPreviousValue(Long.MAX_VALUE);
						if(val != null)
							currentTime = val.getTimestamp();
						else
							currentTime = appMan.getFrameworkTime();					
					} else
						currentTime = appMan.getFrameworkTime();
				}
				String timeOfReport = "Time of Report: " + TimeUtils.getDateAndTimeString(currentTime);
				setText(timeOfReport, req);
			}
		};
		
		StaticTable topTable = new StaticTable(1, 3);
		if(config.showEvalStartButton) {
			ResourceRedirectButton<KPIPageConfig> openConfigButton =
					new ResourceRedirectButton<KPIPageConfig>(page, "openConfigButton", "Select Gateways", "configurationGws.html");
			openConfigButton.selectDefaultItem(entrRes);
			topTable.setContent(0, 0, openConfigButton);
		}
		
		//triggerOnPost(dateOfReport, intervalDrop);
		triggerOnPost(intervalDrop, showTimeDrop);
		triggerOnPost(showTimeDrop, dateOfReport);
		triggerOnPost(showTimeDrop, mainTable);

		topTable.setContent(0, 1, intervalDrop).setContent(0, 2, showTimeDrop);
		
		this.header = getHeader(page);
		page.append(header);
		page.append(Linebreak.getInstance());
		page.append(dateOfReport);
		
		page.append(topTable);
	}
	
	protected Set<String> getProviders() {
		Set<String> result = new LinkedHashSet<>();
		for(ResultToShow res: entrRes.resultsToShow().getAllElements()) {
			result.add(res.evalConfiguration().evaluationProviderId().getValue());
		}
		return result ;
	}
	
	@Override
	protected void addWidgetsBelowTable() {
		if(!config.showEvalStartButton)
			return;
		StaticTable bottomTable =  new StaticTable(1, getProviders().size()+1);
		int i = 0;
		for(String evalId: getProviders()) {
			RedirectButton startEvalButton = new RedirectButton(page, "startEvalButton"+evalId, "Start "+evalId+"...",
					"OfflineEvaluationControl.html?configId="+evalId);
			bottomTable.setContent(0, i, startEvalButton);
			i++;
		}
		if(entrRes.messageProviderId().isActive()) {
			Button sendMessageButton = new Button(page, "sendMessageButton", "Send Message") {
				private static final long serialVersionUID = 1L;
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					app.sendMessage(entrRes.pageId().getValue());
				}
			};
			bottomTable.setContent(0, i, sendMessageButton);
		}
		page.append(bottomTable);
	}

	@Override
	/** For now we do not support "Current" here
	 * 
	 */
	public void addWidgets(KPIResultType objectIn, ObjectResourceGUIHelper<KPIResultType, Resource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if(additionalColumns != null && additionalColumns.contains("Gateway")) {
			if(req != null)
				vh.stringLabel("Gateway", id, objectIn.specialLindeId, row);
			else
				vh.registerHeaderEntry("Gateway");
		}
		if(!((objectIn == null) || (objectIn instanceof KPIResultTypeMultiKPI)))
			throw new IllegalStateException("Table objects must be of type KPIResultTypeMultiKPI!");
		KPIResultTypeMultiKPI objM = (KPIResultTypeMultiKPI)objectIn;
		for(int idx=0; idx<columns.size(); idx++) {
			ResultType r = null;
			KPIColumn col = columns.get(idx);
			final KPIStatisticsManagementI objectLoc;
			final GaRoDataTypeI dataType;
			if(req != null) {
				objectLoc = objM.ksmList.get(idx);
				if(objectLoc == null) continue;
				GaRoSingleEvalProvider prov = objectLoc.getEvalProvider(); //scheduler.getProvider(objectLoc.providerId);
				if(prov != null ) {
					r = getResultTypeById(prov, objectLoc.resultTypeId());
				}
			} else objectLoc = null;
			if(r != null && r instanceof GaRoDataTypeI) dataType = (GaRoDataTypeI) r;
			else dataType = null;
			for(int pastIdx = col.pastColumnNum; pastIdx > 0; pastIdx--) {
				final String widgetResultId = col.resultId.startsWith("$")?(col.resultId.substring(1)):col.resultId;
				final String name;
				if(col.pastColumnNum > 1) name = widgetResultId+"_"+pastIdx;
				else name = widgetResultId;
				if(req == null) {
					vh.registerHeaderEntry(name);
					continue;
				}
				final int localPastIdx = pastIdx;
				Label valueLabel;
				if(objectLoc == null)
					valueLabel = new Label(mainTable, "Minus"+pastIdx+"_"+widgetResultId+"_"+id, "n.i.eval", req);
				else
					valueLabel = new Label(mainTable, "Minus"+pastIdx+"_"+widgetResultId+"_"+id, req) {
						private static final long serialVersionUID = 1L;
						@Override
						public void onGET(OgemaHttpRequest req) {
							int intervalType = intervalDrop.getSelectedItem(req);
							if(col.resultId.equals("timeOfCalculation")) {
								if(intervalType != intervalTypesNP.get(0)) {
									setText("not base", req);
									return;
								}
								SampledValue sv = objectLoc.getTimeOfCalculationNonAligned(intervalType, currentTime, localPastIdx);
								if(sv == null) {
									setText("n/a", req);																	
								} else {
									long tcalc = sv.getValue().getLongValue();
									String text = TimeUtils.getDateAndTimeString(tcalc);
									setText(text, req);
								}
							} else {
								if(col.resultId.startsWith("$")) {
									if(objectIn.specialLindeId().equals("Overall")) {
										setText("see below", req);
										return;
									}
									String text = objectLoc.getStringValue(currentTime, localPastIdx);
									if(text == null)
										setText("n/a", req);									
									else
										setText(text, req);									
								} else {
									SampledValue sv = objectLoc.getValueNonAligned(intervalType, currentTime, localPastIdx);
									String text = formatValue(sv, dataType);
									setText(text, req);
								}
							}
						}
					};
				row.addCell(name, valueLabel);
				vh.triggerOnPostForRequest(intervalDrop, valueLabel);
				vh.triggerOnPostForRequest(showTimeDrop, valueLabel);
			}
		}
		if(req == null) {
			vh.registerHeaderEntry("Size");
			vh.registerHeaderEntry("Viewer");
		} else {
			Label valueLabel = new Label(mainTable, "Size_"+id, req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					int intervalType = intervalDrop.getSelectedItem(req);
					ReadOnlyTimeSeries sched = objectIn.getIntervalSchedule(intervalType);
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
			vh.triggerOnPostForRequest(intervalDrop, valueLabel);
			
			//FIXME
			DefaultScheduleViewerConfigurationProviderExtended schedProv = ScheduleViewerConfigProvEvalOff.getInstance();
			if(schedProv != app.schedProv) {
				throw new IllegalStateException("Sched in app:"+app.schedProv+" now:"+schedProv);
			}
			ScheduleViewerOpenButton openScheduleViewer = new ScheduleViewerOpenButton(mainTable, "openScheduleViewer"+id, "Data Viewer",
					ScheduleViewerConfigProvEvalOff.PROVIDER_ID,
					schedProv, req) {
				private static final long serialVersionUID = 1L;
				
				public void onGET(OgemaHttpRequest req) {
					int intervalType = intervalDrop.getSelectedItem(req);
					ReadOnlyTimeSeries sched = objectIn.getIntervalSchedule(intervalType);
					if(sched == null || sched.size() < 3) {
						disable(req);
					} else enable(req);
				};
				
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					List<ReadOnlyTimeSeries> result = new ArrayList<>();
					Map<ReadOnlyTimeSeries, String> shortNames = new HashMap<ReadOnlyTimeSeries, String>();
					Map<ReadOnlyTimeSeries, String> longNames = new HashMap<ReadOnlyTimeSeries, String>();
					Map<ReadOnlyTimeSeries, Class<?>> types = new HashMap<>();
					int intervalType = intervalDrop.getSelectedItem(req);
					String intervalName = intervalDrop.getSelectedLabel(req);
					long startTime = -1;
					for(int idx=0; idx<columns.size(); idx++) {
						final KPIStatisticsManagementI objectLoc;
						objectLoc = objM.ksmList.get(idx);
						if(objectLoc == null) continue;
						ReadOnlyTimeSeries sched = objectLoc.getIntervalSchedule(intervalType);
						if(startTime < 0) {
							SampledValue sv = sched.getNextValue(0);
							if(sv != null) startTime = sv.getTimestamp();
						}
						String shortName = objM.specialLindeId+"-"+intervalName+objectLoc.resultTypeId();
						shortNames.put(sched, shortName);
						String loc;
						if(sched instanceof Resource)
							loc = ((Resource)sched).getLocation();
						else {
							ChronoUnit chrono = KPIStatisticsUtil.getIntervalTypeChronoUnit(intervalType);
							loc = objectLoc.evalConfigLocation()+"#"+shortName+"#"+chrono.toString();
						}
							
						longNames.put(sched, objM.specialLindeId+"-"+intervalName+objectLoc.resultTypeId()+"-"+loc);

						ResultType r = null;
						final GaRoDataTypeI dataType;
						GaRoSingleEvalProvider prov = objectLoc.getEvalProvider(); //scheduler.getProvider(objectLoc.providerId);
						if(prov != null ) {
							r = getResultTypeById(prov, objectLoc.resultTypeId());
						}
						if(r != null && r instanceof GaRoDataTypeI) dataType = (GaRoDataTypeI) r;
						else dataType = null;
						if(dataType != null) types.put(sched, dataType.representingResourceType());
						result.add(sched);
					}
					List<Collection<TimeSeriesFilter>> programs = new ArrayList<>();
					List<TimeSeriesFilter> programsInner = new ArrayList<>();
					
					programsInner.add(new DefaultTimeSeriesFilterExtended("Filter for "+objectIn.specialLindeId,
							shortNames, longNames, null, null, types, null));
					programs.add(programsInner);
					long endTime = appMan.getFrameworkTime();
					if(startTime < 0) startTime = endTime;
					else if(endTime < startTime) endTime = startTime;
					final ScheduleViewerConfiguration viewerConfiguration =
							ScheduleViewerConfigurationBuilder.newBuilder().setPrograms(programs).
							setStartTime(startTime).setEndTime(endTime).build();
					
					String ci = addConfig(new DefaultDedicatedTSSessionConfiguration(result, viewerConfiguration));
					setConfigId(ci, req);
				}
			};
			row.addCell("Viewer", openScheduleViewer);
		}
	}

	@Override
	public String getLineId(KPIResultType object) {
		return ResourceUtils.getValidResourceName(object.specialLindeId);
	}
}
