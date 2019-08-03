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
package com.iee.app.evaluationofflinecontrol.gui.element;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.timeseries.eval.eventlog.util.EventLogParserUtil;
import org.smartrplace.os.util.ZipUtil;

import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;

public abstract class RemoteSlotsDBBackupButton extends Button {
	private static final long serialVersionUID = 1L;
	protected final static String basePathStr = System.getProperty("org.smartrplace.analysis.backup.parser.basepath");
	protected final static Path basePath = Paths.get(basePathStr!=null?basePathStr:"");

	public static final long MINUTE_MILLIS = 60000;
	public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
	public static final long DAY_MILLIS = 24 * HOUR_MILLIS;
	public static final Path REMOTE_SLOTS_BACKUP_DESTINATION_PATH = Paths.get("../evaluationresults/remoteSlotsBackup.zip");

	//Override these
	protected Path getBasePath(OgemaHttpRequest req) {
		return basePath;
	}
	protected abstract IntervalConfiguration getInterval(OgemaHttpRequest req);
	protected abstract List<String> getGWIds(OgemaHttpRequest req);
	
	public RemoteSlotsDBBackupButton(WidgetPage<?> page, String id, String text) {
		super(page, id, text);
	}
			
	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		final List<String> gwIDs = getGWIds(req);
		IntervalConfiguration itv = getInterval(req);
		final long startTime;
		final long endTime;
		if(itv.multiStart == null || itv.multiStart.length > 0) {
			startTime = itv.start;
			endTime = itv.end;
		} else {
			startTime = itv.multiStart[0];
			endTime = itv.multiEnd[itv.multiStart.length-1];
		}
		
		Path dest = REMOTE_SLOTS_BACKUP_DESTINATION_PATH;
		performSlotsBackup(null, dest, startTime, endTime, gwIDs, null, false);
		
