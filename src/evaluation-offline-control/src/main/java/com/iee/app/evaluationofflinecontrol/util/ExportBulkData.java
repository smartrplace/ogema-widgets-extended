package com.iee.app.evaluationofflinecontrol.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.ogema.core.application.ApplicationManager;
import org.ogema.externalviewer.extensions.IntervalConfiguration;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.TimeSeriesDataOffline;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

public class ExportBulkData {
	/** If not specified otherwise usually all requirements defined within an option have to be fulfilled (AND linking).
	 * All ComplexOptionDescriptions given via #getComplexOptionsExtended are evalulated and all results shall
	 * be used, so these represent an OR linking of the conditions*/
	public static class ComplexOptionDescription {
		/** The pathElement shall found within the path of the time series to be relevant for the plotType. It can also be a special
		 * String starting with # e.g. for manual time series*/
		public String pathElement = null;
		public GaRoDataTypeI type = null;
		public ComplexOptionDescription(String pathElement) {
			this.pathElement = pathElement;
		}
		public ComplexOptionDescription(GaRoDataTypeI type) {
			this.type = type;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj == null)
				return false;
			if(!(obj instanceof ComplexOptionDescription))
				return false;
			ComplexOptionDescription objc = (ComplexOptionDescription) obj;
			if(pathElement != null) {
				if(!pathElement.equals(objc.pathElement))
					return false;
			} else if(objc.pathElement != null)
				return false;
			if(type != null) {
				if(!type.id().equals(objc.type.id()))
					return false;
			} else if(objc.type != null)
				return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int prime = 31;
			return prime + Objects.hash(pathElement, type == null ? null : type.id());
			//return prime + (pathElement == null ? 0 : 11*pathElement.hashCode())
			//		+ (type == null ? 0 : 7*type.id().hashCode());
		}
	}

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
	public static void cleanList(List<TimeSeriesData> input, Collection<ComplexOptionDescription> inputsToUse) {
		List<TimeSeriesData> toRemove = new ArrayList<>();
		for (TimeSeriesData tsdBase : input) {
			String id = tsdBase.label(null);
			boolean found = false;
			for(ComplexOptionDescription use: inputsToUse) {
				if(use.pathElement != null) {
					if(id.contains(use.pathElement)) {
						found = true;
						break;
					}
				} else {
					GaRoDataType tsdType = GaRoEvalHelper.getDataType(id);
					if(use.type.equals(tsdType)) {
						found = true;
						break;
					}
				}
			}

			if(!found)
				toRemove.add(tsdBase);
		}
		input.removeAll(toRemove);
	}
	
	public static String getDeviceShortId(String location) {
		String[] parts = location.split("/");
		if(parts.length < 3) return "?S?";
		if(!(parts[0].toLowerCase().startsWith("homematic") ||
				parts[0].toLowerCase().startsWith("homematicip")))
			return "?X?";
		if(!parts[1].equals("devices")) return "?Y?";
		if(parts[2].length() < 5) return parts[2];
		return parts[2].substring(parts[2].length()-4);	
	}

}
