package com.iee.app.evaluationofflinecontrol.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.externalviewer.extensions.IntervalConfiguration;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.TimeSeriesDataOffline;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

public class ExportBulkData {
	public static IntervalConfiguration getInterval(String config, ApplicationManager appMan) {
		if(config.equals("TestOneMonth")) {
			IntervalConfiguration r = new IntervalConfiguration();
			long now = appMan.getFrameworkTime();
			r.end = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.MONTH)-1;
			r.end = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -1, AbsoluteTiming.MONTH);
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -1, AbsoluteTiming.MONTH);
			return r;
		} else if(config.equals("Test From Feb-June")) {
			IntervalConfiguration r = new IntervalConfiguration();
			long now = appMan.getFrameworkTime();
			r.end = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.MONTH)-1;
			r.end = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -2, AbsoluteTiming.MONTH);
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -1, AbsoluteTiming.MONTH);
			return r;
		} else if(config.equals("Test from June-Sep")) {
			IntervalConfiguration r = new IntervalConfiguration();
			long now = appMan.getFrameworkTime();
			r.end = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.MONTH)-1;
			r.end = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -3, AbsoluteTiming.MONTH);
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -2, AbsoluteTiming.MONTH);
			return r;
		} else if(config.equals("Test from Oct-Feb")) {
			IntervalConfiguration r = new IntervalConfiguration();
			long now = appMan.getFrameworkTime();
			r.end = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.MONTH)-1;
			r.end = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -5, AbsoluteTiming.MONTH);
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -2, AbsoluteTiming.MONTH);
			return r;
		} else if(config.equals("Jan2018")) {
			IntervalConfiguration r = new IntervalConfiguration();
			r.start = 1541458800000l; //06.11.2018
			r.end = 1561932000000l; //01.07.2019
			return r;
		}
		return StandardConfigurations.getConfigDuration(config, appMan);
		
	}
	
	/** Clean a list of input from everything not wanted*/
	public static void cleanList(List<TimeSeriesData> input, boolean useHumidity, boolean useTemp, boolean useBattery) {
		List<TimeSeriesData> toRemove = new ArrayList<>();
		for (TimeSeriesData tsdBase : input) {
			if(!(tsdBase instanceof TimeSeriesDataOffline)) throw new IllegalStateException("getStartAndEndTime only works on TimeSeriesData input!");
			TimeSeriesDataOffline tsd = (TimeSeriesDataOffline) tsdBase;
			boolean found = false;
			if(tsd instanceof TimeSeriesDataExtendedImpl) {
				TimeSeriesDataExtendedImpl tse = (TimeSeriesDataExtendedImpl)tsd;
				if(tse.type instanceof GaRoDataTypeI) {
					String inputLabel = ((GaRoDataTypeI)tse.type).label(null);
					if(useHumidity && inputLabel.contains("Humidity"))
						found = true;
					if((!found) && useTemp && inputLabel.contains("Temperature"))
						found = true;
					if((!found) && useBattery && inputLabel.contains("ChargeVoltage"))
						found = true;
				}
			}
			if(!found)
				toRemove.add(tsdBase);
		}
		input.removeAll(toRemove);
	}
	/** Like {@link #cleanList(List, boolean, boolean, boolean)}, but specifiy arbitraty inputs
	 * 
	 * @param input list to clean input data rows from not specified in inputsToUse
	 * @param inputsToUse Strings to search for in timeseries labels
	 */
	public static void cleanList(List<TimeSeriesData> input, Collection<String> inputsToUse) {
		List<TimeSeriesData> toRemove = new ArrayList<>();
		for (TimeSeriesData tsdBase : input) {
			String id = tsdBase.label(null);
			boolean found = false;
			for(String use: inputsToUse) {
				if(id.contains(use)) {
					found = true;
					break;
				}
			}
			/*if(!(tsdBase instanceof TimeSeriesDataOffline)) throw new IllegalStateException("getStartAndEndTime only works on TimeSeriesData input!");
			TimeSeriesDataOffline tsd = (TimeSeriesDataOffline) tsdBase;
			boolean found = false;
			if(tsd instanceof TimeSeriesDataExtendedImpl) {
				TimeSeriesDataExtendedImpl tse = (TimeSeriesDataExtendedImpl)tsd;
				if(tse.type instanceof GaRoDataTypeI) {
					String inputLabel = ((GaRoDataTypeI)tse.type).label(null);
					for(String use: inputsToUse) {
						if(inputLabel.contains(use)) {
							found = true;
							break;
						}
					}
				}
			}*/
			if(!found)
				toRemove.add(tsdBase);
		}
		input.removeAll(toRemove);
	}
	
	public static String getDeviceShortId(String location) {
		String[] parts = location.split("/");
		if(parts.length < 3) return "?S?";
		if(!(parts[0].toLowerCase().equals("homematic") ||
				parts[0].toLowerCase().equals("homematicip")))
			return "?X?";
		if(!parts[1].equals("devices")) return "?Y?";
		if(parts[2].length() < 5) return parts[2];
		return parts[2].substring(parts[2].length()-4);	
	}

}
