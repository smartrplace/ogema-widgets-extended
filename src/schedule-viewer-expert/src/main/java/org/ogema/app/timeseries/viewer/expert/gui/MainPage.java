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

import org.ogema.app.timeseries.viewer.expert.ScheduleViewerBasicApp;
import org.ogema.core.application.ApplicationManager;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.reswidget.scheduleviewer.clone.ScheduleViewerExtended;
import de.iwes.widgets.reswidget.scheduleviewer.utils.ScheduleViewerUtil;

// ?providerId=LoadMonitoring&configId=config_0&expertMode=true

public class MainPage {

	private static final String HEADER_TEXT = System.getProperty("org.ogema.app.timeseries.viewer.expert.gui.header",
			"Schedule Viewer Standard Configurations");
	public final static long MAX_UPDATE_INTERVAL = 30000; // do not update values more often than every 30s...
	public final ScheduleViewerBasicApp scheduleViewerBasicApp;


	public ScheduleViewerExtended scheduleViewerSimple /*,scheduleViewerExpert*/;
	public final WidgetPage<?> page;
	
	ScheduleViewerUtil util = ScheduleViewerUtil.getInstance();

	public MainPage(final WidgetPage<?> page, final ApplicationManager appManager, final ScheduleViewerBasicApp app) {
		this.scheduleViewerBasicApp = app;
		this.page = page;

		final Header header = new Header(page, "header", HEADER_TEXT, true) {

			private static final long serialVersionUID = -862546046902822922L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				String text;
				if(Boolean.getBoolean("org.ogema.app.timeseries.viewer.expert.gui.addprovideridtoheader"))
					text = HEADER_TEXT + " ("+getPageParameter(req, page, ScheduleViewerBasicApp.PARAM_PROVIDER_ID)+")";
				else
					text = HEADER_TEXT;
				setText(text, req);
			}
			
		};
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		
		/** TODO: Check if snippet can really be global. Otherwise caching via alreadyAdded will probably not
		 * work for a new session with a known providerId.*/
		PageSnippet snippet = new PageSnippet(page, "snippet", true) {

			private static final long serialVersionUID = 6159453362312862893L;
			// providerId -> list of configIds
			//private final Map<String, Set<String>> alreadyAdded = new HashMap<>();
			//private final Map<String, BasicScheduleViewBuilder> builders = new HashMap<>();

			@Override
			public void onGET(OgemaHttpRequest req) {
				/*String providerId = getPageParameter(req, page, ScheduleViewerBasicApp.PARAM_PROVIDER_ID);
				String configId =  getPageParameter(req, page, ScheduleViewerBasicApp.PARAM_SELECTION_CONFIG_ID);
							
				synchronized (alreadyAdded) {
					if(providerId == null) {
						providerId = DummyScheduleViewerConfigurationProvider.CONFIGURATION_PROVIDER_ID;
					}
					Set<String> ids = alreadyAdded.get(providerId);
					if(ids == null || (!ids.contains(providerId))) {	
						ScheduleViewerConfigurationProvider provider = app.getScheduleProviders().get(providerId);
						if(provider == null) {
							return;
						}
						try {
							BasicScheduleViewBuilder builder = new BasicScheduleViewBuilder(page, provider, app, req);						
							append(builder.getScheduleViewer(), req);
							if(ids == null) alreadyAdded.put(providerId, new HashSet<String>(Arrays.asList(new String[] {configId})));
							else ids.add(configId);
							//alreadyAdded.add(providerId);
							//builders.put(providerId, builder);
						}catch (Exception e) {
							e.printStackTrace();
						}						
						
					}
				}*/
							 
			}
		};
		BasicScheduleViewBuilder builder = new BasicScheduleViewBuilder(page, app);
		
		snippet.append(header, null);
		snippet.append(builder.getScheduleViewer(), null);
		page.append(snippet);
		page.showOverlay(true);

	}
	

	
	public static String getPageParameter(OgemaHttpRequest req, WidgetPage<?> page, String paramName) {
		String param = null;
		try {
			param= page.getPageParameters(req).get(paramName)[0];
		}catch(Exception e) {
			
		}
		return param;
	}


}
