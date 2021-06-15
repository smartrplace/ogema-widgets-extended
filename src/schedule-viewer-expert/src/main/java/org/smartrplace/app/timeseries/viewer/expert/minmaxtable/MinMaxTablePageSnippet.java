/**
 * ﻿Licensed under the Apache License, Version 2.0 (the "License");
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
package org.smartrplace.app.timeseries.viewer.expert.minmaxtable;

import java.util.List;

import org.ogema.core.application.ApplicationManager;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.extended.html.bricks.PageSnippetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.DynamicTableData;

/**
 * Displays minimum, maximum and average for all time series plotted.
 * 
 * This is always a global widget, but the table displaying the data may show session-specific data
 */
@Deprecated
public class MinMaxTablePageSnippet extends PageSnippet {

	/**
	 * Use this to trigger an update of other widgets upon changes made to a schedule via this widget.
	 */
	public static final TriggeringAction SCHEDULE_CHANGED = new TriggeringAction("scheduleChanged");
	
	private static final long serialVersionUID = 550753654103033620L;   
	private List<DefaultSchedulePresentationDataPlus> defaultSchedule = null;
	protected final DynamicTable<DefaultSchedulePresentationDataPlus> table;  // TODO make this an actual subwidget; must be destroyed upon destruction of the manipulator?
	
	public final ApplicationManager appMan;
	/*
	 ************************** constructors ***********************/
    
    /** 
     * Default constructor: session dependent 
     */
    public MinMaxTablePageSnippet(WidgetPage<?> page, String id) {
        this(page, id, null);
	}
    
    public MinMaxTablePageSnippet(WidgetPage<?> page, String id, MinMaxTableConfiguration config,
    		ApplicationManager appMan) {
        this(page, id, false, config, appMan);
	}
    public MinMaxTablePageSnippet(WidgetPage<?> page, String id, MinMaxTableConfiguration config) {
        this(page, id, false, config);
	}
    
    public MinMaxTablePageSnippet(WidgetPage<?> page, String id, boolean globalWidget, MinMaxTableConfiguration config) {
    	this(page, id, globalWidget, config, null);
    }
    public MinMaxTablePageSnippet(WidgetPage<?> page, String id, boolean globalWidget, MinMaxTableConfiguration config,
    		ApplicationManager appMan) {
        super(page, id, true); // this itself is always a global widget
        this.appMan = appMan;
        if (config == null)
        	config = new MinMaxTableConfiguration();
        this.table = new DynamicTable<DefaultSchedulePresentationDataPlus>(page, id + "__MMA__table", globalWidget) {

			private static final long serialVersionUID = 1L;
			
			/*private List<DefaultSchedulePresentationDataPlus> getValues(OgemaHttpRequest req) {
				final TimeSeries schedule = getSchedule(req);
				if (schedule == null)
					return Collections.emptyList();
				final List<DefaultSchedulePresentationDataPlus> values = new ArrayList<>();
				values.add(MinMaxTableData.HEADER_LINE_ID);
				if (isAllowPointAddition(req)) {
					values.add(MinMaxTableData.NEW_LINE_ID);
				}
				final long startTime = startTimePicker.getDateLong(req);
				final Iterator<SampledValue> it = getSchedule(req).iterator(startTime, Long.MAX_VALUE);
				final int nrValues = nrItemsDropdown.getSelectedItem(req);
				int cnt = 0;
				while (cnt++ < nrValues && it.hasNext()) {
					values.add(it.next().getTimestamp());
				}
				return values;
			}*/
        	
			@Override
			public void onGET(OgemaHttpRequest req) {
				List<DefaultSchedulePresentationDataPlus> data = getSchedule(req);
				//clear(req);
				updateRows(data, req);
				//updateRows(getValues(req),req);
			}
        	
        };
 
        MinMaxTableRowTemplatePageSnippet  rowTemplate = new MinMaxTableRowTemplatePageSnippet(table,this,config.getAlert());
        table.setRowTemplate(rowTemplate);
        table.addDefaultStyle(DynamicTableData.CELL_ALIGNMENT_RIGHT);

        this.triggerAction(table, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
        this.triggerAction(this, SCHEDULE_CHANGED, TriggeredAction.GET_REQUEST);
        this.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
        
    	/*Flexbox fb = new Flexbox(page, id + "__XX__upperflexbox", true); // TODO Flexbox settings
        if (config.isShowInterpolationMode()) {
        	fb.addItem(interpolationLabel,null).addItem(interpolationDropdown, null);
        }
        fb.addItem(nrItemsLabel, null).addItem(nrItemsDropdown, null); // TODO new row?
        fb.addItem(startTimeLabel, null).addItem(startTimePicker, null);
    	this.append(fb, null).linebreak(null);
        this.append(table,null).linebreak(null);
    	fb = new Flexbox(page, id + "__XX__lowerflexbox", true); // TODO Flexbox settings
    	fb.addItem(firstButton, null).addItem(nextButton, null);
    	fb.setDefaultJustifyContent(JustifyContent.FLEX_LEFT);
    	this.append(fb, null);*/
        
        this.append(table,null).linebreak(null);
   }
    
    /*
     ******* Inherited methods ******/   
    
    /*
     * (non-Javadoc)
     * Duplicate of {@see org.ogema.tools.widget.html.calendar.datepicker.Datepicker#registerJsDependencies()} method; 
     * required because the Datepicker subwidgets of this widget are only created upon the first request 
     */
    /*@Override
    protected void registerJsDependencies() {
    	registerLibrary(true, "moment", "/ogema/widget/datepicker/lib/moment-with-locales_2.10.0.min.js"); // FIXME global moment variable will be removed in some future version
        registerLibrary(true, "jQuery.fn.datetimepicker", "/ogema/widget/datepicker/lib/bootstrap-datetimepicker_4.17.37.min.js"); 
    	super.registerJsDependencies();
    }*/
	
	@Override
	public MinMaxTableDataPageSnippet createNewSession() {
		MinMaxTableDataPageSnippet opt = new MinMaxTableDataPageSnippet(this);
		return opt;
	}
    
	@Override
	public MinMaxTableDataPageSnippet getData(OgemaHttpRequest req) {
		return (MinMaxTableDataPageSnippet) super.getData(req);
	}
	
	@Override
	protected void setDefaultValues(PageSnippetData opt) {
		super.setDefaultValues(opt);
		((MinMaxTableDataPageSnippet) opt).setSchedule(defaultSchedule);
	}
	
	@Override
	public void destroy() {
		this.table.destroyWidget();
		super.destroyWidget();
	}
	
	
	/*
	 ************************* public methods ***********************/
	
	public List<DefaultSchedulePresentationDataPlus> getDefaultSchedule() {
		return defaultSchedule;
	}

	public void setDefaultSchedule(List<DefaultSchedulePresentationDataPlus> defaultSchedule) {
		this.defaultSchedule = defaultSchedule;
	}

	public List<DefaultSchedulePresentationDataPlus> getSchedule(OgemaHttpRequest req) {
		return getData(req).getSchedule();
	}

	public void setSchedule(List<DefaultSchedulePresentationDataPlus> schedule, OgemaHttpRequest req) {
		getData(req).setSchedule(schedule);
	}
	
	/*
	 ************************* internal methods, etc ***********************/	
}