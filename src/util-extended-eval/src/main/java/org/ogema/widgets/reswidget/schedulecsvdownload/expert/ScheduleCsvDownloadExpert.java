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
package org.ogema.widgets.reswidget.schedulecsvdownload.expert;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.security.WebAccessManager;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.timeseries.implementations.TreeTimeSeries;
import org.slf4j.LoggerFactory;

import de.iwes.widgets.api.services.NameService;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.calendar.datepicker.Datepicker;
import de.iwes.widgets.html.schedulemanipulator.ScheduleRowTemplate;
import de.iwes.widgets.resource.timeseries.OnlineTimeSeries;
import de.iwes.widgets.reswidget.schedulecsvdownload.ScheduleCsvDownload;
import de.iwes.widgets.reswidget.scheduleviewer.api.SchedulePresentationData;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilterExtended;

public class ScheduleCsvDownloadExpert<T extends ReadOnlyTimeSeries> extends ScheduleCsvDownload<T> {
	private static final long serialVersionUID = 4246352034235133637L;

	public ScheduleCsvDownloadExpert(WidgetPage<?> page, String id, WebAccessManager wam) {
		super(page, id, wam);
	}
	
	public ScheduleCsvDownloadExpert(WidgetPage<?> page, String id, WebAccessManager wam, Alert alert) {
		super(page, id, wam, null, true, null, null);
	}
	
	public ScheduleCsvDownloadExpert(WidgetPage<?> page, String id, WebAccessManager wam, Alert alert, 
			boolean showUserInput, Datepicker startPicker, Datepicker endPicker) {
		super(page, id, wam, alert, showUserInput, startPicker, endPicker);
	}
	
