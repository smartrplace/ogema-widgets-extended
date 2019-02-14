package org.ogema.tools.app.logtransfercontrol.model;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.action.Action;
import org.ogema.model.alignedinterval.AlignedTimeIntervalLength;
import org.ogema.model.prototypes.Data;
	
/** Information on log data transferred from client to master. The log data itself usually is transferred
 * via the OGEMA file transfer machanism (or e.g. SCP) in the RecordedData file format.
*/
public interface DataLogTransferInfo extends Data {
	/**Client resource is not replicated in resource structure of master, so we just give the resource
	 * location on client here*/
	StringResource clientLocation();
	
	/** 0: no logging activated anymore<br>
	 *  1: fixed interval<br>
	 *  2: on value change<br>
	 *  3: on value update
	 * @return
	 */
	IntegerResource currentStorageType();
	
	/** only relevant for currentStorageType == 1*/
	TimeResource fixedInterval();
	
	/**Information on housekeeping on the client may be given here. This does not imply that this should
	 * be used for housekeeping on the master - typically the master should keep especially data the
	 * client may delete early.*/
	HousekeepingConfig remoteHouseKeeping();
	
	/** Note that each transfer requires sending the entire data for the current day. So usually the transfer
	 * interval should be set to the standard duration of one day. If the same value is written again into
	 * this field a single transfer is initiated immediately.
	 * @deprecated not used any more
	 */
	@Deprecated
	AlignedTimeIntervalLength transferInterval();
	TimeResource lastTransferTime();
	Action checkOldData();
	/**True if old data was checked and no interruption was detected*/
	BooleanResource checkedOldData();
	/**Each resource in this list shall have the name of the date directory that was sent and the timestamp when it
	 * was sent. This list is maintained by the client, but may be also edited by the master e.g. when a date
	 * shall be resent or when some dates have been transferred offline
	 */
	ResourceList<TimeResource> datesSentCompletely();

	TransmissionStorageControl transmissionStorageControl();
}