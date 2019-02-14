/**
 * ﻿Copyright 2014-2018 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
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
package de.iwes.timeseries.eval.api.semaextension.variant;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Period;

import de.iwes.timeseries.eval.garo.api.base.GaRoTimeSeriesId;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.CSVArchiveExporter;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GatewayDataExportI;

public class GatewayDataExportUtil {

	/**Exports selected data rows to standard sema-zip-file structure.
	 * TODO: For now uses standard 1-minute step size
	 * @param file
	 * @param gatewayIds
	 * @param timeSeriesToExport Note: As timeseries id also the values
	 * 		ElectricityPoints, SemaLevelElectricity, HeatingPoints, SemaLevelHeating shall be supported
	 * @param startTime
	 * @param endTime
	 * @param output
	 * @throws IOException
	 */
	public static void writeGatewayDataArchive(FileOutputStream file,
			Collection<GaRoTimeSeriesId> timeSeriesToExport,
            long startTime, long endTime,
            GatewayDataExportI gde) throws IOException {
        Map<String, List<String>> timeSeriesSelection = new HashMap<>();
        timeSeriesToExport.forEach(tse -> {
            timeSeriesSelection.computeIfAbsent(tse.gwId,
                    __ -> {
                        return new ArrayList<>();
                    }).add(tse.timeSeriesId);
        });
        //System.out.printf("evaluation done, exporting %d time series%n", timeSeriesEvalAll.size());
        //System.out.printf("selected %s time series from %s gateways%n", timeSeriesEvalAll.size(), timeSeriesSelection.size());
        //writeGatewayDataArchive(timeSeriesSelection, null, null, Period.ZERO, null);
        //String outputFileName = "evaluation-output-test.zip";
        DateTime start = new DateTime(startTime);
        DateTime end = new DateTime(endTime);
        Period step = new Period("PT60s");
        gde.writeGatewayDataArchive(timeSeriesSelection.keySet(),
                (gwId, rdId) -> {return timeSeriesSelection.getOrDefault(gwId, Collections.emptyList()).contains(rdId);},
                start, end, step, file);
        //Path of = Paths.get(outputFileName);
        //System.out.printf("export done, %dkb in %s%n", Files.size(of)/1024, of);
 		
	}
	
	public static class CSVArchiveExporterGDE implements CSVArchiveExporter {

		public CSVArchiveExporterGDE(GatewayDataExportI gatewayDataExport) {
			this.gde = gatewayDataExport;
		}

        private final GatewayDataExportI gde;

		@Override
		public void writeGatewayDataArchive(FileOutputStream file, Collection<GaRoTimeSeriesId> timeSeriesToExport,
				long startTime, long endTime) throws IOException {
			GatewayDataExportUtil.writeGatewayDataArchive(file, timeSeriesToExport, startTime, endTime,gde);	
		}
		
	}
}
