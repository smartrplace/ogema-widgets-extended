package org.ogema.widgets.reswidget.scheduleviewer.api.expert.ext;

import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationBuilder;

public class ScheduleViewerConfigurationBuilderExpert extends ScheduleViewerConfigurationBuilder {
	protected ScheduleViewerConfigurationBuilderExpert() {
		super();
	}
	
	public static ScheduleViewerConfigurationBuilderExpert newBuilder() {
		return new ScheduleViewerConfigurationBuilderExpert();
	}

	private boolean doScale = true;
	
	public ScheduleViewerConfigurationBuilderExpert setDoScale(boolean doScale) {
		this.doScale = doScale;
		return this;
	}
	
	@Override
	public ScheduleViewerConfigurationExpert build() {
		return new ScheduleViewerConfigurationExpert(showManipulator, showCsvDownload, useNameService, showOptionsSwitch,
				manipulatorConfiguration, showNrPointsPreview, startTime, endTime, programs, filters, bufferWindow,
				showIndividualConfigBtn, showStandardIntervals, showPlotTypeSelector, downsamplingInterval, showUpdateInterval, plotType,
				loadSchedulesOnInit,
				//new parameters
				doScale);
	}
	
}
