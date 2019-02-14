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
package org.ogema.app.timeseries.viewer.expert.gui;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.resource.timeseries.OnlineTimeSeriesCache;
import de.iwes.widgets.resource.widget.autocomplete.ResourcePathAutocomplete;
import de.iwes.widgets.reswidget.scheduleviewer.DefaultSchedulePresentationData;
import de.iwes.widgets.reswidget.scheduleviewer.ScheduleViewerBasic;
import de.iwes.widgets.reswidget.scheduleviewer.api.SchedulePresentationData;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationBuilder;
import de.iwes.widgets.template.DisplayTemplate;

public class OnlineDataViewerPage {

	private final WidgetPage<?> page;
	private final Header header;
	private final ResourcePathAutocomplete newItemSelector;
	private final Button newItemSubmit;
	private final ScheduleViewerBasic<SchedulePresentationData> scheduleViewer;
	private final Cache<SingleValueResource, SchedulePresentationData> onlineTimeSeries = CacheBuilder.newBuilder()
			.softValues().build();

	public OnlineDataViewerPage(final WidgetPage<?> page, final OnlineTimeSeriesCache timeSeriesCache,
			final ApplicationManager am) {
		this.page = page;
		this.header = new Header(page, "header", true);
		header.setDefaultText("Online data viewer");
		header.addDefaultStyle(HeaderData.CENTERED);
		header.setDefaultColor("blue");

		this.newItemSelector = new ResourcePathAutocomplete(page, "newItemSelect", am.getResourceAccess()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected boolean filter(final Resource resource) {
				if (resource instanceof StringResource)
					return false;
				if (onlineTimeSeries.asMap().keySet().contains(resource))
					return false;
				return true;
			}

		};
		newItemSelector.setDefaultResourceType(SingleValueResource.class);

		this.newItemSubmit = new Button(page, "newItemSubmit", "Add resource") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				setWidgetVisibility(newItemSelector.getSelectedResource(req) != null, req);
			}

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				final Resource res = newItemSelector.getSelectedResource(req);
				if (!(res instanceof SingleValueResource) || onlineTimeSeries.asMap().containsKey(res)) // in particular
																										// null
					return;
				final ReadOnlyTimeSeries timeseries = timeSeriesCache
						.getResourceValuesAsTimeSeries((SingleValueResource) res);
				final Class<?> type = res instanceof TemperatureResource ? TemperatureResource.class
						: FloatResource.class;
				onlineTimeSeries.put((SingleValueResource) res,
						new DefaultSchedulePresentationData(timeseries, type, res.getPath()));
			}

		};
		final DisplayTemplate<SchedulePresentationData> template = new DisplayTemplate<SchedulePresentationData>() {

			@Override
			public String getId(SchedulePresentationData object) {
				return object.getLabel(OgemaLocale.ENGLISH);
			}

			@Override
			public String getLabel(SchedulePresentationData object, OgemaLocale locale) {
				return object.getLabel(locale);
			}
		};

		final ScheduleViewerConfiguration config = ScheduleViewerConfigurationBuilder.newBuilder()
				.setShowIndividualConfigBtn(true).setShowManipulator(false).setShowNrPointsPreview(false)
				.setShowOptionsSwitch(false).setShowStandardIntervals(false).setShowCsvDownload(false)
				.setUseNameService(false).build();

		this.scheduleViewer = new ScheduleViewerBasic<SchedulePresentationData>(page, "scheduleViewer", am, config,
				template) {

			private static final long serialVersionUID = 1L;

			@Override
			protected List<SchedulePresentationData> update(OgemaHttpRequest req) {
				return new ArrayList<>(onlineTimeSeries.asMap().values());
			}

			@Override
			public void onGET(OgemaHttpRequest req) {
				// now plus 1 week... this is actually irrelevant, should just be far enough in
				// the future
				final long end = am.getFrameworkTime() + 7 * 24 * 60 * 60 * 1000;
				setEndTime(end, req);
			}

		};
		scheduleViewer.getSchedulePlot().setDefaultPollingInterval(10000);
		scheduleViewer.getSchedulePlot().getDefaultConfiguration().doScale(false);
		buildPage();
		setDependencies();
	}

	private final void buildPage() {
		page.append(header).linebreak();
		int row = 0;
		final StaticTable tab = new StaticTable(2, 2, new int[] { 3, 3 }).setContent(row, 0, "Add a resource")
				.setContent(row++, 1, newItemSelector).setContent(row++, 1, newItemSubmit);
		page.append(tab).linebreak().append(scheduleViewer);
	}

	private final void setDependencies() {
		newItemSelector.triggerAction(newItemSubmit, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		newItemSubmit.triggerAction(scheduleViewer.getScheduleSelector(), TriggeringAction.POST_REQUEST,
				TriggeredAction.GET_REQUEST);
		newItemSubmit.triggerAction(newItemSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}

}
