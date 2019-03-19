/**
 * ﻿Copyright 2018-2019 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ogema.timeseries.eval.garo.dp.csv.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.timeseries.eval.garo.dp.csv.GaRoMultiEvalDataProviderCSV1;
import org.ogema.tools.timeseriesimport.api.ImportConfiguration;
import org.ogema.tools.timeseriesimport.api.TimeseriesImport;

import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;

public class CSVFileReader {
	protected final Path sourceDir;
	protected final GatewayTimeseriesData destination;
	protected final ImportConfiguration importConfig;
	protected final TimeseriesImport csvImport;
	
	protected boolean dataReadDone = false;
	
	public CSVFileReader(Path sourceDir, GatewayTimeseriesData destination, ImportConfiguration importConfig,
			TimeseriesImport csvImport) {
		this.sourceDir = sourceDir;
		this.destination = destination;
		this.importConfig = importConfig;
		this.csvImport = csvImport;
	}

	public GatewayTimeseriesData getData() {
		return getData(false);
	}
	public GatewayTimeseriesData getData(boolean forceUpdate) {
		readData(forceUpdate);
		return destination;
	}
	
	public boolean readData() {
		return readData(false);
	}
	public boolean readData(boolean forceUpdate) {
		if((!forceUpdate) && dataReadDone) return false;
		if(forceUpdate && dataReadDone) {
			//TODO: Might require synchronization
			destination.roomData.clear();
		}
	    File[] files = sourceDir.toFile().listFiles(new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.toLowerCase().endsWith(".csv");
		    }
		});
	 
	    if(files == null) return false;
	    Arrays.sort(files);
	    for (final File fileEntry : files) {
	    	readSingleFile(fileEntry.toPath());
	    }

		dataReadDone = true;
		return true;
	}
	
	public void readSingleFile(Path source) {
		String fileName = source.getFileName().toString();
		if(!fileName.endsWith(".csv")) return;
		final String roomId;
		if(fileName.startsWith(GaRoMultiEvalDataProviderCSV1.ROOM_IDENT_STRING)) {
			String[] els = fileName.split("\\.", 2);
			if(els.length != 2) {
				throw new IllegalStateException("Filename "+source+" does not delimit room from timeseries id!");
			}
			roomId = els[0].substring(GaRoMultiEvalDataProviderCSV1.ROOM_IDENT_STRING.length());
			fileName = els[1];
		} else
			roomId = GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID;
		String tsId;
		fileName = fileName.substring(0, fileName.length()-4);
		int dotIdx = fileName.indexOf(".");
		if(dotIdx < 0)
			tsId = fileName;
		else
			tsId = fileName.substring(0, dotIdx);
		if(tsId.equals(GaRoMultiEvalDataProviderCSV1.MULTI_TS_IDENT_STRING))
			throw new UnsupportedOperationException("Not implemented yet: "+GaRoMultiEvalDataProviderCSV1.MULTI_TS_IDENT_STRING);
		try {
			ReadOnlyTimeSeries ts = csvImport.parseCsv(sourceDir, importConfig);
			destination.addTimeSeries(roomId, tsId, ts);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