	@Deprecated //remove as soon as new release of widget-experimental is available
	public static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };
	/**
	 * 
	 * @param start
	 * @param end
	 * @param schedules
	 * @param tempFolder
	 * @param filters may be null
	 * @param exportJSON if false CSV is exported
	 * @return
	 * @throws IOException 
	 */
	@Deprecated //remove as soon as new release of widget-experimental is available
	public static <S extends ReadOnlyTimeSeries> Path exportFile(long start, long end,
			List<S> schedules, Path tempFolder, String zipBaseName, boolean exportJSON,
			List<TimeSeriesFilterExtended> filters,
			Integer schedCountReportInterval) throws IOException {
		if (!Files.exists(tempFolder))
			Files.createDirectories(tempFolder);
		final Path base = Files.createTempDirectory(tempFolder, zipBaseName);

		if (start > end) {
			return null;
		}
		final Path zipFile = tempFolder.resolve(base.getFileName() + ".zip");
        final URI uri = URI.create("jar:" + zipFile.toUri());
        int i = 0;
        try (final FileSystem zipfs = FileSystems.newFileSystem(uri, Collections.singletonMap("create", "true"))) {
			int schedCount = 0;
			int nextReportCount = (schedCountReportInterval!=null)?schedCountReportInterval:-1;
        	for (S rd : schedules) {
				final String id;
				if (rd instanceof RecordedData)
					id = ((RecordedData) rd).getPath();
				else if (rd instanceof Schedule)
					id = ((Schedule) rd).getPath();
				else if (rd instanceof SchedulePresentationData)
					id = ((SchedulePresentationData) rd).getLabel(OgemaLocale.ENGLISH);
				else if (rd instanceof OnlineTimeSeries)
					id = ((OnlineTimeSeries) rd).getResource().getPath();
				else if (rd instanceof TreeTimeSeries)
					id = "TreeTimeSeries_" + i++;
				else
					id = "_" + new BigInteger(65, new Random()).toString(32);
				String formatId = System.getProperty("org.ogema.widgets.schedulecsvdownload.formatid");
				if(formatId != null && formatId.contains("HUMREAD")) {
					if(id.contains("TEMPERATURE")|| id.contains("temperatureSensor") || ScheduleRowTemplate.isTemperatureSchedule(rd))
						formatId += "CELSIUS";
				}
				
				String fileFormat = System.getProperty("org.ogema.widgets.schedulecsvdownload.fileformat");
				String filename;
				if(filters != null && fileFormat != null && fileFormat.equals("FULL")) {
					filename = getShortLabel(rd, filters, null, null);
					filename = filename.replace("TemperatureRoomSensor", "TempRS");
				} else
					filename = id.replace("/", "%2F");
				for (char c : ILLEGAL_CHARACTERS) {
					filename = filename.replace(c, '_');
				}
				
				if(exportJSON) {
					writeValuesToFile(base, start, end, zipfs, rd, filename+".json", formatId);
				} else {
					writeValuesToFile(base, start, end, zipfs, rd, filename+".csv", formatId);
				}
				schedCount++;
				if(schedCountReportInterval != null && schedCount > nextReportCount) {
					System.out.println("Exported "+schedCount+" files, now:"+id);
					nextReportCount = schedCount + schedCountReportInterval;
				}
			}
			
		}
		FileUtils.deleteDirectory(base.toAbsolutePath().toFile());
        return zipFile;			
	}
	
	/** Get short Label for display, file names etc.
	 * 
	 * @param schedule
	 * @param filters filters to use. Usually a single filter should provide the short name
	 * @param nameService may be null
	 * @param locale may be null
	 * @return
	 */
	@Deprecated //remove as soon as new release of widget-experimental is available
	public static String getShortLabel(ReadOnlyTimeSeries schedule,
			List<TimeSeriesFilterExtended> filters, NameService nameService, OgemaLocale locale) {
		for(TimeSeriesFilterExtended filter : filters) {
			if(filter.accept(schedule)) {
				return filter.shortName(schedule);
			}
		}
		
		if(schedule instanceof SchedulePresentationData) {
			return ((SchedulePresentationData) schedule).getLabel(locale);
		}
		
		if (schedule instanceof Schedule) {
			if (nameService != null) {
				String name = nameService.getName((Schedule) schedule, locale);
				if (name != null) {
					return name;
				}
			}
			return ResourceUtils.getHumanReadableName((Schedule) schedule);
		}
		if (schedule instanceof RecordedData) {
			return ((RecordedData) schedule).getPath();
		}
		for(TimeSeriesFilterExtended filter : filters) {
			if(filter.accept(schedule)) {
				return filter.longName(schedule);
			}
		}
		
		throw new IllegalArgumentException("Could not determine schedule label for time series " + schedule +
				". Please provide a Long Name.");
		
	}
	
	@Deprecated //remove as soon as new release of widget-experimental is available
	private static <S extends ReadOnlyTimeSeries> void writeValuesToFile(final Path base, final long start, final long end, final FileSystem zipfs, S rd,
			String filename, String formatId) throws IOException {
		Path file = base.resolve(filename);
		if(filename.endsWith(".csv")) {
			toCsvFile(rd.iterator(start,end), file, formatId);
		}else if(filename.endsWith(".json")) {
			toJSONFile(rd.iterator(start,end), file, filename.replace(".json", ""));
		}else if(filename.endsWith(".python")) {
			//TODO: Does this option make sense?
			return;
		}
		Path pathInZipfile = zipfs.getPath("/" + filename);          
		// copy a file into the zip file
		Files.move(file, pathInZipfile, StandardCopyOption.REPLACE_EXISTING );
	}
	
	@Deprecated //remove as soon as new release of widget-experimental is available
	private static void toCsvFile(Iterator<SampledValue> values, Path path, String formatId) throws IOException {
		final Locale locale;
		final boolean celsius = (formatId != null) && formatId.contains("CELSIUS");
		if(formatId != null && formatId.contains("DE")) locale = Locale.GERMANY;
		else locale = null;
		//TODO: Support also other fixed steps and interpolation / averaging
		final boolean fixStepMinute = (formatId != null) && formatId.contains("FIXmm");
		
		SimpleDateFormat date = null;
		if(formatId != null && formatId.contains("#TS#")) {
			String[] els = formatId.split("#TS#");
			if(els.length == 3) {
				String dateFormat = els[1];
				if(locale != null)
					date = new SimpleDateFormat(dateFormat, locale);
				else
					date = new SimpleDateFormat(dateFormat);
			}
		}
		try (final Writer writer = new BufferedWriter(new FileWriter(path.toFile()))) {
			SampledValue sv;
			String lastVal = null;
			Long writeTimeStamp = null;
			long nextTimeStamp = -1;
			while (values.hasNext()) {
				try {
					sv = values.next();
				} catch (NoSuchElementException e) { // does in fact occur, because slots database may be corrupt (not ordered). We better skip those.
					LoggerFactory.getLogger(ScheduleCsvDownload.class).error("Trying to read corrupt time series data. Maybe timestamps are not ordered: " + path);
					return;
				}
				final float value;
				if(celsius) value = sv.getValue().getFloatValue() - 273.15f;
				else value = sv.getValue().getFloatValue();
				final String val;
				if(locale != null) val = String.format(locale, "%.3f", value);
				else val = String.format("%.3f", value);
				
				if(fixStepMinute) {
					if(writeTimeStamp == null) {
						writeTimeStamp = sv.getTimestamp()-60000l;
						lastVal = val;
					}
					nextTimeStamp = sv.getTimestamp();
					while(writeTimeStamp < nextTimeStamp) {
						writeCSVLine(writer, writeTimeStamp, date, lastVal);
						writeTimeStamp += 60000l;
					}
					lastVal = val;
				} else {
					writeCSVLine(writer, sv.getTimestamp(), date, val);
				}
			}
			writer.flush();
		}
	}
	
	@Deprecated //remove as soon as new release of widget-experimental is available
	private static void writeCSVLine(Writer writer, long timeStamp, SimpleDateFormat date, String val)
			throws IOException {
		if(date == null)
			writer.write(timeStamp + ";" + val + "\n");
		else
			writer.write(date.format(new Date(timeStamp)) + ";" + val + "\n");			
	}
	
	@Deprecated //remove as soon as new release of widget-experimental is available
	private static void toJSONFile(Iterator<SampledValue> values, Path path,String name) throws IOException {
		try (final Writer writer = new BufferedWriter(new FileWriter(path.toFile()))) {
			SampledValue sv;
			SortedMap<Long, Float> map = new TreeMap<>();
			while (values.hasNext()) {
				try {
					sv = values.next();
				} catch (NoSuchElementException e) { // does in fact occur, because slots database may be corrupt (not ordered). We better skip those.
					LoggerFactory.getLogger(ScheduleCsvDownload.class).error("Trying to read corrupt time series data. Maybe timestamps are not ordered: " + path);
					return;
				}
				map.put(sv.getTimestamp(), sv.getValue().getFloatValue());
			}
			final JSONObject json = new JSONObject();
			json.put(name, map);
			writer.write(json.toString()); 
			writer.flush();
		}
	}
}
