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
package com.iee.app.evaluationofflinecontrol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;
import org.osgi.framework.BundleContext;

import com.iee.app.evaluationofflinecontrol.gui.ConfigurationPage;
import com.iee.app.evaluationofflinecontrol.gui.GatewayConfigPage;
import com.iee.app.evaluationofflinecontrol.gui.JSONResultPage;
import com.iee.app.evaluationofflinecontrol.gui.KPIPage;
import com.iee.app.evaluationofflinecontrol.gui.KPIPageGWOverview;
import com.iee.app.evaluationofflinecontrol.gui.KPIPageLastEvals;
import com.iee.app.evaluationofflinecontrol.gui.KPIShowAlgoAssessmentPage;
import com.iee.app.evaluationofflinecontrol.gui.MainPage;
import com.iee.app.evaluationofflinecontrol.gui.OfflineEvaluationControl;
import com.iee.app.evaluationofflinecontrol.gui.QueuePage;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.base.provider.BasicEvaluationProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GatewayBackupAnalysisAccess;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GatewayDataExportI;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.services.MessagingService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

/**
 * Template OGEMA application class
 */
@References({
	@Reference(
		name="evaluationProviders",
		referenceInterface=EvaluationProvider.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addEvalProvider",
		unbind="removeEvalProvider"),
	@Reference(
		name="dataProviders",
		referenceInterface=DataProvider.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addDataProvider",
		unbind="removeDataProvider"),
})
@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class OfflineEvaluationControlApp implements Application, OfflineEvalServiceAccess {
	public static final String urlPath = "/com/example/app/evaluationofflinecontrol";

    private OgemaLogger log;
    private ApplicationManager appMan;
    private OfflineEvaluationControlController controller;
    private ShellCommandsEOC shellCommands;
    private BundleContext ctx;
    
	public WidgetApp widgetApp;
	public NavigationMenu menu;

	@Reference
	private OgemaGuiService guiService;
	
	//@Reference
	//public EvaluationManager evalManager;
	
	//The two following references are both just required for CSV export
	/** Note: this is currently provided by gateway-data-export-v2 */
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    protected GatewayDataExportI gatewayDataExport;
	@Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
	public GatewayBackupAnalysisAccess gatewayParser;

	@Reference
	public EvalResultManagement evalResultMan;
	private volatile boolean initDone = false;
	List<GaRoSingleEvalProvider> earlyProviders = new ArrayList<>();
	
	public MainPage mainPage;
	public OfflineEvaluationControl offlineEval;
	
	private final Map<String,EvaluationProvider> evaluationProviders = Collections.synchronizedMap(new LinkedHashMap<String,EvaluationProvider>());
	private final Map<String,DataProvider<?>> dataProviders = Collections.synchronizedMap(new LinkedHashMap<String,DataProvider<?>>());
	
	public Map<String, EvaluationProvider> getEvaluations() {
		synchronized (evaluationProviders) {
			return new LinkedHashMap<>(evaluationProviders);
		}
	}
	public Map<String, DataProvider<?>> getDataProviders() {
		synchronized (dataProviders) {
			return new LinkedHashMap<>(dataProviders);
		}
	}

	public BasicEvaluationProvider basicEvalProvider = null;
	EvaluationProvider eval;
	
    @Activate
    protected void activate(BundleContext ctx) {
    	this.ctx = ctx;
    }

    /*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        appMan = appManager;
        log = appManager.getLogger();

        // 
		guiService.getMessagingService().registerMessagingApp(appMan.getAppID(), "Evaluation Control");
        controller = new OfflineEvaluationControlController(appMan, this, gatewayDataExport, null, false);
		
		//register a web page with dynamically generated HTML
		widgetApp = guiService.createWidgetApp(urlPath, appManager);
		WidgetPage<?> page = widgetApp.createStartPage();
		mainPage = new MainPage(page, controller);
		WidgetPage<?> page1 = widgetApp.createWidgetPage("OfflineEvaluationControl.html");
		offlineEval = new OfflineEvaluationControl(page1, controller);
		//WidgetPage<?> page2 = widgetApp.createWidgetPage("EvaluationResultOverview.html");
		//new EvaluationResultOverview(page2, controller);
		WidgetPage<?> page3 = widgetApp.createWidgetPage("autoKPIs.html");
		new KPIPage(page3, controller, evalResultMan);
		WidgetPage<?> page3b = widgetApp.createWidgetPage("allKPIs.html");
		new KPIPageLastEvals(page3b, controller, evalResultMan);
		WidgetPage<?> page3c = widgetApp.createWidgetPage("specialKPIs.html");
		//new KPIShowConfigurationPage(page3c, controller, evalResultMan);
		//WidgetPage<?> page3cc = widgetApp.createWidgetPage("specialAlgoKPIs.html");
		new KPIShowAlgoAssessmentPage(page3c, controller, evalResultMan);
		WidgetPage<?> page3d = widgetApp.createWidgetPage("multiGwKPIs.html");
		new KPIPageGWOverview(page3d, controller);
		WidgetPage<?> page4 = widgetApp.createWidgetPage("results.html");
		new JSONResultPage(page4, controller);
		WidgetPage<?> page5 = widgetApp.createWidgetPage("configuration.html");
		new ConfigurationPage(page5, controller);
		WidgetPage<?> page5b = widgetApp.createWidgetPage("configurationGws.html");
		new GatewayConfigPage(page5b, controller);
		WidgetPage<?> page6 = widgetApp.createWidgetPage("queue.html");
		new QueuePage(page6, controller, evalResultMan);
		
		menu = new NavigationMenu("Select Page");
		menu.addEntry("Evaluation Provider Overview", page);
		menu.addEntry("Offline Evaluation Control", page1);
		//menu.addEntry("Evaluation Result Overview", page2);
		menu.addEntry("Auto-Scheduling KPI Overview", page3);
		menu.addEntry("All Last Calculated KPIs", page3b);
		menu.addEntry("Special KPI Configurations", page3c);
		//menu.addEntry("Special Algo Assessment KPI Configurations", page3cc);
		menu.addEntry("Multi-Gateway Special KPI Configurations", page3d);
		menu.addEntry("Results Overview", page4);
		menu.addEntry("Configuration", page5);
		menu.addEntry("Queued Evaluation Runs", page6);
		
		MenuConfiguration mc = page.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = page.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = page1.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		//mc = page2.getMenuConfiguration();
		//mc.setCustomNavigation(menu);
		mc = page3.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = page3b.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = page4.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = page5.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = page5b.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = page6.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = page3c.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = page3d.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		//mc = page3cc.getMenuConfiguration();
		//mc.setCustomNavigation(menu);
		controller.registerExistingMultiPages();
		controller.registerBackupListeners();
		
		initDone = true;
		for(GaRoSingleEvalProvider p: earlyProviders) initGaroProvider(p);

    	shellCommands = new ShellCommandsEOC(controller, ctx); 
	}

     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	if (widgetApp != null) widgetApp.close();
 		if (shellCommands != null)
 			shellCommands.close();
 		shellCommands = null;
		if (controller != null)
    		controller.close();
        log.info("{} stopped", getClass().getName());
    }
    
    protected void addEvalProvider(EvaluationProvider provider) {
    	evaluationProviders.put(provider.id(), provider);
    	if(provider instanceof GaRoSingleEvalProvider) {
    		if(initDone) initGaroProvider((GaRoSingleEvalProvider) provider);
    		else earlyProviders.add((GaRoSingleEvalProvider) provider);
    	}
     	if((provider instanceof BasicEvaluationProvider)&&(basicEvalProvider == null)) {
    		basicEvalProvider = (BasicEvaluationProvider) provider;
    	}
    }
    
    private void initGaroProvider(GaRoSingleEvalProvider provider) {
   		evalResultMan.getEvalScheduler().registerEvaluationProvider(provider);
    }
    
    protected void removeEvalProvider(EvaluationProvider provider) {
    	evaluationProviders.remove(provider.id());
    	try {
    	if(provider instanceof GaRoSingleEvalProvider)
    		evalResultMan.getEvalScheduler().unregisterEvaluationProvider((GaRoSingleEvalProvider) provider);
    	} catch(NullPointerException e) {
    		//ignore
    	}
    }
    
    protected void addDataProvider(DataProvider<?> provider) {
    	dataProviders.put(provider.id(), provider);
    	//if(provider instanceof GaRoMultiEvalDataProvider) {
    	//	if(initDone) initGaroProvider((GaRoSingleEvalProvider) provider);
    	//	else earlyProviders.add((GaRoSingleEvalProvider) provider);
    	//}
    }
    protected void removeDataProvider(DataProvider<?> provider) {
    	dataProviders.remove(provider.id());
    	//if(provider instanceof GaRoSingleEvalProvider)
    	//	evalResultMan.getEvalScheduler().unregisterEvaluationProvider((GaRoSingleEvalProvider) provider);
    }
	@Override
	public WidgetApp getWidgetApp() {
		return widgetApp;
	}
	@Override
	public NavigationMenu getMenu() {
		return menu;
	}
	@Override
	public EvalResultManagement evalResultMan() {
		return evalResultMan;
	}
	@Override
	public GatewayBackupAnalysisAccess gatewayParser() {
		return gatewayParser;
	}

	public MessagingService messageService() {
		return guiService.getMessagingService();
	}
}
