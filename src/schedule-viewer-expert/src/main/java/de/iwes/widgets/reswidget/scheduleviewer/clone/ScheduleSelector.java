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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import de.iwes.widgets.api.services.NameService;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;
import de.iwes.widgets.html.multiselect.TemplateMultiselectData;
import de.iwes.widgets.reswidget.scheduleviewer.api.ConditionalTimeSeriesFilter;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SessionConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilterExtended;
import de.iwes.widgets.reswidget.scheduleviewer.utils.ScheduleViewerUtil;
import de.iwes.widgets.template.DisplayTemplate;

public class ScheduleSelector extends TemplateMultiselect<ReadOnlyTimeSeries> {
	private static final TriggeredAction GET_REQUEST = TriggeredAction.GET_REQUEST;
	private static final TriggeringAction POST_REQUEST = TriggeringAction.POST_REQUEST;

	/**
	 * Possibility to deselect and select all options in the Schedule Selector.
	 * Possibly. It would also be reasonable standard that with more than 10 options no
	 * Option is selected, but only the multiselect is filled with options;
	 * if there are a lot of rows in the filter, then you often had to laboriously
	 * Manually select / deselect all rows
	 *
	 */
	class SelectAllButton extends Button {

		private static final long serialVersionUID = -146823525912151858L;


		@Override
		public void onPOSTComplete(String data, OgemaHttpRequest req) {
			final List<ReadOnlyTimeSeries> allSchedules = getItems(req);
			boolean select = getData(req).selectOrDeselect;
			if (select) {
				schedView.selectSchedules(allSchedules, req);
			} else {
				schedView.selectSchedules(Collections.emptyList(), req);

			}
			getData(req).selectOrDeselect = !getData(req).selectOrDeselect;
			getData(req).pushed = true;
		}

		public SelectAllButton(WidgetPage<?> page, String id) {
			super(page, id);
			this.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		}
		public SelectAllButton(OgemaWidget parent, String id, OgemaHttpRequest req) {
			super(parent, id, req);
			this.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		}

		@Override
		public SelectAllButtonData createNewSession() {
			return new SelectAllButtonData(this);
		}

		@Override
		public SelectAllButtonData getData(OgemaHttpRequest req) {
			return (SelectAllButtonData) super.getData(req);
		}

		boolean hasBeenPushed(OgemaHttpRequest req) {
			final boolean pushed = getData(req).pushed;
			getData(req).pushed = false;
			return pushed;
		}

		@SuppressWarnings("deprecation")
		@Override
		public void onGET(OgemaHttpRequest req) {
			if (getData(req).selectOrDeselect) {
				setText(System.getProperty("org.ogema.app.timeseries.viewer.expert.gui.selectAllSchedlabel",
						"Select all Schedules  "), req);
				setCss("btn btn-info", req);
			} else {
				setText(System.getProperty("org.ogema.app.timeseries.viewer.expert.gui.deselectAllSchedlabel",
						"Deselect all Schedules"), req);
				setCss("btn btn-danger", req);
			}
		}

	}

	private static class SelectAllButtonData extends ButtonData {

		private boolean pushed = false;
		private boolean selectOrDeselect = true;

		public SelectAllButtonData(Button button) {
			super(button);
		}
	}

	static class TemplateMultiselectDataExtended<T extends ReadOnlyTimeSeries> extends TemplateMultiselectData<T> {

		DisplayTemplate<T> template;
		public boolean init = true;
		public boolean initBug = true;

		public TemplateMultiselectDataExtended(TemplateMultiselect<T> multiselect, DisplayTemplate<T> template) {
			super(multiselect);
			this.template = template;
		}

		public void setDisplayTemplate(DisplayTemplate<T> displayTemplate) {
			this.template = displayTemplate;
		}

		public DisplayTemplate<T> getTemplate() {
			return template;
		}

		@Override
		protected String[] getValueAndLabel(T item) {
			String label = template.getLabel(item, OgemaLocale.ENGLISH);
			String value = template.getId(item);
			Objects.requireNonNull(label);
			Objects.requireNonNull(value);
			return new String[] { value, label };
		}

	}

	private static class DropdownDataExented extends DropdownData {

		boolean pushed = false;

		public DropdownDataExented(Dropdown dropdown) {
			super(dropdown);
		}
	}

	private static final long serialVersionUID = 1L;
	public final SelectAllButton selectAllOrDeselectAllButton;
	//public final ChangeScheduleNameDropdown scheduleNameDropDown;
	final DisplayTemplate<ReadOnlyTimeSeries> templateShort;
	public final DisplayTemplate<ReadOnlyTimeSeries> templateLong;
	final DisplayTemplate<ReadOnlyTimeSeries> templateLocation;
	private final ScheduleViewerExtended schedView;

