package org.ogema.tools.app.logtransfercontrol.model;

import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Data;
	
/** Data for configuration of transmission of SlotsDB, backup data and similar that are transmitted via
 * File Upload
*/
public interface FileTransmissionTaskData extends Data {
	/**Maximum number of trials to connect until each start of the action/task is stopped if transmission is not successful.
	 */
	IntegerResource maxRetry();
	/**Default value is to wait one hour until the next attempt to connect is made*/
	TimeResource retryInterval();
	/**To avoid sending all client gateways at the same time, set inidividual delays that should all be within the
	 * retryInterval span to let all systems make their initial attempt before the first system start a retry
	 */
	TimeResource initialDelay();
	
	/** <=now: Server is assumed to be available<br>
	 *  >now: Server could not be reached by other connection task, so transmissions with not high priority should 
	 *  not try to connect at all
	 */
	TimeResource serverUnavailableUntil();
}