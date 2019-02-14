/**
 * Copyright 2009 - 2016
 *
 * Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Fraunhofer IWES
 *
 * All Rights reserved
 */
package org.ogema.tools.app.logtransfercontrol.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.model.communication.CommunicationStatus;
import org.ogema.tools.app.logtransfercontrol.LogTransferController;
import org.ogema.tools.app.logtransfercontrol.localisation.ActivateLogModusDictionary;
import org.ogema.tools.app.logtransfercontrol.model.DataLogTransferInfo;
import org.ogema.tools.app.logtransfercontrol.template.SingleValueResourceTemplate;
import org.slf4j.Logger;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.alert.AlertData;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.DynamicTableData;
import de.iwes.widgets.html.form.label.Header;

// FIXME global resource table not compatible with OGEMA security; all devices will be shown, independently of user permissions
public class PageBuilder {

	protected final ApplicationManager appMan; 
	protected final Logger logger;
	protected final DynamicTable<SingleValueResource> devicesTable;

    
	public PageBuilder(final WidgetPage<?> page, final ApplicationManager appMan, final ResourceList<DataLogTransferInfo> dataLogs, LogTransferController controller) {
		this.appMan = appMan;
		this.logger = appMan.getLogger();
		Objects.requireNonNull(dataLogs);
		
	    final Header header = new Header(page, "header","Activate Log Modus App") {
			private static final long serialVersionUID = 1L;

			@Override
	    	public void onGET(OgemaHttpRequest req) {
	    		setText(((ActivateLogModusDictionary) getPage().getDictionary(req)).header(), req);
	    	}
    	
	    };
	    header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_CENTERED);
	    page.append(header).linebreak();
	    
	    final Alert info = new Alert(page, "description","Explanation") {
			private static final long serialVersionUID = 1L;

			@Override
	    	public void onGET(OgemaHttpRequest req) {
	    		setText(((ActivateLogModusDictionary) getPage().getDictionary(req)).description(), req);
	    		allowDismiss(true, req);
	    		autoDismiss(-1, req);
	    	}
	    	
	    };
	    page.append(info).linebreak();
	    info.addDefaultStyle(AlertData.BOOTSTRAP_INFO);
	    info.setDefaultVisibility(true);
	    
		this.devicesTable = new DynamicTable<SingleValueResource>(page, "PageBuilder_DeviceTable", true) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				controller.getLoggableResources().forEach(r -> addItem(r, null));
		        // For logging the CommunicationStatus resource of all PhysicalElements (which own such a subresource)
		        List<CommunicationStatus> csList = appMan.getResourceAccess().getResources(CommunicationStatus.class);
			    for(CommunicationStatus cs : csList) {
			    	this.addItem(cs.communicationDisturbed(), req);
			    }
			}
			
		};
			SingleValueResourceTemplate template = new SingleValueResourceTemplate(page, controller, devicesTable, dataLogs);
			devicesTable.setRowTemplate(template);
		devicesTable.setDefaultRowIdComparator(String.CASE_INSENSITIVE_ORDER);
		
		List<WidgetStyle<?>> styles = new ArrayList<>();
		styles.add(DynamicTableData.TEXT_ALIGNMENT_CENTERED);
		devicesTable.setDefaultStyles(styles);
        devicesTable.setColumnSize(SingleValueResourceTemplate.DEVICE_PATH_COLUMN, 10, null);

		page.append(devicesTable);
	}
	
}