	public ScheduleSelector(OgemaWidget parent, String id,
			SessionConfiguration sessionconfig, OgemaHttpRequest req,
			ScheduleViewerExtended schedView) {
		super(parent, id, req);
		this.schedView = schedView;
		setDefaultWidth("100%");

		selectAllOrDeselectAllButton = new SelectAllButton(parent, id + "_selectScheduleButton", req);
		//scheduleNameDropDown = new ChangeScheduleNameDropdown(page, id + "_scheduleNameChanger");
		List<Collection<TimeSeriesFilter>> programs = parseFilters(schedView.configuration(req).programs);// sessionconfig.programsPreselected();
		List<Collection<TimeSeriesFilterExtended>> filterCollection = ScheduleViewerUtil.getInstance()
				.parse(programs, schedView.am.getResourceAccess());
		
		NameService nameService = schedView.configuration(req).useNameService ? getNameService() : null;
		templateShort = new DefaultTimeSeriesDisplayTemplate<>(nameService, filterCollection, ScheduleViewerExtended.SHORT_NAME);
		templateLong = new DefaultTimeSeriesDisplayTemplate<>(nameService, filterCollection, ScheduleViewerExtended.LONG_NAME);
		templateLocation = new DefaultTimeSeriesDisplayTemplate<>(nameService, filterCollection, ScheduleViewerExtended.LOCATION);
		selectAllOrDeselectAllButton.triggerAction(this, POST_REQUEST, GET_REQUEST);
		schedView.dropdownScheduleNames.triggerAction(this, POST_REQUEST, GET_REQUEST);
		setTemplate(templateShort);
	}

	/**
	 * Parse the list from Maps and make a list of lists
	 * @param programs
	 * @return
	 */
	public static List<Collection<TimeSeriesFilter>> parseFilters(List<Map<String, TimeSeriesFilter>> programs) {
		List<Collection<TimeSeriesFilter>> outer = new ArrayList<>();

		if (programs != null) {
			for (Map<String, TimeSeriesFilter> map : programs) {
				if (map != null) {
					Collection<TimeSeriesFilter> inner = map.values();
					outer.add(inner);
				}
			}
		}
		return outer;
	}

	@Override
	public TemplateMultiselectDataExtended<ReadOnlyTimeSeries> createNewSession() {
		return new TemplateMultiselectDataExtended<ReadOnlyTimeSeries>(this, templateShort);
	}

	@Override
	public TemplateMultiselectDataExtended<ReadOnlyTimeSeries> getData(OgemaHttpRequest req) {
		return (TemplateMultiselectDataExtended<ReadOnlyTimeSeries>) super.getData(req);
	}

	/**
	 * initalized the ScheduleSelector, updates the Naming (Long, Short, Locations)
	 */
	@Override
	public void onGET(OgemaHttpRequest req) {
		if (selectAllOrDeselectAllButton.hasBeenPushed(req)) {
			return;
		}

		if (schedView.dropdownScheduleNames.hasBeenPushed(req)) {
			updateScheduleNaming(req);
			return;
		}

		final SessionConfiguration sessionConfig = schedView.getSessionConfiguration(req);

		TemplateMultiselectDataExtended<ReadOnlyTimeSeries> d = getData(req);
		if (d != null && d.init && sessionConfig.overwritePrograms()) {
			overwriteScheduleSelectorWithProvider(req, sessionConfig);
			if (getData(req).initBug) {
				getData(req).initBug = false;
			}
			getData(req).init = false;
		}

		List<ReadOnlyTimeSeries> updateList;
		List<ReadOnlyTimeSeries> selected = sessionConfig.timeSeriesSelected();
		if(selected == null) updateList = schedView.getDefaultSchedules(req);
		else if(sessionConfig.overwriteDefaultTimeSeries())
			updateList = selected;
		else {
			updateList = new ArrayList<>(schedView.getDefaultSchedules(req));
			updateList.addAll(selected);
		}
		update(updateList, req);
		if(Boolean.getBoolean("org.ogema.reswidget.eval.scheduleviewer.expert.selectOffered.donot"))
			selectItem(updateList.get(0), req);
		else if(!updateList.isEmpty())
			selectItems(updateList, req);
	}

	@Override
	public void update(Collection<ReadOnlyTimeSeries> items, OgemaHttpRequest req) {
		super.update(items, req);
	}
	
	@Override
	public boolean addItem(ReadOnlyTimeSeries item, OgemaHttpRequest req) {
		return super.addItem(item, req);
	}
	
	@Override
	public void addOption(String label, String value, boolean selected, OgemaHttpRequest req) {
		super.addOption(label, value, selected, req);
	}
	
	@Override
	public void update(Map<String, String> values, OgemaHttpRequest req) {
		super.update(values, req);
	}
	
	@Override
	public void selectDefaultItems(Collection<ReadOnlyTimeSeries> items) {
		super.selectDefaultItems(items);
	}
	
	@Override
	public void setDefaultOptions(Collection<DropdownOption> defaultOptions) {
		super.setDefaultOptions(defaultOptions);
	}
	
