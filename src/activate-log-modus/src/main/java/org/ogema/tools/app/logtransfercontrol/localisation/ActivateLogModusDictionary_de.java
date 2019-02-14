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

public class ActivateLogModusDictionary_de implements ActivateLogModusDictionary {

	@Override
	public OgemaLocale getLocale() {
		return OgemaLocale.GERMAN;
	}

	@Override
	public String header() {
		return "Aktivieren des Logmodus";
	}

	@Override
	public String description() {
		return "Auf dieser Seite kannst du einstellen, für welche deiner angelernten Geräte die Daten protokolliert werden sollen."
				+ " Dabei ist es möglich, das protokollieren für Thermostate, Temperatur(/Luftfeuchtigkeits)-Sensoren,"
				+ " Fensterkontakten, Funksteckdosen und Bewegungsmeldern, an/aus zu schalten. "
				+ " Ebenfalls lässt sich das Übertragen der protokollierten Daten an den Server an und ausschalten.";
	}

	//Constant	
//	@Override
//	public String path() {
//		return "Pfad der Ressource";
//	}
//
//	@Override
//	public String loggingActive() {
//		return "Datenprotokollierung der Ressourcendaten";
//	}
//
//	@Override
//	public String transmitActive() {
//		return "Datenübertragung an Server";
//	}

	
}
