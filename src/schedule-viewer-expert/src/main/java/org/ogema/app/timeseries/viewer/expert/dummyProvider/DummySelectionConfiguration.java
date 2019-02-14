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
package org.ogema.app.timeseries.viewer.expert.dummyProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import de.iwes.widgets.reswidget.scheduleviewer.api.ConditionalTimeSeriesFilter;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SelectionConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;

public class DummySelectionConfiguration implements SelectionConfiguration {

	private final List<ConditionalTimeSeriesFilter<?>> filtersPreSelected;
	private final List<ReadOnlyTimeSeries> timeSeriesSelected;
	private final int MAX_PRESELECTED_TIMESERIES = 6;
	
	public final List<Collection<TimeSeriesFilter>> programsAll;
	public final List<ConditionalTimeSeriesFilter<?>> filtersAll;
	private final ApplicationManager appManager;

	public DummySelectionConfiguration(ApplicationManager appManager) {
		filtersPreSelected = new ArrayList<>();
		timeSeriesSelected = new ArrayList<>();
		programsAll = new ArrayList<>();
		programsAll.add(Arrays.asList(DummySessionConfiguration.ACCEPT_NOTHING_FILTER));
		filtersAll = new ArrayList<>();
		this.appManager = appManager;
	}

	@Override
	public List<ReadOnlyTimeSeries> timeSeriesSelected() {
		final List<ReadOnlyTimeSeries> allSchedules = new ArrayList<>();
		for (ReadOnlyTimeSeries schedule : appManager.getResourceAccess().getResources(Schedule.class)) {
			allSchedules.add(schedule);
		}
		timeSeriesSelected.clear();
		for (Collection<TimeSeriesFilter> filters : programsAll) {
			for (TimeSeriesFilter filter : filters) {
				for (ReadOnlyTimeSeries schedule : allSchedules) {					
					if (filter.accept(schedule)) {
						timeSeriesSelected.add(schedule);	
					}
					if(timeSeriesSelected.size() >= MAX_PRESELECTED_TIMESERIES) {
						return timeSeriesSelected;
					}
				}
			}
		}
		
		
		return timeSeriesSelected;
	}

	@Override
	public List<Collection<TimeSeriesFilter>> programsPreselected() {
		return programsAll;
	}

	@Override
	public Integer conditionalTimeSeriesFilterCategoryPreselected() {
		return 0;
	}

	@Override
	public List<ConditionalTimeSeriesFilter<?>> filtersPreSelected() {
		return filtersPreSelected;
	}

}
