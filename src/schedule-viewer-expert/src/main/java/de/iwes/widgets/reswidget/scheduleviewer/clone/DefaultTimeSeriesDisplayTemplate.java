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

import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.humread.valueconversion.SchedulePresentationData;
import org.ogema.tools.resource.util.ResourceUtils;

import de.iwes.widgets.api.services.NameService;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilterExtended;
import de.iwes.widgets.template.DisplayTemplate;

public class DefaultTimeSeriesDisplayTemplate<T extends ReadOnlyTimeSeries> implements DisplayTemplate<T> {

	private final NameService nameService;
	private final List<TimeSeriesFilterExtended> filters;	
	private final String selectedNamingType;
	
	public DefaultTimeSeriesDisplayTemplate(NameService nameService, List<Collection<TimeSeriesFilterExtended>> filterCollection, String selectedNamingType) {
		this.nameService = nameService;
		this.filters = new ArrayList<>();
		for(Collection<TimeSeriesFilterExtended> item : filterCollection) {
			this.filters.addAll(item);
		}	
		this.selectedNamingType = selectedNamingType;
	}
	
	@Override
	public String getId(T schedule) {
		if (schedule instanceof Schedule)
			return ((Schedule) schedule).getPath();
		if (schedule instanceof RecordedData)
			return ((RecordedData) schedule).getPath();
		if (schedule instanceof SchedulePresentationData)
			return ResourceUtils.getValidResourceName(((SchedulePresentationData) schedule).getLabel(OgemaLocale.ENGLISH));
		for(TimeSeriesFilterExtended filter : filters) {
			if(filter.accept(schedule)) {
				return filter.longName(schedule);
			}
		}

		throw new IllegalArgumentException("Could not determine schedule id for time series " + schedule +
				". Please provide a unique Long Name!") ; //Custom Display is not supported for Schedule Viewer Expert
	}

	@Override
	public String getLabel(T schedule, OgemaLocale locale) {
		
		for(TimeSeriesFilterExtended filter : filters) {
			if(filter.accept(schedule)) {
				if(ScheduleViewerExtended.SHORT_NAME.equals(selectedNamingType)) {
					return filter.shortName(schedule);
				}else if(ScheduleViewerExtended.LONG_NAME.equals(selectedNamingType)) {				
					return filter.longName(schedule);
				}
			}
		}
		
		if (schedule instanceof Schedule) {			
			if(ScheduleViewerExtended.LOCATION.equals(selectedNamingType)) {
				Schedule s = (Schedule) schedule;
				return s.getLocation();
			}
		}
		
		if(schedule instanceof SchedulePresentationData) {
			return ((SchedulePresentationData) schedule).getLabel(locale);
		}
		
		if (schedule instanceof Schedule) {
			if (nameService != null) {
				String name = nameService.getName((Schedule) schedule, locale);
				if (name != null) {
					return name;
				}
			}
			return ResourceUtils.getHumanReadableName((Schedule) schedule);
		}
		if (schedule instanceof RecordedData) {
			return ((RecordedData) schedule).getPath();
		}
		for(TimeSeriesFilterExtended filter : filters) {
			if(filter.accept(schedule)) {
				return filter.longName(schedule);
			}
		}
		
		throw new IllegalArgumentException("Could not determine schedule label for time series " + schedule +
				". Please provide a Long Name.");
	}
	
	


}
