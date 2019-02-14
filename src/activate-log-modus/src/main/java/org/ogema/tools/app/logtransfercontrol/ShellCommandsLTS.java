package org.ogema.tools.app.logtransfercontrol;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.app.logtransfercontrol.LogTransferController.DevData;
import org.ogema.tools.resource.util.LoggingUtils;
import org.ogema.tools.resource.util.TimeUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/*
@Component(specVersion = "1.2")
@Properties( { 
	@Property(name = "osgi.command.scope", value = "logmodus"),
	@Property(name = "osgi.command.function", value = { "devicesfortransfer", "transferallsensors", "starttransfer", "stoptransfer" }) 
})
@Service(ShellCommandsLTS.class)
*/
// FIXME these commands should take the resource path as argument instead of some funny four-digit device id
@Descriptor("Log transfer commands")
class ShellCommandsLTS {
	
	private final LogTransferController app;
	private final ServiceRegistration<ShellCommandsLTS> ownService;
	
	public ShellCommandsLTS(LogTransferController app, BundleContext ctx) {
		this.app = app; 
		final Dictionary<String, Object> props = new Hashtable<>();
    	props.put("osgi.command.scope", "logmodus");
		props.put("osgi.command.function", new String[] {"devicesfortransfer", "transferallsensors", "starttransfer", "stoptransfer"});
		this.ownService = ctx.registerService(ShellCommandsLTS.class, this, props);

	}

	void close() {
		ForkJoinPool.commonPool().submit(ownService::unregister);
	}
	
    @Descriptor("show all devices configured by log-transfer-control and last time a value was received")
    public void devicesfortransfer() throws IOException {
    try {
        Map<String, DevData> devdata = app.getDevices(true);
        for(DevData dev: devdata.values()) {
        	System.out.println("   "+dev.name+getStatus(dev)+" received last value on "+TimeUtils.getDateAndTimeString(dev.lastValue));
        }
    } catch(Exception e) {
    	e.printStackTrace();
    }
    }

    private String getStatus(DevData dev) {
    	return "("+dev.numLogged+" / "+dev.numTransferred+" / "+dev.numTotal+")";
    }
    
    @Descriptor("send new log data now (for testing purposes, blocks while searching for files)")
    public void transferallsensors() throws IOException {
    	starttransfer(null);
    }
    
    // WTF four digit device id???
    @Descriptor("Start logging for a device")
/*
    public void starttransfer(String shortId) throws IOException {
    	DevData dd = app.getDevice(shortId, false);
    	if(dd == null) System.out.println(shortId+" not found");
    	else {
    		for(SingleValueResource res: dd.resources) {
    			LoggingUtils.activateLogging(res, -2);
    			app.startTransmitLogData(res);
    		}
    		System.out.println("Started transfer and logging for "+dd.resources.size()+" resources.");
    	}
    }
   */
    public void starttransfer(
    		@Parameter(names= {"-p", "--path"}, absentValue="")
    		@Descriptor("The resource path. If absent, all applicable resources will be logged")
    		String path) throws IOException {
    	final Stream<SingleValueResource> svr;
    	if (path != null && !path.isEmpty()) {
    		final Resource r = app.getAppMan().getResourceAccess().getResource(path);
    		if (r instanceof SingleValueResource)
    			svr = Stream.of((SingleValueResource) r);
    		else if (r instanceof PhysicalElement)
    			svr = LogTransferController.getLoggableResourcesForDevice((PhysicalElement) r);
    		else
    			svr = Stream.empty();
    	} else {
    		svr = app.getLoggableResources();
    	}
    	final AtomicInteger cnt = new AtomicInteger(0);
    	svr.forEach(resource -> {
    		LoggingUtils.activateLogging(resource, -2);
    		if (app.startTransmitLogData(resource))
    			cnt.getAndIncrement();
    	});
  		System.out.println("Started transfer and logging for " + cnt.get() + " resources.");
    }

    @Descriptor("Stop logging for a device.")
    public void stoptransfer(
    		@Parameter(names= {"-p", "--path"}, absentValue="")
    		@Descriptor("The resource path. If absent, all applicable resources will be logged")
    		String path) throws IOException {
    	final Stream<SingleValueResource> svr;
    	if (!path.isEmpty()) {
    		final Resource r = app.getAppMan().getResourceAccess().getResource(path);
    		if (r instanceof SingleValueResource)
    			svr = Stream.of((SingleValueResource) r);
    		else if (r instanceof PhysicalElement)
    			svr = LogTransferController.getLoggableResourcesForDevice((PhysicalElement) r);
    		else
    			svr = Stream.empty();
    	} else {
    		svr = app.getLoggableResources();
    	}
    	final AtomicInteger cnt = new AtomicInteger(0);
    	svr.forEach(resource -> {
    		LoggingUtils.deactivateLogging(resource);
    		if (app.stopTransmitLogData(resource))
    			cnt.getAndIncrement();
    	});
  		System.out.println("Stopped transfer and logging for " + cnt.get() + " resources.");
    }
}
