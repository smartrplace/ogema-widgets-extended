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

import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.textfield.ValueInputField;
import de.iwes.widgets.html.popup.Popup;

public class ConfigPopup extends Popup {
	
	private static final long serialVersionUID = 1L;
	private final TemplateDropdown<ReadOnlyTimeSeries> selector;
	private final ValueInputField<Float> scaleField;
	private final ValueInputField<Float> offsetField;
	private final Button closeBtn;

	public ConfigPopup(WidgetPage<?> page, String id, final ScheduleViewerExtended scheduleViewer) {
		super(page, id, true);
		this.selector = new TemplateDropdown<ReadOnlyTimeSeries>(page, "selector_" + id) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final List<ReadOnlyTimeSeries> schedules = scheduleViewer.getSelectedItems(req);
				update(schedules, req);
			}
			
		};
		//selector.setTemplate(scheduleViewer.scheduleSelector.templateLong);
		this.scaleField = new ValueInputField<Float>(page, "scaleField_" + id, Float.class) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				final ReadOnlyTimeSeries schedule = selector.getSelectedItem(req);
				if (schedule == null) {
					setNumericalValue(1F, req);
					return;
				}
				final String label = scheduleViewer.getDisplayTemplate(req).getLabel(schedule, req.getLocale());
				final float val = scheduleViewer.getSchedulePlot().getScheduleData(req).getScale(label, req);
				setNumericalValue(val, req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				final ReadOnlyTimeSeries schedule = selector.getSelectedItem(req);
				if (schedule == null) 
					return;
				final String label = scheduleViewer.getDisplayTemplate(req).getLabel(schedule, req.getLocale());
				final Float val = getNumericalValue(req);
				if (val == null || val <= 0)
					return;
				scheduleViewer.getSchedulePlot().getScheduleData(req).setScale(label, val, req);
			}
			
		};
		
		this.offsetField = new ValueInputField<Float>(page, "offsetField_" + id, Float.class) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				final ReadOnlyTimeSeries schedule = selector.getSelectedItem(req);
				if (schedule == null) {
					setNumericalValue(0F, req);
					return;
				}
				final String label = scheduleViewer.getDisplayTemplate(req).getLabel(schedule, req.getLocale());
				final float val = scheduleViewer.getSchedulePlot().getScheduleData(req).getOffset(label, req);
				setNumericalValue(val, req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				final ReadOnlyTimeSeries schedule = selector.getSelectedItem(req);
				if (schedule == null) 
					return;
				final String label = scheduleViewer.getDisplayTemplate(req).getLabel(schedule, req.getLocale());
				final Float val = getNumericalValue(req);
				if (val == null)
					return;
				scheduleViewer.getSchedulePlot().getScheduleData(req).setOffset(label, val, req);
			}
			
		};
		this.closeBtn = new Button(page, "closeBtn_" + id, "Close");
		
		PageSnippet snippet = new PageSnippet(page, "snippet_"+id, true);
		final StaticTable t = new StaticTable(3, 2, new int[]{4,8})
				.setContent(0, 0, "Select schedule").setContent(0, 1, selector)
				.setContent(1, 0, "Scale").setContent(1, 1, scaleField)
				.setContent(2, 0, "Offset").setContent(2, 1, offsetField);
		snippet.append(t, null);
		this.setBody(snippet, null);
		this.setTitle("Schedule viewer configuration", null);
		this.setFooter(closeBtn, null);
		
		closeBtn.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
		selector.triggerAction(offsetField, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		selector.triggerAction(scaleField, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
	void trigger(final OgemaWidget widget) {
		widget.triggerAction(selector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		widget.triggerAction(scaleField, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		widget.triggerAction(offsetField, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		widget.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
	}
	
}
