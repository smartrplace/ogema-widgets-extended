package org.ogema.tools.app.logtransfercontrol.model;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.prototypes.PhysicalElement;

/** This model is intended to be used for transfer of information from a Remote Supervision client to
 * a master e.g. by the RemoteRESTConnector. For this reason such a resource usually is available on
 * the client as well as on the master and is synchronized.<br>
 * Use {@link PhysicalElement#name()} for a human readable name 
 *  
 */
public interface GatewayTransferInfo extends PhysicalElement {
	
	TimeResource lastConnection();
	
	TimeResource initialConnection();
	
	/**
	 * This can be modified on both client and server side, and will
	 * cause the client to adapt its connection interval. In ms.
	 * Note: This is the pull interval, which is still here for compatibility reasons. The
	 * push interval is in MasterToClientTransferInfo
	 */
	TimeResource connectionInterval();
	
	IntegerResource connectionCounter();
	
	StringResource id();
	
	/**
	 * If the gateway is connected to a switch box, set a reference here 
	 */
	OnOffSwitch onOffSwitch();
		
	StringResource localIP();
	
	ResourceList<DataLogTransferInfo> dataLogs();
	/**Each resource in this list shall have the name of the date directory that was sent and the timestamp when it
	 * was sent. This list is maintained by the client, but may be also edited by the master e.g. when a date
	 * shall be resent or when some dates have been transferred offline.<br>
	 * Dates kept here shall not be stored in the datesSentCompletely field of individual gateways anymore in order
	 * to gain efficiency.
	 */
	@Deprecated
	ResourceList<TimeResource> datesSentCompletelyAllLogs();
	
	/**Data to be sent from master to client*/
	MasterToClientTransferInfo masterToClientData();
	TimeResource lastLogTransferTrial();
	TimeResource lastLogTransferSuccess();
	TimeResource lastLogTransferError();
	TimeResource lastBackupTransferTrial();
	TimeResource lastBackupTransferSuccess();
	TimeResource lastBackupTransferError();
	
	FileTransmissionTaskData fileTransmissionTaskData();
}