package org.ogema.tools.app.logtransfercontrol.template;

import java.util.LinkedHashMap;
import java.util.Map;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.tools.app.logtransfercontrol.LogTransferController;
import org.ogema.tools.app.logtransfercontrol.model.DataLogTransferInfo;
import org.ogema.tools.resource.util.LoggingUtils;
import org.ogema.tools.resource.util.ResourceUtils;
import org.slf4j.Logger;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.label.Label;

public class SingleValueResourceTemplate extends RowTemplate<SingleValueResource> {
    
    public static final String DEVICE_TYPE_COLUMN = "deviceNameColumn";
    public static final String DEVICE_PATH_COLUMN = "devicePathColumn";
	private static final String DEVICE_LOG_COLUMN = "deviceLogColumn";
	private static final String DEVICE_TRANSMIT_COLUMN = "deviceTransmitColumn";

	protected final WidgetPage<?> page;
	protected final LogTransferController app;
	protected final Logger logger;
	protected final DynamicTable<SingleValueResource> table;
	
	public SingleValueResourceTemplate(WidgetPage<?> page, LogTransferController app, DynamicTable<SingleValueResource> table, 
			ResourceList<DataLogTransferInfo> dataLogs) {
		this.page = page;
		this.app = app;
		this.logger = app.getAppMan().getLogger();
		this.table = table;
	}
	
	@Override
	public Row addRow(final SingleValueResource resource, OgemaHttpRequest req) {
		Row row = new Row();
		final String id = getLineId(resource);

		Label nameLabel = new Label(page, id + "_nameLabel");
        nameLabel.setDefaultHtml(String.format(
        		"<div style='text-align: left;'><span style='font-weight: bold;'>%s</span></div>", 
                resource.getResourceType().getSimpleName().replace("Resource", ""), resource.getPath()));
		row.addCell(DEVICE_TYPE_COLUMN, nameLabel);
		
		Label pathLabel = new Label(page, id + "_pathLabel");
		pathLabel.setDefaultHtml(String.format("<div style='text-align: left;'>%s</div>", resource.getPath()));
		row.addCell(DEVICE_PATH_COLUMN, pathLabel);
		
		final Button transmitLogDataButton = new Button(page, id + "_transmitLogDataButton", true) {
			
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				DataLogTransferInfo log = null;
				
				for(DataLogTransferInfo dl : app.getAppMan().getResourceAccess().getResources(DataLogTransferInfo.class)) {
					if(dl.clientLocation().getValue().equals(resource.getPath()))
						log = dl;
				}
				
				if(log != null && log.isActive()) {
					this.setText("On", null);
					if(LoggingUtils.isLoggingEnabled(resource))
						this.setStyle(ButtonData.BOOTSTRAP_GREEN, null);
					else 
						this.setStyle(ButtonData.BOOTSTRAP_ORANGE, null);
				} else {
					this.setText("Off", null);
					this.setStyle(ButtonData.BOOTSTRAP_RED, null);
				}
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				
				final String path = resource.getPath();
				final DataLogTransferInfo log = app.getAppMan().getResourceAccess().getResources(DataLogTransferInfo.class).stream()
					.filter(dl ->path.equals(dl.clientLocation().getValue()))
					.findAny()
					.orElse(null);
				if(log != null && this.getText(req).equals("On")) {
					this.setText("Off", req);
					this.setStyle(ButtonData.BOOTSTRAP_RED, req);
					log.delete();
					logger.info("Transmitting logdata for resource '" + resource.getPath() + "' disabled");
				} else if(log == null || this.getText(req).equals("Off")) {
					this.setText("On", req);
					if(LoggingUtils.isLoggingEnabled(resource))
						this.setStyle(ButtonData.BOOTSTRAP_GREEN, req);
					else 
						this.setStyle(ButtonData.BOOTSTRAP_ORANGE, req);
					
					app.startTransmitLogData(resource);
					logger.info("Transmitting logdata for resource '" + resource.getPath() + "' enabled");
				} else {
					if(log != null)
						log.delete();
					this.setText("Off", req);
					this.setStyle(ButtonData.BOOTSTRAP_RED, req);
				}
			}
		};
		transmitLogDataButton.addDefaultStyle(ButtonData.BOOTSTRAP_RED);
		transmitLogDataButton.setDefaultText("Off");
		transmitLogDataButton.triggerAction(transmitLogDataButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		row.addCell(DEVICE_TRANSMIT_COLUMN, transmitLogDataButton);
		
		final Button loggingActiveButton = new Button(page, id + "_loggingActiveButton", true) {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(LoggingUtils.isLoggingEnabled(resource)) {
					this.setText("On", req);
					this.setStyle(ButtonData.BOOTSTRAP_GREEN, req);
				} else {
					this.setText("Off", req);
					this.setStyle(ButtonData.BOOTSTRAP_RED, req);
				}
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				if(LoggingUtils.isLoggingEnabled(resource)) {
					this.setText("Off", req);
					this.setStyle(ButtonData.BOOTSTRAP_RED, req);
					if(transmitLogDataButton.getText(req).equals("On"))
						transmitLogDataButton.setStyle(ButtonData.BOOTSTRAP_ORANGE, req);
					LoggingUtils.deactivateLogging(resource);
					logger.info("Logging for resource '" + resource.getPath() + "' disabled");
				} else {
					this.setText("On", req);
					this.setStyle(ButtonData.BOOTSTRAP_GREEN, req);
					if(transmitLogDataButton.getText(req).equals("On"))
						transmitLogDataButton.setStyle(ButtonData.BOOTSTRAP_GREEN, req);
					LoggingUtils.activateLogging(resource, -2);
					logger.info("Logging for resource '" + resource.getPath() + "' enabled");
				}
			}
		};
		loggingActiveButton.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);
		loggingActiveButton.setDefaultText("On");
		loggingActiveButton.triggerAction(loggingActiveButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		loggingActiveButton.triggerAction(transmitLogDataButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		row.addCell(DEVICE_LOG_COLUMN, loggingActiveButton);
		
		return row;
	}
	
	private void startLogAllResources(SingleValueResource resource) {
		LoggingUtils.activateLogging(resource, -2);
	}

	@Override
	public Map<String, Object> getHeader() {
		Map<String, Object> header = new LinkedHashMap<String, Object>();
		header.put(DEVICE_TYPE_COLUMN, "Type");
		header.put(DEVICE_PATH_COLUMN, "Path");
		header.put(DEVICE_LOG_COLUMN, "Logging");
		header.put(DEVICE_TRANSMIT_COLUMN, "Transmitting");
		return header;
	}

	@Override
	public String getLineId(SingleValueResource resource) {
		return ResourceUtils.getValidResourceName(resource.getPath());
	}

}
