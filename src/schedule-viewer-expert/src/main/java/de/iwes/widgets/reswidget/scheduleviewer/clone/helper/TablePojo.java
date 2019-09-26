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

import de.iwes.widgets.api.extended.OgemaWidgetBase;
import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.HtmlItem;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.reswidget.scheduleviewer.clone.PageSnippetSelector;
import de.iwes.widgets.reswidget.scheduleviewer.clone.ScheduleSelector;
import de.iwes.widgets.reswidget.scheduleviewer.clone.ScheduleViewerExtended;

/**
 * Helper class which serves as PoJo to show or hide different rows in the DynamicTable from ScheduleViewerBasic
 * @author skarge
 *
 * @param <T>
 */
public class TablePojo { //<T extends OgemaWidget> { 

	private final Label label;
	private final String id;
	private final PageSnippet snippet;
	private static int COUNT = 0;
	
	public TablePojo(String id, Label label, OgemaWidgetBase<?> widget, WidgetPage<?> page){		
		this(id, label, widget, null, page);
	}
	
	public TablePojo(String id, Label label, HtmlItem widget, OgemaWidgetBase<?> widgetOptional, WidgetPage<?> page){		
		this.label = label;	
		this.id= "id_"+String.format("%04d", TablePojo.COUNT++) +"_table_"+id;
		this.snippet = initSnippet(page, widget, widgetOptional);
	}
	public TablePojo(String id, Label label, OgemaWidgetBase<?> widget, OgemaWidgetBase<?> widgetOptional, WidgetPage<?> page){		
		this.label = label;	
		this.id= "id_"+String.format("%04d", TablePojo.COUNT++) +"_table_"+id;
		this.snippet = initSnippet(page, widget, widgetOptional);
	}
	//TODO: Only for ScheduleSelector
	public TablePojo(String id, Label label, WidgetPage<?> page, ScheduleViewerExtended sve){		
		this.label = label;	
		this.id= "id_"+String.format("%04d", TablePojo.COUNT++) +"_table_"+id;
		this.snippet = sve.selectorSnippet = initSelectorSnippet(page,sve);
	}

	public Label getLabel() {
		return label;
	}


	public String getId() {
		return id;
	}
	
	public PageSnippet getSnippet() {
		return snippet;
	}

	private PageSnippet initSnippet(WidgetPage<?> page, HtmlItem w1, OgemaWidget w2) {
		PageSnippet snippet = new PageSnippet(page, id, true);
		if(w1 != null) {
			snippet.append(w1, null);
		}
		if(w2 != null) {
			snippet.append(w2, null);
		}
		return snippet;		
	}
	private PageSnippet initSnippet(WidgetPage<?> page, OgemaWidget w1, OgemaWidget w2) {
		PageSnippet snippet = new PageSnippet(page, id, true);
		if(w1 != null) {
			snippet.append(w1, null);
		}
		if(w2 != null) {
			snippet.append(w2, null);
		}
		return snippet;		
	}

	private PageSnippetSelector initSelectorSnippet(WidgetPage<?> page, ScheduleViewerExtended sve) {
		PageSnippetSelector snippet = new PageSnippetSelector(page, id, false) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				ScheduleSelector sel = sve.scheduleSelector(req);
				append(sel, req);
				append(sel.selectAllOrDeselectAllButton, req);
			}
		};
		return snippet;		
	}

}
