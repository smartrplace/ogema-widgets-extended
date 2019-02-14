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
/**
 * Copyright 2009 - 2016
 *
 * Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Fraunhofer IWES
 *
 * All Rights reserved
 */
package org.ogema.app.timeseries.viewer.expert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.ogema.app.timeseries.viewer.expert.dummyProvider.DummyScheduleViewerConfigurationProvider;
import org.ogema.app.timeseries.viewer.expert.gui.MainPage;
import org.ogema.app.timeseries.viewer.expert.gui.OnlineDataViewerPage;
import org.ogema.app.timeseries.viewer.expert.gui.ScheduleProviderOverviewPage;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.recordeddata.DataRecorder;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;
import de.iwes.widgets.resource.timeseries.OnlineTimeSeriesCache;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider;

@Component(specVersion = "1.2")
@Service(Application.class)
@References({
	@Reference(name = "sources", 
			referenceInterface = ScheduleViewerConfigurationProvider.class, 
			cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, 
			policy = ReferencePolicy.DYNAMIC, 
			bind = "addScheduleProvider", 
			unbind = "removeScheduleProvider"), 
	}
)
public class ScheduleViewerBasicApp implements Application { 

    private OgemaLogger logger;
    private WidgetApp wApp;
    private WidgetPage<?> page;
    public ApplicationManager appManager;
    public MainPage mainPage;
	protected final Map<String, ScheduleViewerConfigurationProvider> scheduleProviders = new HashMap<String, ScheduleViewerConfigurationProvider>();
    public static final String URL_PATH = "/de/iwes/tools/schedule/viewer-basic-example";
    public static final String PARAM_PROVIDER_ID = "providerId";
    public static final String PARAM_SELECTION_CONFIG_ID = "configId";
	
    
    @Reference
    private OgemaGuiService guiService;
    
    @Reference
    private DataRecorder dataRecorder;
    
    @Reference
    private OnlineTimeSeriesCache timeSeriesCache;
        

	@Override
    public void start(ApplicationManager appManager) {
    	this.appManager = appManager;
        this.logger = appManager.getLogger();
        logger.info("{} started", getClass().getName());
        
        wApp = guiService.createWidgetApp(URL_PATH, appManager);
        page = wApp.createWidgetPage("index.html",true);
        mainPage = new MainPage(page, appManager, this);       

        final WidgetPage<?> onlinePage = wApp.createWidgetPage("onlineData.html");
        final WidgetPage<?> overviewPage = wApp.createWidgetPage("overviewPage.html");
        new OnlineDataViewerPage(onlinePage, timeSeriesCache, appManager);
        new ScheduleProviderOverviewPage(overviewPage, appManager, this);
        
        	// navigation menu
        NavigationMenu customMenu = new NavigationMenu(" Select page");
		customMenu.addEntry("View schedules", page);
		customMenu.addEntry("View online data", onlinePage);
		customMenu.addEntry("View ScheduleProvider Overivew", overviewPage);
		MenuConfiguration mc = page.getMenuConfiguration();
		mc.setCustomNavigation(customMenu);
		mc = onlinePage.getMenuConfiguration();
		mc.setCustomNavigation(customMenu);
		
		mc = overviewPage.getMenuConfiguration();
		mc.setCustomNavigation(customMenu);
		Boolean testRes = Boolean.getBoolean("org.ogema.apps.createtestresources");
		if (testRes) {
			createTestResource();
		}
		
		DummyScheduleViewerConfigurationProvider dummyProvider = new DummyScheduleViewerConfigurationProvider(appManager);
		addScheduleProvider(dummyProvider);
	
    }	

    @Override
    public void stop(AppStopReason reason) {
        if (wApp != null)
        	wApp.close();    	
    	logger.info("{} stopped", getClass().getName());
    	logger = null;
    	wApp = null;
    	appManager = null;
    }

   
    private void createTestResource()  {
    	TemperatureSensor fl = appManager.getResourceManagement().createResource("scheduleViewerBasicTestResource", TemperatureSensor.class);
    	fl.reading().program().create();
    	Room room = appManager.getResourceManagement().createResource("testRoom", Room.class);
    	fl.location().room().setAsReference(room);
    	long t0 = appManager.getFrameworkTime();
    	List<SampledValue> values = new ArrayList<>();
    	int nrValues = 10;
    	for (int i=0; i< nrValues; i++) {
    		float value = (float) Math.sin(2*Math.PI * i/nrValues);
    		SampledValue sv= new SampledValue(new FloatValue(value), t0 + i* 10*60*1000, Quality.GOOD);
    		values.add(sv);
    	}
    	fl.reading().program().addValues(values);
    	fl.reading().program().activate(false);
    	room.activate(false);
    }

	public DataRecorder getDataRecorder() {
		return dataRecorder;
	}

	public void addScheduleProvider(ScheduleViewerConfigurationProvider provider) { 
		scheduleProviders.put(provider.getConfigurationProviderId(), provider);		
	}

	public void removeScheduleProvider(ScheduleViewerConfigurationProvider provider) {
		scheduleProviders.remove(provider.getConfigurationProviderId());
	}
	
	public Map<String, ScheduleViewerConfigurationProvider> getScheduleProviders(){
		return scheduleProviders;
	}
	


}
