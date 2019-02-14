package org.ogema.tools.app.logtransfercontrol;

import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.ResourceDemandListener;
import org.ogema.tools.app.logtransfercontrol.gui.PageBuilder;
import org.ogema.tools.app.logtransfercontrol.localisation.ActivateLogModusDictionary;
import org.ogema.tools.app.logtransfercontrol.localisation.ActivateLogModusDictionary_de;
import org.ogema.tools.app.logtransfercontrol.localisation.ActivateLogModusDictionary_en;
import org.ogema.tools.app.logtransfercontrol.model.GatewayTransferInfo;
import org.ogema.tools.resource.util.LoggingUtils;
import org.osgi.framework.BundleContext;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;

/**
 * Configure resource logging and log data transfer to server
 */
@Component(specVersion = "1.2")
@Service(Application.class)
public class LogTransferModusApp implements Application, ResourceDemandListener<GatewayTransferInfo> {
	
	private static final String PROPERTY_LOG_ALL_RESOURCES = "de.iee.ogema.activatelogmodus.logAllResources";
	private static final String PROPERTY_TRANSFER_ALL_RESOURCES = "de.iee.ogema.activatelogmodus.transmitAllLogData";
	/**
	 * Only relevant if {@link #PROPERTY_LOG_ALL_RESOURCES} is "true"
	 */
	private static final String PROPERTY_LOG_ALL_DELAY = "de.iee.ogema.activatelogmodus.logAllDelayMs";
	private static final String PROPERTY_TRANSMIT_ALL_DELAY = "de.iee.ogema.activatelogmodus.transmitAllDelayMs";
	
	private static final long DEFAULT_LOG_ALL_DELAY = 4 * 60 * 1000;
	private static final long DEFAULT_TRANSMIT_ALL_DELAY = 5 * 60 * 1000;
	
	private static final String urlPath = "/de/iwes/ogema/apps/activatelogtransfermodus";

	private ApplicationManager appMan;
    private WidgetApp widgetApp;
    private ShellCommandsLTS shellCommands;
    private Timer allLoggingTimer;
    private Timer allTransferTimer;
    
    private LogTransferController controller;
    private BundleContext ctx;
	
    @Reference
	private OgemaGuiService guiService;
    
    @Activate
    protected void activate(BundleContext ctx) {
    	this.ctx = ctx;
    }
	
 	@Override
    public void start(final ApplicationManager appMan) {
 		this.appMan = appMan;
    	List<GatewayTransferInfo> gwtis = appMan.getResourceAccess().getResources(GatewayTransferInfo.class);
    	//This app will present the first GatewayTransferInfo in the UI.
    	if (gwtis.size() > 1) 
    		appMan.getLogger().error("More than one element of type GatewayTransferInfo on system, just using first element!");
    	if (!gwtis.isEmpty())
    		startInternal();
    	appMan.getResourceAccess().addResourceDemand(GatewayTransferInfo.class, this);
    	// we only check this once on startup, but this should be fine
        if (Boolean.getBoolean(PROPERTY_LOG_ALL_RESOURCES)) {
        	final long delay = getPropOrMin(PROPERTY_LOG_ALL_DELAY, DEFAULT_LOG_ALL_DELAY, 10000);
        	this.allLoggingTimer = appMan.createTimer(delay, t -> { // wait for 4 minutes after start... then all relevant reosurces should be available
        		t.destroy();
        		appMan.getLogger().info("Activating logging for all resources");
        		LogTransferController.getLoggableResources(appMan)
        			.filter(r -> r.getResourceType() != StringResource.class)
        			.forEach(r -> LoggingUtils.activateLogging(r, -2));
        	});
        }
        
    }
 	
 	private void startInternal() {
        final List<GatewayTransferInfo> infos = appMan.getResourceAccess().getResources(GatewayTransferInfo.class);
        final GatewayTransferInfo gtiSelected = infos.stream()
        	.filter(gti -> gti.dataLogs().size() > 0)
        	.findAny().orElse(infos.stream().findAny().orElse(null));
        if (gtiSelected != null) {
	    	controller = new LogTransferController(appMan, gtiSelected.dataLogs().create());
	        if (Boolean.getBoolean(PROPERTY_TRANSFER_ALL_RESOURCES)) {
	        	final long delay = getPropOrMin(PROPERTY_TRANSMIT_ALL_DELAY, DEFAULT_TRANSMIT_ALL_DELAY, 10000);
	        	this.allTransferTimer = appMan.createTimer(delay, t -> {
	        		t.destroy();
	        		appMan.getLogger().info("Activating log transfer for all logged resources");
	        		appMan.getResourceAccess().getResources(SingleValueResource.class).stream()
	        			.filter(r -> r.getResourceType() != StringResource.class)
	        			.filter(LoggingUtils::isLoggingEnabled)
	        			.forEach(controller::startTransmitLogData);
	        	});
	        }
	    	shellCommands = new ShellCommandsLTS(controller, ctx); 
	        widgetApp = guiService.createWidgetApp(urlPath, appMan);
	    	final WidgetPage<ActivateLogModusDictionary> page = widgetApp.createStartPage();
	    	page.registerLocalisation(ActivateLogModusDictionary_de.class).registerLocalisation(ActivateLogModusDictionary_en.class);
	    	new PageBuilder(page, appMan, gtiSelected.dataLogs(), controller);
        }
 	}

 	@Override
 	public void resourceAvailable(GatewayTransferInfo gti) {
 		if (controller == null || !controller.getDataLogs().equalsLocation(gti.dataLogs())) {
	 		stopInternal();
	 		startInternal();
 		}
 	}
 	
 	@Override
 	public void resourceUnavailable(GatewayTransferInfo gti) {
 		if (controller != null && controller.getDataLogs().equalsLocation(gti.dataLogs())) {
	 		stopInternal();
	 		startInternal();
 		}
 	}
 	
 	private void stopInternal() {
 		if (widgetApp != null) {
    		widgetApp.close();
        }
 		widgetApp = null;
 		if (shellCommands != null)
 			shellCommands.close();
 		shellCommands = null;
    	if (allTransferTimer != null)
    		allTransferTimer.destroy();
    	allTransferTimer = null;
 	}
 	
	@Override
    public void stop(AppStopReason reason) {
    	stopInternal();
    	appMan.getResourceAccess().removeResourceDemand(GatewayTransferInfo.class, this);
    	if (allLoggingTimer != null)
    		allLoggingTimer.destroy();
    	allLoggingTimer = null;
    }
	
	private static long getPropOrMin(final String property, final long defaultVal, final long minValue) {
		final long l = Long.getLong(property, defaultVal);
		return l >= minValue ? l : minValue;
	}
	
}