		/*List<File> inputFiles = new ArrayList<>();
		int tsOverallNum = 0;
		int logFileOverallNum = 0;
		for(String currentGwId: gwIDs) {
    		Path gwdir = basePath.resolve(currentGwId);

    		//Resource Zip file
    		File zipFile = lastFileModified(gwdir.toFile(), "generalBackup");
    		if(zipFile != null) inputFiles.add(zipFile);
    		
    		//First slotsDB
    		Path logDir = gwdir.resolve("slotsdb");
			File[] logFiles = logDir.toFile().listFiles(File::isDirectory);
			//long lastFileTime = -1;
			int tsNum = 0;
			if(logFiles != null) for(File lgz: logFiles) {
				//long fileTime = lgz.lastModified();
				Path gzFile = lgz.toPath();
				Long dayStartFile = getSlotsFolderTime(gzFile);
				if(dayStartFile == null) {
					continue;
				}
				long dayEndFile = lgz.lastModified() + DAY_MILLIS;
				if((dayEndFile <= startTime || dayStartFile >= endTime)) continue;
				//Collection<File> dirFiles dirFiles = FileUtils.listFiles(lgz, null, true);
				Collection<File> dirFiles = FileUtils.listFiles(lgz, FileFileFilter.FILE,
						TrueFileFilter.INSTANCE);
				inputFiles.addAll(dirFiles);
				tsNum += dirFiles.size();
			}
			
    		//Now event log files
    		logDir = gwdir.resolve("logs");
			logFiles = logDir.toFile().listFiles();
			//long lastFileTime = -1;
			int logFileNum = 0;
			if(logFiles != null) for(File lgz: logFiles) {
				//long fileTime = lgz.lastModified();
				Path gzFile = lgz.toPath();
				Long dayStartFile = EventLogParserUtil.getDayStartOfLogFile(gzFile);
				if(dayStartFile == null) {
					continue;
				}
				long dayEndFile = AbsoluteTimeHelper.getNextStepTime(dayStartFile, AbsoluteTiming.DAY);
				if((dayEndFile <= startTime || dayStartFile >= endTime)) continue;
				//lastFileTime = fileTime;
				inputFiles.add(lgz);
				logFileNum++;
			}
			tsOverallNum += tsNum;
			logFileOverallNum += logFileNum;
			System.out.println("Exported data for Gw "+currentGwId+" tsNum:"+tsNum+" logFiles:"+logFileNum);
		}
		Path topPath = basePath;
		Path dest = Paths.get("../evaluationresults/remoteSlotsBackup.zip");
		ZipUtil.compress(dest, inputFiles, topPath);
		System.out.println("Exported total tsNum:"+tsOverallNum+" logNum:"+logFileOverallNum+" to "+dest.toString());*/
	}
	
	/** Backup slotsDB into zip-File
	 * 
	 * @param sourcePath for single gateway backup provide path upper to slotsDB here (e.g ogema/data)
	 * @param destination
	 * @param startTime
	 * @param endTime
	 * @param gwIDs for single gateway just provide a list of size 1
	 * @param generalBackupSource path to directory where zip-file with resources is located
	 */
	public static void performSlotsBackup(Path sourcePath, Path destination,
			long startTime, long endTime, List<String> gwIDs, File generalBackupSource,
			boolean useFolderTimeOnly) {
		List<File> inputFiles = new ArrayList<>();
		int tsOverallNum = 0;
		int logFileOverallNum = 0;
		for(String currentGwId: gwIDs) {
    		Path gwdir;
    		if(sourcePath != null)
    			gwdir = sourcePath;
    		else
    			gwdir = basePath.resolve(currentGwId);

    		//Resource Zip file
    		File zipFile = null;
    		if(generalBackupSource != null) {
    			zipFile = lastFileModified(generalBackupSource, "generalBackup");    			
    		} else {
    			zipFile = lastFileModified(gwdir.toFile(), "generalBackup");
    		}
			if(zipFile != null) inputFiles.add(zipFile);
    		
    		//First slotsDB
    		Path logDir = gwdir.resolve("slotsdb");
			File[] logFiles = logDir.toFile().listFiles(File::isDirectory);
			//long lastFileTime = -1;
			int tsNum = 0;
			if(logFiles != null) for(File lgz: logFiles) {
				//long fileTime = lgz.lastModified();
				Path gzFile = lgz.toPath();
				Long dayStartFile = getSlotsFolderTime(gzFile);
				if(dayStartFile == null) {
					continue;
				}
				if(useFolderTimeOnly) {
					if((dayStartFile < startTime || dayStartFile > endTime)) continue;
				} else {
					long dayEndFile = lgz.lastModified() + DAY_MILLIS;
					if((dayEndFile <= startTime || dayStartFile >= endTime)) continue;
				}
				//Collection<File> dirFiles dirFiles = FileUtils.listFiles(lgz, null, true);
				Collection<File> dirFiles = FileUtils.listFiles(lgz, FileFileFilter.FILE,
						TrueFileFilter.INSTANCE);
				inputFiles.addAll(dirFiles);
				tsNum += dirFiles.size();
			}
			
    		//Now event log files
    		logDir = gwdir.resolve("logs");
			logFiles = logDir.toFile().listFiles();
			//long lastFileTime = -1;
			int logFileNum = 0;
			if(logFiles != null) for(File lgz: logFiles) {
				//long fileTime = lgz.lastModified();
				Path gzFile = lgz.toPath();
				Long dayStartFile = EventLogParserUtil.getDayStartOfLogFile(gzFile);
				if(dayStartFile == null) {
					continue;
				}
				long dayEndFile = AbsoluteTimeHelper.getNextStepTime(dayStartFile, AbsoluteTiming.DAY);
				if((dayEndFile <= startTime || dayStartFile >= endTime)) continue;
				//lastFileTime = fileTime;
				inputFiles.add(lgz);
				logFileNum++;
			}
			tsOverallNum += tsNum;
			logFileOverallNum += logFileNum;
			System.out.println("Exported data for Gw "+currentGwId+" tsNum:"+tsNum+" logFiles:"+logFileNum);
		}
		Path topPath = basePath;
		ZipUtil.compress(destination, inputFiles, topPath);
		System.out.println("Exported total tsNum:"+tsOverallNum+" logNum:"+logFileOverallNum+" to "+destination.toString());
		
	}
	
	public static Long getSlotsFolderTime(Path folder) {
		try {
			String folderName = folder.getFileName().toString();
			long time = Long.parseLong(folderName);
			if(time < 50000000) {
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
				try {
				    Date parsed = format.parse(folderName);
				    return parsed.getTime();
				} catch (ParseException pe) {
				    System.out.println("ERROR1: Cannot parse \"" + folderName+ "\"");
				    return null;
				}
			} else
				return time;
		} catch(NumberFormatException e) {
			return null;
		}
	}
	
	/** Returns last file modified directly in directory (not recursive)*/
	public static File lastFileModified(File dir, String startString) {
	    File[] files = dir.listFiles(new FileFilter() {          
	        public boolean accept(File file) {
	            if(!file.isFile()) return false;
	            return (startString == null) || file.getName().startsWith(startString);
	        }
	    });
	    long lastMod = Long.MIN_VALUE;
	    File choice = null;
	    if(files != null) for (File file : files) {
	        if (file.lastModified() > lastMod) {
	            choice = file;
	            lastMod = file.lastModified();
	        }
	    }
	    return choice;
	}
}
