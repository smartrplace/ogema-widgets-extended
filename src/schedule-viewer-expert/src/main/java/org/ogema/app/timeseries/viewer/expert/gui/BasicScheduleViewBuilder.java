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
import java.util.concurrent.CopyOnWriteArrayList;

import org.ogema.app.timeseries.viewer.expert.ScheduleViewerBasicApp;
import org.ogema.app.timeseries.viewer.expert.dummyProvider.DummyScheduleViewerConfigurationProvider;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.recordeddata.DataRecorder;
import org.ogema.recordeddata.RecordedDataStorage;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.reswidget.scheduleviewer.clone.ScheduleViewerExtended;

public class BasicScheduleViewBuilder {

	//private final ScheduleViewerBasicApp app;
	private final ScheduleViewerExtended scheduleViewer;
	public final static long MAX_UPDATE_INTERVAL = 30000; // do not update values more often than every 30s...
	private long lastUpdate = System.currentTimeMillis() - 2 * MAX_UPDATE_INTERVAL;
	private final CopyOnWriteArrayList<ReadOnlyTimeSeries> items = new CopyOnWriteArrayList<>();
	private final DataRecorder dataRecorder;
	//final String PROVIDER_ID;

	/*public BasicScheduleViewBuilder(final WidgetPage<?> page, final ScheduleViewerConfigurationProvider provider,
			final ScheduleViewerBasicApp app, final OgemaHttpRequest req) {
		//this.app = app;
		dataRecorder = app.getDataRecorder();
		PROVIDER_ID = provider.getConfigurationProviderId();
		String configId = MainPage.getPageParameter(req, page, ScheduleViewerBasicApp.PARAM_SELECTION_CONFIG_ID);			
		SessionConfiguration sessionConfig = provider.getSessionConfiguration(configId);
		
		scheduleViewer = new ScheduleViewerExtended(page, PROVIDER_ID+configId, app.appManager, null, sessionConfig, provider) {

			private static final long serialVersionUID = 325148964135465313L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				String configId = MainPage.getPageParameter(req, page, ScheduleViewerBasicApp.PARAM_SELECTION_CONFIG_ID);				 
				updateSelectionConfiguration(req, configId); 
				String providerId = MainPage.getPageParameter(req, page, ScheduleViewerBasicApp.PARAM_PROVIDER_ID);	
				if(providerId == null) {
					providerId = DummyScheduleViewerConfigurationProvider.CONFIGURATION_PROVIDER_ID;
				}
				boolean visible = PROVIDER_ID.equals(providerId);				
				
				setWidgetVisibility(visible, req);
			}

			@Override
			protected List<ReadOnlyTimeSeries> getDefaultSchedules(final OgemaHttpRequest req) {
				final long now = System.currentTimeMillis();
				final boolean cancelUpdate;
				synchronized (this) {
					if (now - lastUpdate < MAX_UPDATE_INTERVAL)
						cancelUpdate = true;
					else {
						lastUpdate = now;
						cancelUpdate = false;
					}
				}
				if (cancelUpdate) {
					return items;
				}
				final List<ReadOnlyTimeSeries> newScheds = new ArrayList<>();
				for (ReadOnlyTimeSeries schedule : app.appManager.getResourceAccess().getResources(Schedule.class)) {
					newScheds.add(schedule);
				}
				RecordedDataStorage rds;
				for (String id : dataRecorder.getAllRecordedDataStorageIDs()) {
					rds = dataRecorder.getRecordedDataStorage(id);
					if (rds != null) {
						newScheds.add(rds);
					}
				}
				items.retainAll(newScheds);
				items.addAllAbsent(newScheds);
				return items;
			}
		};		
	}*/

	public BasicScheduleViewBuilder(WidgetPage<?> page, ScheduleViewerBasicApp app) {
		//this.app = app;
		dataRecorder = app.getDataRecorder();
		
		scheduleViewer = new ScheduleViewerExtended(page, "scheduleViewerExtended", app, null) {

			private static final long serialVersionUID = 325148964135465313L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				/*String configId = MainPage.getPageParameter(req, page, ScheduleViewerBasicApp.PARAM_SELECTION_CONFIG_ID);				 
				updateSelectionConfiguration(req, configId); 
				String providerId = MainPage.getPageParameter(req, page, ScheduleViewerBasicApp.PARAM_PROVIDER_ID);	
				if(providerId == null) {
					providerId = DummyScheduleViewerConfigurationProvider.CONFIGURATION_PROVIDER_ID;
				}*/
				//boolean visible = PROVIDER_ID.equals(providerId);				
				
				//setWidgetVisibility(visible, req);
			}

			@Override
			protected List<ReadOnlyTimeSeries> getDefaultSchedules(final OgemaHttpRequest req) {
				final long now = System.currentTimeMillis();
				final boolean cancelUpdate;
				synchronized (this) {
					if (now - lastUpdate < MAX_UPDATE_INTERVAL)
						cancelUpdate = true;
					else {
						lastUpdate = now;
						cancelUpdate = false;
					}
				}
				if (cancelUpdate) {
					return items;
				}
				final List<ReadOnlyTimeSeries> newScheds = new ArrayList<>();
				for (ReadOnlyTimeSeries schedule : app.appManager.getResourceAccess().getResources(Schedule.class)) {
					newScheds.add(schedule);
				}
				RecordedDataStorage rds;
				for (String id : dataRecorder.getAllRecordedDataStorageIDs()) {
					rds = dataRecorder.getRecordedDataStorage(id);
					if (rds != null) {
						newScheds.add(rds);
					}
				}
				items.retainAll(newScheds);
				items.addAllAbsent(newScheds);
				return items;
			}
		};		
	}



	public ScheduleViewerExtended getScheduleViewer() {
		return scheduleViewer;
	}
}
