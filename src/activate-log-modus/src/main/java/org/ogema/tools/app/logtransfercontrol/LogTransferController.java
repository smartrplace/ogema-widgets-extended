package org.ogema.tools.app.logtransfercontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.model.actors.MultiSwitch;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.Sensor;
import org.ogema.tools.app.logtransfercontrol.model.DataLogTransferInfo;
import org.ogema.tools.resource.util.LoggingUtils;
import org.smartrplace.tissue.util.resource.ResourceHelperSP;
import org.smartrplace.tissue.util.resource.ValueResourceHelperSP;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ResourceHelper.DeviceInfo;

public class LogTransferController {
//    public final static Collection<Class<? extends Sensor>> supportedSensorTypes = Arrays.<Class<? extends Sensor>>asList(
//            Sensor.class //i.e. all of them, replace with concrete subclasses if necessary
//    );

    private final ApplicationManager appMan;
	private final OgemaLogger logger;
	private final ResourceList<DataLogTransferInfo> dataLogs;

	public LogTransferController(ApplicationManager appMan, ResourceList<DataLogTransferInfo> dataLogs) {
		this.appMan = appMan;
		this.logger = appMan.getLogger();
		this.dataLogs = Objects.requireNonNull(dataLogs);
	}
	
	public ResourceList<DataLogTransferInfo> getDataLogs() {
		return dataLogs;
	}
	
	public ApplicationManager getAppMan() {
		return appMan;
	}
	
	public Stream<SingleValueResource> getLoggableResources() {
		return getLoggableResources(appMan);
	}
	
	static Stream<SingleValueResource> getLoggableResources(final ApplicationManager appMan) {
		return appMan.getResourceAccess().getResources(PhysicalElement.class).stream()
				.flatMap(LogTransferController::getLoggableResourcesForDevice);
		}
	
	
	/**
	 * Returns a list that may contain virtual resources
	 * @param device
	 * @return
	 */
	static Stream<SingleValueResource> getLoggableResourcesForDevice(final PhysicalElement device) {
		if (device instanceof Thermostat) {
			final Thermostat t = (Thermostat) device;
			return Stream.of(
					t.valve().setting().stateFeedback(),
					t.temperatureSensor().settings().setpoint(),
					t.temperatureSensor().deviceFeedback().setpoint(),
					t.temperatureSensor().reading());
    	} else if (device instanceof OnOffSwitch) {
    		final OnOffSwitch sw = (OnOffSwitch) device;
    		return Stream.of(sw.stateFeedback(), sw.stateControl());
    	} else if (device instanceof MultiSwitch) {
    		final MultiSwitch m = (MultiSwitch) device;
    		return Stream.of(m.stateFeedback(), m.stateControl());
        } else if (device instanceof Sensor) {
        	final Sensor s = (Sensor) device;
        	return s.reading() instanceof SingleValueResource ? Stream.of((SingleValueResource) s.reading()) :Stream.empty();
//       } else if (device instanceof SingleSwitchBox) { // already included in OnOffSwitches 
		} 
		return Stream.empty();
	}
	
	public static class DevData {
		String name;
		//PhysicalElement resource;
		long lastValue = -1;
		int size = 0;
		int numLogged = 0;
		int numTransferred = 0;
		int numTotal = 0;
		
		List<SingleValueResource> resources = new ArrayList<>();
	}
	
	public Map<String, DevData> getDevices(boolean getLastValue) {
		Stream<SingleValueResource> resC = getLoggableResources();
		Map<String, DevData> devices = new HashMap<>();
		for (SingleValueResource res: resC.collect(Collectors.toList())) {
			org.smartrplace.tissue.util.resource.ResourceHelperSP.DeviceInfo devInfo = ResourceHelperSP.getDeviceInformation(res, false);
			final String devName;
			if(devInfo != null)
				devName = devInfo.getDeviceName();
			else
				devName = res.getLocation();
			DevData d = devices.get(devName);
			if(d == null) {
				d = new DevData();
				d.name = devName;
				devices.put(devName, d);
			}
			d.resources.add(res);
			
			if(getLastValue) {
				d.numTotal++;
				if(LoggingUtils.isLoggingEnabled(res)) d.numLogged++;
				if(isResourceTransferred(res)) d.numTransferred++;

				RecordedData rec = ValueResourceHelperSP.getRecordedData(res);
				if(rec == null) continue;
				SampledValue sv = rec.getPreviousValue(Long.MAX_VALUE);
				
				//FIXME: just for testing
				int sizeLoc = rec.size();
				Iterator<SampledValue> it = rec.iterator();
				if((it.hasNext()) && (sizeLoc == 0)) {
					logger.error("This should never occur and may cause performance issues: Data in ReadOnlyTimeries obtainable via iterator, but size zero!");
					int count = 0;
					while(it.hasNext()) {
						count++;
						sv = it.next();
					}
					//System.out.println("size:"+sizeLoc+", but iterator has "+count+" elements");
					sizeLoc = count;
				}
				d.size += sizeLoc;
				
				//System.out.println("Found "+sizeLoc+" values for resource "+res.getLocation());
				
				if(sv == null) continue;
				long myVal = sv.getTimestamp();
				if(myVal > d.lastValue) d.lastValue = myVal;
			}
		}
		return devices;
	}	
	
	@Deprecated
	public DevData getDevice(String shortId, boolean getLastValue) {
		Map<String, DevData> devs = getDevices(getLastValue);
		for(Entry<String, DevData> d: devs.entrySet()) {
			int len = d.getKey().length();
			if(len < 4) continue;
			String toTest;
			if(d.getKey().charAt(len-2) == '_') {
				if(len < 6) continue;
				toTest = d.getKey().substring(len-6, len-2);
			} else toTest = d.getKey().substring(len-4);
			if(toTest.equals(shortId)) return d.getValue();
		}
		return null;
	}
	
	@Deprecated //not really tested
	public List<String> getDevices() {
		List<SingleValueResource> resC = getLoggableResources().collect(Collectors.toList());
		Set<String> devices = new HashSet<>();
		for(SingleValueResource res: resC) {
			DeviceInfo devInfo = ResourceHelper.getDeviceInformation(res);
			if(devInfo != null) devices.add(devInfo.getDeviceName());
			else devices.add(res.getLocation());
		}
		List<String> result = new ArrayList<>(devices);
		result.sort(null);
		return result ;
	}
	
	public boolean isResourceTransferred(SingleValueResource resource) {
		final DataLogTransferInfo log = getDti(resource);
		return (log != null && log.isActive());
	}
	
	public boolean startTransmitLogData(SingleValueResource resource) {
		DataLogTransferInfo log = getDti(resource);
		if (log != null) {
			if (!log.isActive())
				log.activate(true);
			return false;
		}
		log = dataLogs.add();
		StringResource clientLocation = log.clientLocation().create();
		clientLocation.setValue(resource.getPath());
	
		// not used any more
		/*
		TimeIntervalLength tLength = log.transferInterval().timeIntervalLength().create();
		IntegerResource type = tLength.type().create();
		type.setValue(10);
		*/
		log.activate(true);
		return true;
	}
	
	public boolean stopTransmitLogData(SingleValueResource resource) {
		final DataLogTransferInfo log = getDti(resource);
		if(log == null) 
			return false;
		log.delete();
		return true;
	}
	
    private final DataLogTransferInfo getDti(final SingleValueResource resource) {
		final String path = resource.getPath();
		return appMan.getResourceAccess().getResources(DataLogTransferInfo.class).stream()
				.filter(dl -> path.equals(dl.clientLocation().getValue()))
				.findAny()
				.orElse(null);
    }

}
