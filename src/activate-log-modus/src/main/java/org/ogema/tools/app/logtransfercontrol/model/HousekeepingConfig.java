package org.ogema.tools.app.logtransfercontrol.model;

import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Data;
	
/** TODO: make this real
*/
public interface HousekeepingConfig extends Data {
	TimeResource maxStorageTime();
	
}