/**
 * Copyright 2009 - 2016
 *
 * Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Fraunhofer IWES
 *
 * All Rights reserved
 */
package org.ogema.tools.app.logtransfercontrol.localisation;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class ActivateLogModusDictionary_en implements ActivateLogModusDictionary {

	@Override
	public OgemaLocale getLocale() {
		return OgemaLocale.ENGLISH;
	}

	@Override
	public String header() {
		return "Message forwarding configuration";
	}

	@Override
	public String description() {
		return "This page allows you to configure for which devices you want to activate/deactivate "
				+ "datalogging. It is possible to activate the datalogging for Thermstats, "
				+ "Temperature(/Humidity)-Sensors, DoorWindowSensors, Switchboxes and "
				+ "MotionSensors. It is also possible to activate/deactivate the transmission "
				+ "of the logdata to the server.";
	}

	//Constant
//	@Override
//	public String path() {
//		return "Path of resource";
//	}
//
//	@Override
//	public String loggingActive() {
//		return "Logging resourcedata";
//	}
//
//	@Override
//	public String transmitActive() {
//		return "Transmit logdata to server";
//	}

}
