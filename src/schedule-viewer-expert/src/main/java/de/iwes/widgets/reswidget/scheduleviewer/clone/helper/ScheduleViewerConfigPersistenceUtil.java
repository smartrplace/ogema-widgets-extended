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
package de.iwes.widgets.reswidget.scheduleviewer.clone.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.tools.resource.util.ValueResourceUtils;

import de.iwes.widgets.reswidget.scheduleviewer.api.ConditionalTimeSeriesFilter;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SelectionConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.clone.ScheduleViewerBasisConfig;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;

/**Util class to save {@link SelectionConfiguration}s into JSON Strings that are stored in a
 * StringArrayResource
 *
 */
public class ScheduleViewerConfigPersistenceUtil {

	public  final String PROVIDER_NAME;
	private final String NO_RESOURCE_PATH_FOUND = "noResourcePathFound";
	private final String J_CONDITIONAL_TIME_SERIES_FILTER_CATEGORY_PRESELECTED = "conditionalTimeSeriesFilterCategoryPreselected";
	private final String J_FILTERS_PRE_SELECTED = "filtersPreSelected";
	private final String J_TIME_SERIES_SELECTED = "timeSeriesSelected";
	private final String J_PROGRAMS_PRESELECTED = "programsPreselected";
	private final String J_CONFIGURATION_ID = "configurationId";
	private final ApplicationManager appManager;
	private final Map<String, JSONObject> jsonConfigurations;
	private final Map<String, SelectionConfiguration> selectionConfigurations;
	private ScheduleViewerBasisConfig config;
	
	public ScheduleViewerConfigPersistenceUtil(ScheduleViewerBasisConfig config, ApplicationManager appManager, String providerName) {
		this.appManager = appManager;
		this.PROVIDER_NAME = providerName;
		jsonConfigurations = new HashMap<>();
		selectionConfigurations = new HashMap<>();		
		initConfigurationResource(config);
		readConfigurationResource();
	}
	
	/**
	 * creates or retrieves a configuration resource from the resource database
	 */
	private void initConfigurationResource(ScheduleViewerBasisConfig config) {
		this.config = config;
		if(!config.isActive()) {
			config.create();
			config.activate(true);
		}
		
		StringArrayResource array = this.config.sessionConfigurations();
		if (!array.isActive()) {
			array.create();
			array.activate(true);
		}
	}
	
	private void readConfigurationResource() {
		StringArrayResource array = this.config.sessionConfigurations(); 
		
		for(String entry : array.getValues()){
			 JSONObject json = new JSONObject(entry);
			 jsonConfigurations.put(json.getString(J_CONFIGURATION_ID), json); 
		}
	}
		
