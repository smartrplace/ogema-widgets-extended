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

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.DynamicTableData;

/**
 * Displays minimum, maximum and average for all time series plotted.
 * 
 * This is always a global widget, but the table displaying the data may show session-specific data
 */
public class MinMaxTable extends DynamicTable<DefaultSchedulePresentationDataPlus> {

	/**
	 * Use this to trigger an update of other widgets upon changes made to a schedule via this widget.
	 */
	public static final TriggeringAction SCHEDULE_CHANGED = new TriggeringAction("scheduleChanged");
	
	private static final long serialVersionUID = 550753654103033620L;   
	private List<DefaultSchedulePresentationDataPlus> defaultSchedule = null;
	//protected final DynamicTable<DefaultSchedulePresentationDataPlus> table;  // TODO make this an actual subwidget; must be destroyed upon destruction of the manipulator?
	
	public final ApplicationManager appMan;
	/*
	 ************************** constructors ***********************/
    
    /** 
     * Default constructor: session dependent 
     */
    public MinMaxTable(WidgetPage<?> page, String id, 
    		ApplicationManager appMan) {
        this(page, id, false, appMan);
	}
    public MinMaxTable(WidgetPage<?> page, String id) {
        this(page, id, false);
	}
    
    public MinMaxTable(WidgetPage<?> page, String id, boolean globalWidget) {
    	this(page, id, globalWidget, null);
    }
    public MinMaxTable(WidgetPage<?> page, String id, boolean globalWidget,
    		ApplicationManager appMan) {
        super(page, id + "__MMA__table", globalWidget); // this itself is always a global widget
        this.appMan = appMan;
 
        MinMaxTableRowTemplate  rowTemplate = new MinMaxTableRowTemplate(this);
        setRowTemplate(rowTemplate);
        addDefaultStyle(DynamicTableData.CELL_ALIGNMENT_RIGHT);

        //this.triggerAction(table, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
        //this.triggerAction(this, SCHEDULE_CHANGED, TriggeredAction.GET_REQUEST);
        //this.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
        
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
        
        //this.append(table,null).linebreak(null);
   }
    
	//@Override
	public void onGETInternal(List<DefaultSchedulePresentationDataPlus> data, OgemaHttpRequest req) {
		//List<DefaultSchedulePresentationDataPlus> data = getSchedule(req);
		//clear(req);
		updateRows(data, req);
		//updateRows(getValues(req),req);
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
	
	/*@Override
	public MinMaxTableData createNewSession() {
		MinMaxTableData opt = new MinMaxTableData(this);
		return opt;
	}
    
	@Override
	public MinMaxTableData getData(OgemaHttpRequest req) {
		return (MinMaxTableData) super.getData(req);
	}
	
	@Override
	protected void setDefaultValues(PageSnippetData opt) {
		super.setDefaultValues(opt);
		((MinMaxTableData) opt).setSchedule(defaultSchedule);
	}
	
	@Override
	public void destroy() {
		this.table.destroyWidget();
		super.destroyWidget();
	}*/
	
	
	/*
	 ************************* public methods ***********************/
	
	/*public List<DefaultSchedulePresentationDataPlus> getDefaultSchedule() {
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
	}*/
	
	/*
	 ************************* internal methods, etc ***********************/	
}