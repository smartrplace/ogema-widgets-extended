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
package de.iwes.widgets.reswidget.scheduleviewer.clone;

import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.tools.resource.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.calendar.datepicker.Datepicker;
import de.iwes.widgets.html.calendar.datepicker.DatepickerData;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SessionConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SessionConfiguration.PreSelectionControllability;
import de.iwes.widgets.reswidget.scheduleviewer.utils.ScheduleViewerUtil;

public class ViewerDatepicker extends Datepicker {
	protected class ViewerDatepickerData extends DatepickerData {

		// overrides default date
		Long explicitDate = null;
		boolean fixedInterval = false;
		boolean init = true;
		//boolean initBug = true;

		public ViewerDatepickerData(ViewerDatepicker datepicker) {
			super(datepicker);
		}
	}

	private static final long serialVersionUID = 1L;
	private final boolean isStartDatepicker;

	private final ScheduleViewerExtended schedView;
	private final WidgetPage<?> page;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	public ViewerDatepicker(WidgetPage<?> page, String id, boolean isStartDatepicker,
			ScheduleViewerExtended schedView) {
		super(page, id);
		this.schedView = schedView;
		this.isStartDatepicker = isStartDatepicker;
		this.page = page;
	}

	@Override
	public ViewerDatepickerData createNewSession() {
		return new ViewerDatepickerData(this);
	}

	@Override
	public ViewerDatepickerData getData(OgemaHttpRequest req) {
		return (ViewerDatepickerData) super.getData(req);
	}

	@Override
	public void onGET(OgemaHttpRequest req) {

		//TODO: What is this needed for? => Moved down
		//if (schedView.configuration.showOptionsSwitch && schedView.optionsCheckbox.getCheckboxList(req).get(ScheduleViewerExtended.FIX_INTERVAL_OPT)) {
		//	return;
		//}
		final SessionConfiguration sessionConfig = schedView.getSessionConfiguration(req);

		synchronized(req) { if (getData(req).init) {
			ScheduleViewerConfiguration vc = sessionConfig.viewerConfiguration();
			Long[] vcTime = null;
			if(vc != null) {
				vcTime = new Long[2];
				vcTime[0] = vc.getStartTime();
				vcTime[1] = vc.getEndTime();
			}
			Long[] time;
			time = ScheduleViewerUtil.getStartEndTimeFromParameter(req, page);
			if(vcTime != null && vcTime[0] != null)
				time[0] = vcTime[0];
			if(vcTime != null && vcTime[1] != null)
				time[1] = vcTime[1];

			if (time[0] != null && time[1] != null) {

				if (isStartDatepicker) {
					setDate(time[0], req);
				} else {
					setDate(time[1], req);
				}
			}

logger.debug("Set "+(isStartDatepicker?"startTime":"endTime")+" to "+TimeUtils.getDateAndTimeString(isStartDatepicker?time[0]:time[1]));
			// FIXME: triggering bug: Widget is called twice by opening Page - without
			// manualy triggerAction at GET
			//if (getData(req).initBug) {
			//	getData(req).initBug = false;
			//	return;
			//}
			getData(req).init = false;
			return;
		}}

		boolean fixInterval;
		if(Boolean.getBoolean("org.ogema.app.timeseries.viewer.expert.gui.usemultiselectbybuttons")) {
			fixInterval = schedView.configuration(req).showOptionsSwitch && schedView.multiSelectOptions.getSelectedItems(req).contains(ScheduleViewerExtended.FIX_INTERVAL_OPT);
		} else
			fixInterval = schedView.configuration(req).showOptionsSwitch && schedView.optionsCheckbox.getCheckboxList(req).get(ScheduleViewerExtended.FIX_INTERVAL_OPT);
		if (fixInterval) {
			return;
		}
		
		if (sessionConfig.intervalControllability() == PreSelectionControllability.MAX_SIZE) {
			// use for Time only the Intervall which Data is in the Schedule
			setMaxSize(req);
			getData(req).fixedInterval = false;
			return;
		} else if (sessionConfig.intervalControllability() == PreSelectionControllability.FIXED) {
			getData(req).fixedInterval = true;
		} else {
			getData(req).fixedInterval = false; // PreSelectionControllability.FLEXIBLE
		}

		if (schedView.intervalDrop != null && getData(req).fixedInterval) {
			final long now = schedView.am.getFrameworkTime();
			final long duration = schedView.intervalDrop.getSelectedItem(req);
			final long target;
			if (!isStartDatepicker)
				target = now;
			else
				target = now - duration;
			if (duration > 0) {
				setDate(target, req);
				return;
			}
			getData(req).explicitDate = null;
		}
		final Long explicitDate = getData(req).explicitDate;
		if (explicitDate != null) {
			setDate(explicitDate, req);
			return;
		}
		//If we reload and interval is not fixed, we should not take into account the pre-defined startTime/endTime anymore
		/*if ((isStartDatepicker && schedView.configuration.startTime != null)
				|| (!isStartDatepicker && schedView.configuration.endTime != null)) {
			//TODO: What's the reason for this ??
			setDate(System.currentTimeMillis()
					+ (isStartDatepicker ? -schedView.configuration.startTime : schedView.configuration.endTime), req);
			return;
		}*/
		final List<ReadOnlyTimeSeries> schedules = schedView.scheduleSelector(req).getSelectedItems(req);
		if (schedules == null || schedules.isEmpty()) {
			setDate(schedView.am.getFrameworkTime(), req); // irrelevant
			return;
		}
		long dateTime = (isStartDatepicker ? Long.MAX_VALUE : Long.MIN_VALUE);
		for (ReadOnlyTimeSeries sched : schedules) {
			SampledValue sv = (isStartDatepicker ? sched.getNextValue(Long.MIN_VALUE)
					: sched.getPreviousValue(Long.MAX_VALUE));
			if (sv != null) {
				long timestamp = sv.getTimestamp();
				if ((isStartDatepicker && timestamp < dateTime) || (!isStartDatepicker && timestamp > dateTime))
					dateTime = timestamp;
			}
		}

		if (isStartDatepicker) {
			if (dateTime == Long.MAX_VALUE)
				dateTime = System.currentTimeMillis();
		} else { // endDatepicker
			if (dateTime == Long.MIN_VALUE)
				dateTime = System.currentTimeMillis();
			if (dateTime < Long.MAX_VALUE - 10000)
				dateTime += 1001; // ensure all data points are really shown
		}
		setDate(dateTime, req);
	}

	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		getData(req).fixedInterval = false;

	}

	/**
	 * The Date selection may be changed like with schedule viewer without
	 * configuration
	 */
	private void setMaxSize(OgemaHttpRequest req) {
		final List<ReadOnlyTimeSeries> schedules = schedView.scheduleSelector(req).getSelectedItems(req);

		long[] startEnd = ScheduleViewerUtil.getStartEndTime((List<ReadOnlyTimeSeries>) schedules);
		if (startEnd[0] == -1 && startEnd[1] == -1) {
			return;
		}
		if (isStartDatepicker) {
			setDate(startEnd[0], req);
		} else {
			setDate(startEnd[1], req);
		}

	}

}