	/**
	 * updates the schedule selector according to conditions specified by the program selector and filter selector
	 * @param req
	 */
	@SuppressWarnings("unchecked")
	public void updateByProgramOrFilterChange(OgemaHttpRequest req) {
		// select all schedules matching the selected filters
		boolean ok;
		boolean filterSet = false;
		List<ReadOnlyTimeSeries> schedules = getItems(req);
		if (schedView.programSelectors != null) {
			for (TemplateMultiselect<TimeSeriesFilterExtended> programSelector : schedView.programSelectors) {
				List<TimeSeriesFilterExtended> selectedFilters = programSelector.getSelectedItems(req);
				if (selectedFilters.isEmpty()) {
					continue;
				}
				filterSet = true;
				Iterator<ReadOnlyTimeSeries> it = schedules.iterator();
				ReadOnlyTimeSeries schedule;
				while (it.hasNext()) {
					ok = false;
					schedule = it.next();
					for (TimeSeriesFilterExtended selectedFilter : selectedFilters) {
						if (selectedFilter.accept(schedule)) {
							ok = true;
							break;
						}
					}
					if (!ok)
						it.remove();
				}
			}
		}
		if (schedView.filterSelectors != null) {
			for (ConditionalProgramSelector filterSelector : schedView.filterSelectors) {
				@SuppressWarnings("rawtypes")
				ConditionalTimeSeriesFilter selectedFilter = filterSelector.filterSelector.getSelectedItem(req);
				if (selectedFilter == null) {
					continue;
				}
				ResourcePattern<?> pattern = filterSelector.instanceSelector.getSelectedItem(req);
				if (pattern == null) {
					continue;
				}
				filterSet = true;
				Iterator<ReadOnlyTimeSeries> it = schedules.iterator();
				while (it.hasNext()) {
					if (!selectedFilter.accept(it.next(), pattern))
						it.remove();
				}
			}
		}
		if (filterSet) {
			schedView.selectSchedules(schedules, req);
		} else {
			schedView.selectSchedules(Collections.emptyList(), req);
		}
	}

	/**
	 * Overwrites the ScheduleSelector with the schedules selected in the provider
	 * @param req
	 * @param sessionConfig
	 */
	public void overwriteScheduleSelectorWithProvider(OgemaHttpRequest req,
			final SessionConfiguration sessionConfig) {

		List<ReadOnlyTimeSeries> selectedTimeSeries = sessionConfig.timeSeriesSelected();
		if (selectedTimeSeries.size() > 10) {
			schedView.selectSchedules(selectedTimeSeries.subList(0, 9), req);
		} else {
			schedView.selectSchedules(selectedTimeSeries, req);
		}
		//update(selectedTimeSeries, req);
	}

	/**
	 * updates the scheduleNaming
	 * @param req
	 */
	public void updateScheduleNaming(OgemaHttpRequest req) {
		final List<ReadOnlyTimeSeries> schedules = getItems(req);
		final List<ReadOnlyTimeSeries> selectedTimeSeries = schedView.getSelectedSchedules(req);

		String naming = schedView.dropdownScheduleNames.getSelectedValue(req);
		if (ScheduleViewerExtended.SHORT_NAME.equals(naming)) {
			getData(req).setDisplayTemplate(templateShort);
		} else if (ScheduleViewerExtended.LONG_NAME.equals(naming)) {
			getData(req).setDisplayTemplate(templateLong);
		} else {
			getData(req).setDisplayTemplate(templateLocation);
		}
		clear(req);
		schedView.setSchedules(schedules, req);
		if (selectedTimeSeries.size() > 10) {
			schedView.selectSchedules(selectedTimeSeries.subList(0, 9), req);
		} else {
			schedView.selectSchedules(selectedTimeSeries, req);
		}
	}

	/**
	 * Dropdown zur Umschaltung der Zeitreihen im Schedule Selector (Location
	 * Longname / Shortname)
	 */

	static class ChangeScheduleNameDropdown extends Dropdown {

		private static final long serialVersionUID = 4525177697823002529L;

		public ChangeScheduleNameDropdown(WidgetPage<?> page, String id) {
			super(page, id);
			final List<DropdownOption> options = new ArrayList<>();
			options.add(new DropdownOption(ScheduleViewerExtended.SHORT_NAME, ScheduleViewerExtended.SHORT_NAME, false));
			options.add(new DropdownOption(ScheduleViewerExtended.LONG_NAME, ScheduleViewerExtended.LONG_NAME, false));
			options.add(new DropdownOption(ScheduleViewerExtended.LOCATION, ScheduleViewerExtended.LOCATION, true));
			setDefaultOptions(options);
		}

		@Override
		public void onGET(OgemaHttpRequest req) {
			selectSingleOption(ScheduleViewerExtended.SHORT_NAME, req);
		}

		@Override
		public void onPOSTComplete(String data, OgemaHttpRequest req) {
			getData(req).pushed = true;
		}

		public boolean hasBeenPushed(OgemaHttpRequest req) {
			final boolean pushed = getData(req).pushed;
			getData(req).pushed = false;
			return pushed;
		}

		@Override
		public DropdownDataExented createNewSession() {
			return new DropdownDataExented(this);
		}

		@Override
		public DropdownDataExented getData(OgemaHttpRequest req) {
			return (DropdownDataExented) super.getData(req);
		}

	}

}