	/**
	 * stores a user selection in the configuration resource as a JSON string
	 * @param currentConfiguration
	 * @param configurationId
	 */
	public void saveCurrentSelectionConfiguration(SelectionConfiguration currentConfiguration, String configurationId) {
		final List<ReadOnlyTimeSeries> timeSeriesSelected = currentConfiguration.timeSeriesSelected();
		final List<Collection<TimeSeriesFilter>> programsPreselected = currentConfiguration.programsPreselected();
		final Integer conditionalTimeSeriesFilterCategoryPreselected = currentConfiguration
				.conditionalTimeSeriesFilterCategoryPreselected();
		final List<ConditionalTimeSeriesFilter<?>> filtersPreSelected = currentConfiguration.filtersPreSelected();

		final List<String> pathsOfTimeSeries = new ArrayList<>();
		final List<String> idsOfProgramms = new ArrayList<>();
		final List<String> idsOfFilers = new ArrayList<>();

		for (final ReadOnlyTimeSeries ts : timeSeriesSelected) {
			pathsOfTimeSeries.add(getPath(ts));
		}

		for (final Collection<TimeSeriesFilter> col : programsPreselected) {
			for (final TimeSeriesFilter programm : col) {
				idsOfProgramms.add(programm.id());
			}
		}

		for (final ConditionalTimeSeriesFilter<?> filter : filtersPreSelected) {
			idsOfFilers.add(filter.id());
		}

		if (configurationId == null) {
			configurationId = "config_" + jsonConfigurations.size();
		}

		final JSONObject json = new JSONObject();
		json.put(J_CONFIGURATION_ID, configurationId);
		json.put(J_PROGRAMS_PRESELECTED, idsOfProgramms);
		json.put(J_TIME_SERIES_SELECTED, pathsOfTimeSeries);
		json.put(J_FILTERS_PRE_SELECTED, idsOfFilers);
		json.put(J_CONDITIONAL_TIME_SERIES_FILTER_CATEGORY_PRESELECTED, conditionalTimeSeriesFilterCategoryPreselected);
		saveJSONConfiguration(json.toString(), configurationId);
		jsonConfigurations.put(configurationId, json);
	}

	
	/**
	 * reads Configuration Resource as a JSON string and returns it
	 */
	public SelectionConfiguration getSelectionConfiguration(SelectionConfiguration providerSelectionConfiguration, String configurationId) {
		
		if(selectionConfigurations.containsKey(configurationId)) {
			return selectionConfigurations.get(configurationId);
		}
		
		JSONObject json = jsonConfigurations.get(configurationId); 
		
		if(json != null) {
			SelectionConfiguration configuration = parseConfigurationResource(json, providerSelectionConfiguration, configurationId);		
			selectionConfigurations.put(json.getString(J_CONFIGURATION_ID), configuration);
			return configuration;
		}
		
		return providerSelectionConfiguration;		
	}
	
	
	/**
	 * parsed a JSON object and returns it as Selected Configuration
	 * @param json
	 * @param selectionConfiguration
	 * @param configurationId
	 * @return
	 */
	private SelectionConfiguration parseConfigurationResource(JSONObject json, SelectionConfiguration selectionConfiguration, String configurationId) {

		final Integer conditionalTimeSeriesFilterCategoryPreselected = json
				.getInt(J_CONDITIONAL_TIME_SERIES_FILTER_CATEGORY_PRESELECTED);
		JSONArray jtimeSeriesSelected = json.getJSONArray(J_TIME_SERIES_SELECTED);
		JSONArray jprogrammsSelected = json.getJSONArray(J_PROGRAMS_PRESELECTED);
		JSONArray jfilterSelected = json.getJSONArray(J_FILTERS_PRE_SELECTED);
		final List<String> pathsOfTimeSeries = convertJsonStringArray(jtimeSeriesSelected);
		final List<String> idsOfProgramms = convertJsonStringArray(jprogrammsSelected);
		final List<String> idsOfFilers = convertJsonStringArray(jfilterSelected);

		final List<ReadOnlyTimeSeries> timeSeriesSelected = new ArrayList<>();
		final List<ConditionalTimeSeriesFilter<?>> filtersPreSelected = new ArrayList<>();
		final List<Collection<TimeSeriesFilter>> programsPreselected = new ArrayList<>();
		for (String path : pathsOfTimeSeries) {
			ReadOnlyTimeSeries ts = getReadOnlyTimeSeries(path);
			if (ts != null) {
				timeSeriesSelected.add(ts);
			}
		}
		for (String filterId : idsOfFilers) {
			for (ConditionalTimeSeriesFilter<?> filter : selectionConfiguration.filtersPreSelected()) {
				if (filterId.equals(filter.id())) {
					filtersPreSelected.add(filter);
				}
			}
		} 

		for (String programmId : idsOfProgramms) {
			for (Collection<TimeSeriesFilter> outerProgrammFilter : selectionConfiguration.programsPreselected()) {
				Collection<TimeSeriesFilter> list = new ArrayList<>();
				for (TimeSeriesFilter innerProgrammFilter : outerProgrammFilter) {
					if (programmId.equals(innerProgrammFilter.id())) {
						list.add(innerProgrammFilter);
					}
				}
				if (!list.isEmpty()) {
					programsPreselected.add(list);
				}
			}
		} 
		
		SessionConfigurationPersistent sessionConfiguration = new SessionConfigurationPersistent(appManager,
				configurationId, timeSeriesSelected, programsPreselected, conditionalTimeSeriesFilterCategoryPreselected,
				filtersPreSelected);
		return sessionConfiguration;
	}

	


	private void saveJSONConfiguration(String jsonConfiguration, String configurationId) {		
		StringArrayResource array = this.config.sessionConfigurations(); 
		ValueResourceUtils.appendValue(array, jsonConfiguration);
	}

	private String getPath(ReadOnlyTimeSeries ts) {
		String path = NO_RESOURCE_PATH_FOUND;
		if (ts instanceof Schedule) {
			Schedule schedule = (Schedule) ts;
			path = schedule.getPath();
		} else if (ts instanceof RecordedData) {
			RecordedData recordedData = (RecordedData) ts;
			path = recordedData.getPath();
		}
		return path;
	}

	private ReadOnlyTimeSeries getReadOnlyTimeSeries(String path) {

		Resource resource = appManager.getResourceAccess().getResource(path);

		if (resource instanceof ReadOnlyTimeSeries) {
			ReadOnlyTimeSeries ts = (ReadOnlyTimeSeries) resource;
			return ts;
		}
		return null;
	}

	private List<String> convertJsonStringArray(JSONArray array) {
		final List<String> list = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			String id = array.getString(i);
			list.add(id);
		}
		return list;
	}

	
}
