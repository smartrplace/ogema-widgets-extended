package org.smartrplace.eval.hardware;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmInterfaceInfo;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmLogicInterface;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.util.extended.eval.widget.IntegerMultiButton;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.resource.GatewaySyncUtil;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.ButtonData;

public class HmCCUPageUtils {
	protected static final Map<String, Timer> timers = new HashMap<>();
	
	public static IntegerMultiButton addTechInModeButton(InstallAppDevice object, HmInterfaceInfo device,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req,
			Row row,
			ApplicationManager appMan, HardwareInstallConfig hwConfig,
			Alert alert) {
		@SuppressWarnings({ "unchecked", "serial" })
		IntegerMultiButton teachInMode = new IntegerMultiButton(vh.getParent(), "techInMode"+id, true, req,
				new WidgetStyle[] {ButtonData.BOOTSTRAP_ORANGE, ButtonData.BOOTSTRAP_GREEN, ButtonData.BOOTSTRAP_ORANGE, ButtonData.BOOTSTRAP_RED,
						ButtonData.BOOTSTRAP_LIGHTGREY}) {
			@Override
			protected void setState(int state, OgemaHttpRequest req) {
				setTeachInState(state, device, hwConfig, appMan);
				/*boolean stateControl = (state==2)||(state==3);
				Resource logicIfRaw = device.getLocationResource().getParent();
				if(logicIfRaw == null || (!(logicIfRaw instanceof HmLogicInterface)))
					return;
				if(stateControl) {
					createTimer(device, (HmLogicInterface) logicIfRaw, appMan, hwConfig);
				}
				((HmLogicInterface) logicIfRaw).installationMode().stateControl().setValue(stateControl);*/
				
				String remoteGatewayOfCcu = GatewaySyncUtil.getGatewayBaseIdIfRemoteDevice(device.getLocationResource());
				if(remoteGatewayOfCcu != null) {
					alert.showAlert("MQTT-Replicator nach dem Anlernen neu starten, um Geräte sichtbar zu machen!", false,
							2*TimeProcUtil.MINUTE_MILLIS, req);
				}
			}
			
			@Override
			protected String getText(int state, OgemaHttpRequest req) {
				String minutes;
				String remoteGatewayOfCcu = GatewaySyncUtil.getGatewayBaseIdIfRemoteDevice(device.getLocationResource());
				if(hwConfig.techInModeDuration().exists())
					minutes = String.format("%.1f", hwConfig.techInModeDuration().getValue());
				else
					minutes = "30";
				switch(state) {
				case 2:
				case 3:
					Timer t = timers.get(device.getLocation());
					if(t != null) {
						long remaining = t.getNextRunTime()-t.getExecutionTime();
						if(remoteGatewayOfCcu != null)
							return "!!MQTT-Repl on ("+StringFormatHelper.getFormattedValue(remaining)+")";
						return "on ("+StringFormatHelper.getFormattedValue(remaining)+")";
					}
					return "on !NO TIMER!";
				case 4:
					return "No TechIn Control";
				default:
					if(remoteGatewayOfCcu != null)
						return "!!MQTT-Repl off ("+minutes+")";
					return "off ("+minutes+")";
				}
			}
			
			@Override
			protected int getState(OgemaHttpRequest req) {
				return getTeachInState(device);
				/*Resource logicIfRaw = device.getLocationResource().getParent();
				if(logicIfRaw == null || (!(logicIfRaw instanceof HmLogicInterface)))
					return 4;
				HmLogicInterface logicIf = (HmLogicInterface) logicIfRaw;
				boolean isDone = logicIf.installationMode().stateFeedback().getValue() == logicIf.installationMode().stateControl().getValue();
				int state = (logicIf.installationMode().stateControl().getValue()?2:0)+(isDone?1:0);
				return state;*/
			}
			
			@Override
			protected int getNextState(int prevstate, OgemaHttpRequest req) {
				if(prevstate == 4)
					return 4;
				if(prevstate >= 2)
					return 0;
				return 2;
			}
		};
		teachInMode.isPolling = true;
		teachInMode.registerDependentWidget(teachInMode);
		teachInMode.setPollingInterval(DeviceTableRaw.DEFAULT_POLL_RATE, req);
		if(alert != null)
			teachInMode.registerDependentWidget(alert);
		row.addCell("TeachIn", teachInMode);
		return teachInMode;
	}
	
