package org.ogema.tools.app.logtransfercontrol.model;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Data;
	
/** Control of data transmission and storage on the master for RemoteSupervision. Note that the client usually holds
 * a private copy of this in a place where the master cannot write via RemoteRESTConnector and only accpets master changes in
 * the fields foreseen for master writing. The client may also not publish the entire information held in its private
 * resource copy.
*/
public interface TransmissionStorageControl extends Data {
	/**For {@link SingleValueTransferInfo}: If true the {@link SingleValueTransferInfo.value} is not updated anymore, so no more information is provided to
	 * master.<br>
	 * For {@link DataLogTransferInfo}: If true the transfer shall be interrupted. Afterwards only new data is transferred by default*/
	BooleanResource stopTransfer();
	/** This may be set to true by the master to indicate that no more information is required.*/
	BooleanResource stopTransferRequest();
	
	/**Information of the client until which time the transfer is allowed e.g. for support*/
	TimeResource performTransferUntil();
	/**The data transmission may be linked to support tickets and is stopped when all tickets in the
	 * list are closed.*/
	ResourceList<StringResource> supportTicketIds();
	
	/**This field can be set to true by the master if data sending is required there. It is up to the client, 
	 * though, whether the request is fulfilled*/
	BooleanResource masterRequest();
	
	/**The client may indicate here whether storing the data sent on the master is accepted. Otherwise the client
	 * expects the master not to store the data received persistently in a way that allows to assign the data to the
	 * client gateway or user.*/
	BooleanResource allowMasterLogging();
	/**The master may indicate here that the data received is logged and stored persistently.*/
	BooleanResource isMasterLoggingActive();
	
	/**This may be set by the master to indicate that data is required and used for billing*/
	BooleanResource isDataUserForBilling();
}