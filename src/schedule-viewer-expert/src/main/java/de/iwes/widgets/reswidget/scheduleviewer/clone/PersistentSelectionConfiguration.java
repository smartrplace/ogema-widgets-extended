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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import de.iwes.widgets.reswidget.scheduleviewer.api.ConditionalTimeSeriesFilter;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SelectionConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;

public class PersistentSelectionConfiguration implements SelectionConfiguration {

	private final List<ReadOnlyTimeSeries> timeSeriesSelected;
	private final List<Collection<TimeSeriesFilter>> programsPreselected;
	private Integer conditionalTimeSeriesFilterCategoryPreselected;
	private final List<ConditionalTimeSeriesFilter<?>> filtersPreSelected;
	
	public PersistentSelectionConfiguration( ) {	
		this.timeSeriesSelected = new ArrayList<>();
		this.programsPreselected = new ArrayList<>();
		this.filtersPreSelected = new ArrayList<>();
	}
	
	public void setConditionalTimeSeriesFilterCategoryPreselected(Integer conditionalTimeSeriesFilterCategoryPreselected) {
		this.conditionalTimeSeriesFilterCategoryPreselected = conditionalTimeSeriesFilterCategoryPreselected;
	}

	@Override
	public List<ReadOnlyTimeSeries> timeSeriesSelected() {
		return timeSeriesSelected;
	}

	@Override
	public List<Collection<TimeSeriesFilter>> programsPreselected() {
		return programsPreselected;
	}

	@Override
	public Integer conditionalTimeSeriesFilterCategoryPreselected() {
		return conditionalTimeSeriesFilterCategoryPreselected;
	}

	@Override
	public List<ConditionalTimeSeriesFilter<?>> filtersPreSelected() {
		return filtersPreSelected;
	}
	
	

}