	/** Get CCU teach-in state
	 * @param device
	 * @return 0=waiting for teach-in off confirmation, 1: off confirmed, 2: waiting for teach-in on confirmation,
	 *	 3: on confirmed, 4: no-teach-in (e.g. CCU set inactive)
	 */
	public static int getTeachInState(HmInterfaceInfo device) {
		Resource logicIfRaw = device.getLocationResource().getParent();
		if(logicIfRaw == null || (!(logicIfRaw instanceof HmLogicInterface)))
			return 4;
		HmLogicInterface logicIf = (HmLogicInterface) logicIfRaw;
		boolean isDone = logicIf.installationMode().stateFeedback().getValue() == logicIf.installationMode().stateControl().getValue();
		int state = (logicIf.installationMode().stateControl().getValue()?2:0)+(isDone?1:0);
		return state;
	}

	public static int getTeachInStateCheckOthers(HmInterfaceInfo device, DatapointService dpService) {
		int baseResult = getTeachInState(device);
		if(baseResult > 1)
			return baseResult;
		Collection<InstallAppDevice> allCCU = dpService.managedDeviceResoures("org.smartrplace.app.drivermonservice.devicehandler.HomematicCCUHandler", false);
		for(InstallAppDevice ccu: allCCU) {
			if(!(ccu.device() instanceof HmInterfaceInfo))
				continue;
			int otherState = getTeachInState((HmInterfaceInfo) ccu.device());
			if((otherState != 1) && (otherState != 4))
				return -1;
		}
		return baseResult;
	}
	
	public static void setTeachInState(int state, HmInterfaceInfo device,
			HardwareInstallConfig hwConfig, ApplicationManager appMan) {
		boolean stateControl = (state==2)||(state==3);
		Resource logicIfRaw = device.getLocationResource().getParent();
		if(logicIfRaw == null || (!(logicIfRaw instanceof HmLogicInterface)))
			return;
		if(stateControl) {
			createTimer(device, (HmLogicInterface) logicIfRaw, appMan, hwConfig);
		}
		((HmLogicInterface) logicIfRaw).installationMode().stateControl().setValue(stateControl);		
	}
	
	public static float getEffectiveMinutesCCUTeachIn(HardwareInstallConfig hwConfig) {
		if(hwConfig.techInModeDuration().exists())
			return hwConfig.techInModeDuration().getValue();
		else
			return 30f;
	}
	protected static Timer createTimer(HmInterfaceInfo device, HmLogicInterface logicIf, ApplicationManager appMan,
			HardwareInstallConfig hwConfig) {
		float minutes = getEffectiveMinutesCCUTeachIn(hwConfig);
		return createTimer(device, logicIf, minutes, appMan);
	}
	public static Timer createTimer(HmInterfaceInfo device, HmLogicInterface logicIf, float minutes,
			ApplicationManager appMan) {
		Timer t = timers.get(device.getLocation());
		if(t != null) {
			t.destroy();
		}
		t = appMan.createTimer((long) (minutes*TimeProcUtil.MINUTE_MILLIS), new TimerListener() {
			
			@Override
			public void timerElapsed(Timer timer) {
				logicIf.installationMode().stateControl().setValue(false);
			}
		});
		timers.put(device.getLocation(), t);
		return t;
	}
	
	public static String addClientUrl(HmInterfaceInfo device, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, Row row) {
		StringResource clientUrlRes = device.getSubResource("clientUrl", StringResource.class);
		if(clientUrlRes.exists()) {
			String fullVal = clientUrlRes.getValue();
			String showVal;
			if(fullVal.startsWith("http://"))
				showVal = fullVal.substring("http://".length());
			else
				showVal = fullVal;
			if(showVal.endsWith(":2001")||showVal.endsWith(":2010"))
				showVal = showVal.substring(0,  showVal.length()-5);
			vh.stringLabel("clientUrl", id, showVal, row);
			return showVal;
		}
		return null;
	}
}
