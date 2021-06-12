package org.smartrplace.app.timeseries.viewer.expert.minmaxtable;

import de.iwes.widgets.reswidget.scheduleviewer.clone.DefaultSchedulePresentationData;

public class DefaultSchedulePresentationDataPlus {
	public final DefaultSchedulePresentationData data;
	public final long startTime;
	public final long endTime;
	
	public DefaultSchedulePresentationDataPlus(DefaultSchedulePresentationData data, long startTime, long endTime) {
		this.data = data;
		this.startTime = startTime;
		this.endTime = endTime;
	}
}
