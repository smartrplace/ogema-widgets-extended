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
package org.ogema.app.timeseries.viewer.expert.gui;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ogema.app.timeseries.viewer.expert.ScheduleViewerBasicApp;
import org.ogema.core.application.ApplicationManager;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.DynamicTableData;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SessionConfiguration;

public class ScheduleProviderOverviewPage {

	private static final String HEADER_TEXT = "Scheduleprovider Overview Page";
	private final SimpleCheckbox checkBoxExpertMode;
	private final Map<String, RedirectButton> buttons;

	public ScheduleProviderOverviewPage(final WidgetPage<?> page, final ApplicationManager am,
			final ScheduleViewerBasicApp app) {
		final Header header = new Header(page, "header", HEADER_TEXT, true);
		buttons = new HashMap<>();
		header.setForceUpdate(true);
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_CENTERED);
		checkBoxExpertMode = new SimpleCheckbox(page, "expertMode", "Expert Mode (Show all Filtering options)");

		page.append(header);
		page.showOverlay(true);

		DynamicTable<ScheduleViewerConfigurationProvider> table = new DynamicTable<ScheduleViewerConfigurationProvider>(
				page, "OnlineAggTable") {

			private static final long serialVersionUID = -8313028543877300830L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				addStyle(DynamicTableData.BOLD_HEADER, req);
				addStyle(DynamicTableData.TABLE_STRIPED, req);
				updateRows(app.getScheduleProviders().values(), req);
			}
		};

		table.setDefaultHeaderColor("00ccff");

		table.setRowTemplate(new RowTemplate<ScheduleViewerConfigurationProvider>() {

			@Override
			public Row addRow(ScheduleViewerConfigurationProvider provider, OgemaHttpRequest req) {

				String id = provider.getConfigurationProviderId();
				String urlToscheduleViewer = ScheduleViewerBasicApp.URL_PATH + "/index.html?providerId=" + id;
				Row row = new Row();

				String info;
				SessionConfiguration sessConfig = provider.getSessionConfiguration(null);
				if(sessConfig == null) info = "No config for "+id;
				else {
					boolean isEmpty = sessConfig.timeSeriesSelected().isEmpty();
					if (isEmpty) {
						info = "No schedules matches with " + id + " Schedule Pattern";
					} else {
						int size = provider.getSessionConfiguration(null).timeSeriesSelected().size();
						info = "Found " + size+ " that matches with the " + id + " Schedule Pattern";
						if(size > 9) {
							info = "Found >= 10 that matches with the " + id + " Schedule Pattern";
						}
					}
				}
				row.addCell("provider", id);
				if(sessConfig == null)
					row.addCell("button", "---");
				else
					row.addCell("button", getRedirectButton(id, urlToscheduleViewer));
				row.addCell("info", info);
				return row;
			}

			private RedirectButton getRedirectButton(String id, String urlToscheduleViewer) {
				String buttonId = id + "_redirectButton";
				if(!buttons.containsKey(buttonId)) {
					
					final RedirectButton button = new RedirectButton(page, buttonId, id + " ScheduleViewer",
							urlToscheduleViewer) {
	
						private static final long serialVersionUID = -5350672290775710246L;
	
						@Override
						public void onPrePOST(String data, OgemaHttpRequest req) {
							boolean expertMode = checkBoxExpertMode.getValue(req); //getCheckboxInput(req, checkBoxExpertMode);
							setUrl(urlToscheduleViewer + "&expertMode=" + expertMode, req);
						}
					};
					buttons.put(buttonId, button);
				}
				return buttons.get(buttonId);
			}

			@Override
			public String getLineId(ScheduleViewerConfigurationProvider provider) {
				return provider.getConfigurationProviderId();
			}

			@Override
			public Map<String, Object> getHeader() {
				final Map<String, Object> header = new LinkedHashMap<>();
				header.put("provider", "Provider");
				header.put("button", "Open");
				header.put("info", "Info");
				return header;
			}

		});

		page.append(table);
		page.append(checkBoxExpertMode);
	}
}
