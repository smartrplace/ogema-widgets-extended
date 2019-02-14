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

import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.html.schedulemanipulator.ScheduleManipulatorConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ConditionalTimeSeriesFilter;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationBuilder;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SelectionConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SessionConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.clone.ScheduleViewerExtended;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;

public class DummySessionConfiguration implements SessionConfiguration {
	public final SelectionConfiguration selectionConfiguration;

	public DummySessionConfiguration(SelectionConfiguration selectionConfiguration) {
		this.selectionConfiguration = selectionConfiguration;
	}

	@Override
	public ScheduleViewerConfiguration viewerConfiguration() {

		List<Collection<ConditionalTimeSeriesFilter<?>>> filters = new ArrayList<>();
		filters.add(selectionConfiguration.filtersPreSelected());
		List<Collection<TimeSeriesFilter>> programs = new ArrayList<>();
		programs.add(Arrays.asList(ACCEPT_NOTHING_FILTER)); 

		final ScheduleManipulatorConfiguration manipulatorConfig = new ScheduleManipulatorConfiguration(null, true,
				true);
		ScheduleViewerConfiguration config;

			config = ScheduleViewerConfigurationBuilder.newBuilder()
				.setShowCsvDownload(true)
				.setShowIndividualConfigBtn(false)
				.setShowNrPointsPreview(true)
				.setShowOptionsSwitch(false)
				.setShowStandardIntervals(false)
				.setUseNameService(false)
				.setManipulatorConfiguration(manipulatorConfig)
				.setPrograms(programs)
				//.setFilters((List) filters, conditionalTimeSeriesFilterCategoryPreselected())
				.setBufferWindow(ScheduleViewerExtended.STANDARD_BUFFER_WINDOW).build();
		return config;
	}

	@Override
	public PreSelectionControllability intervalControllability() {
		return PreSelectionControllability.FLEXIBLE;
	}

	@Override
	public PreSelectionControllability timeSeriesSelectionControllability() {
		return PreSelectionControllability.FLEXIBLE;
	}

	@Override
	public PreSelectionControllability filterControllability() {
		return PreSelectionControllability.FLEXIBLE;
	}

	@Override
	public boolean overwritePrograms() {
		return false;
	}

	@Override
	public boolean overwriteConditionalFilters() {
		return false;
	}

	@Override
	public boolean overwriteProgramlistFixed() {
		return false;
	}
	
	@Override
	public boolean overwriteDefaultTimeSeries() {
		return false;
	}

	@Override
	public boolean markTimeSeriesSelectedViaPreselectedFilters() {
		return false;
	}

	@Override
	public List<ReadOnlyTimeSeries> timeSeriesSelected() {
		return selectionConfiguration.timeSeriesSelected();
	}

	/*@Override
	public List<ReadOnlyTimeSeries> timeSeriesOffered() {
		return null;
	}*/

	@Override
	public List<Collection<TimeSeriesFilter>> programsPreselected() {
		return selectionConfiguration.programsPreselected();
	}

	@Override
	public Integer conditionalTimeSeriesFilterCategoryPreselected() {
		return selectionConfiguration.conditionalTimeSeriesFilterCategoryPreselected();
	}

	@Override
	public List<ConditionalTimeSeriesFilter<?>> filtersPreSelected() {
		return selectionConfiguration.filtersPreSelected();
	}
	

	public static final TimeSeriesFilter ACCEPT_NOTHING_FILTER = new TimeSeriesFilter() { 

		@Override
		public String label(OgemaLocale arg0) {
			return "accept Nothing ";
		}

		@Override
		public String id() {
			return "acceptNothing";
		}

		@Override
		public boolean accept(ReadOnlyTimeSeries schedule) {
			return false;
		}
	};

	@Override
	public boolean generateGraphImmediately() {
		return false;
	}

}
