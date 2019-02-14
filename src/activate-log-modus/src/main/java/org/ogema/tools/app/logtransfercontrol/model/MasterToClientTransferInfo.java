package org.ogema.tools.app.logtransfercontrol.model;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Data;
	
/** Information to be transferred from master to client. Project-specific extensions are possible.
*/
public interface MasterToClientTransferInfo extends Data {
	/**If set the client shall transfer most recent log data up to the size (in kB) specified
	 * here. After completion the client will set this to zero
	 */
	IntegerResource transferMaxKb();

	/**Note: For compatbility reasons the pullInterval is the connectionInterval in the GatewayTransferInfo*/
	//TimeResource pushInterval();
	
	/**Notify client that datalogs up to this time can be deleted from the list of transferred dates. As we cannot store
	 * the date name in the name of the element of the resource list we provide it separatly here.*/
	//TimeResource allDataLogsTransferredUpToHere();
	StringResource allDataLogsTransferredUpToHereDateName();
}