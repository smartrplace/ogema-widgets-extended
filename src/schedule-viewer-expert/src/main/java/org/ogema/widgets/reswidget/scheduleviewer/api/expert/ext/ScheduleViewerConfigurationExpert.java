package org.ogema.widgets.reswidget.scheduleviewer.api.expert.ext;

import java.util.List;
import java.util.Map;

import de.iwes.widgets.html.schedulemanipulator.ScheduleManipulatorConfiguration;
import de.iwes.widgets.reswidget.scheduleplot.api.TimeSeriesPlot;
import de.iwes.widgets.reswidget.scheduleviewer.api.ConditionalTimeSeriesFilter;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;

public class ScheduleViewerConfigurationExpert extends ScheduleViewerConfiguration {
	public final boolean doScale;	
	
	protected ScheduleViewerConfigurationExpert(boolean showManipulator, boolean showCsvDownload, boolean useNameService, boolean showOptionsSwitch, 
			ScheduleManipulatorConfiguration manipulatorConfiguration, boolean showNrPointsPreview, Long startTime, Long endTime, 
			List<Map<String,TimeSeriesFilter>> programs, List<Map<String,ConditionalTimeSeriesFilter<?>>> filters, Long bufferWindow,
			boolean showIndividualConfigPopup, boolean showIntervals, boolean showPlotTypeSelector, boolean downsamplingItv, boolean showUpdateInterval,
			Class<? extends TimeSeriesPlot> plotType, boolean loadSchedulesOnInit,
			//additional parameters for extension
			boolean doScale) {
		super(showManipulator,showCsvDownload, useNameService, showOptionsSwitch, 
				manipulatorConfiguration, showNrPointsPreview, startTime, endTime, 
				programs, filters, bufferWindow,
				showIndividualConfigPopup, showIntervals, showPlotTypeSelector, downsamplingItv, showUpdateInterval,
				plotType, loadSchedulesOnInit);
		this.doScale = doScale;
	}
	
	public boolean isDoScale() {
		return doScale;
	}

}
