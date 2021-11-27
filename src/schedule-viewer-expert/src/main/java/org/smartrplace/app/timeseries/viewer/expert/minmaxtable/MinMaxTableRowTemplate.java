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
package org.smartrplace.app.timeseries.viewer.expert.minmaxtable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.humread.valueconversion.HumanReadableValueConverter;
import org.ogema.humread.valueconversion.HumanReadableValueConverter.LinearTransformation;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.label.Label;

// FIXME changing the time leads to display error and BAD quality; date changing works -> ?
// FIXME adding session dependent widgets to widget group?
public class MinMaxTableRowTemplate extends RowTemplate<DefaultSchedulePresentationDataPlus> {
	public static final float COMMENT_ONLY_VALUE_TS = -99876;
	
	// this is the DynamicTable widget
	private final OgemaWidget parent;
	// need this to trigger updates; will also trigger table update
	//private final MinMaxTable scheduleManipulator;
	// note: the alert may be null
	//private final Alert alert;
	//private final static TriggeredAction SCHEDULE_CHANGED = new TriggeredAction("scheduleChanged");
	
	public MinMaxTableRowTemplate(MinMaxTable scheduleManipulator) {
		this(scheduleManipulator, null);
	}
	
	public MinMaxTableRowTemplate(MinMaxTable scheduleManipulator,  Alert alert) {
		this.parent = scheduleManipulator;
		//this.scheduleManipulator = scheduleManipulator;
		//this.alert = alert;
	}
	
	@Override
	public Row addRow(final DefaultSchedulePresentationDataPlus object, OgemaHttpRequest req) {
		Row row = new Row();
		final String lineId = getLineId(object);
		
		final Label nameLabel = new Label(parent, lineId+ "_commentLabel", object.data.getLabel(null),req);
		row.addCell("Timeseries",nameLabel);
		
		LinearTransformation trans = HumanReadableValueConverter.getTransformationIfNonTrivial(object.data);
		
		List<SampledValue> vals = object.data.getValues(object.startTime, object.endTime);
		double sum = 0;
		int count = 0;
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		for(SampledValue sv: vals) {
			float val = sv.getValue().getFloatValue();
			if(val < min)
				min = val;
			if(val > max)
				max = val;
			if(Float.isNaN(val))
				continue;
			count++;
			sum += val;
		}
		if(count == 0) {
			final Label minLabel = new Label(parent, lineId+ "_minLabel", "--",req);
			row.addCell("Minimum", minLabel);
			final Label maxLabel = new Label(parent, lineId+ "_maxLabel", "--",req);
			row.addCell("Maximum", maxLabel);
			final Label avLabel = new Label(parent, lineId+ "_avLabel", "--",req);
			row.addCell("Average", avLabel);
			return row;
		}
		float av = (float) (sum/count);
		
		min = HumanReadableValueConverter.getHumanValue(min, trans);
		max = HumanReadableValueConverter.getHumanValue(max, trans);
		av = HumanReadableValueConverter.getHumanValue(av, trans);
		
		final Label minLabel = new Label(parent, lineId+ "_minLabel", String.format("%.1f", min),req);
		row.addCell("Minimum", minLabel);
		final Label maxLabel = new Label(parent, lineId+ "_maxLabel", String.format("%.1f", max),req);
		row.addCell("Maximum", maxLabel);
		final Label avLabel = new Label(parent, lineId+ "_avLabel", String.format("%.1f", av),req);
		row.addCell("Average", avLabel);

		return row;
	}
	
	@Override
	public String getLineId(DefaultSchedulePresentationDataPlus timestamp) {
		if (timestamp == null)
			return "_" + MinMaxTableData.NEW_LINE_ID; 
		return WidgetHelper.getValidWidgetId(timestamp.data.getLabel(null)+"_"+timestamp.id);
	}
	
	// FIXME
	@Override
	public Map<String, Object> getHeader() {
		final Map<String, Object> header = new LinkedHashMap<>();
		header.put("Timeseries", "Timeseries");
		header.put("Minimum", "Minimum");
		header.put("Maximum", "Maximum");
		header.put("Average", "Average");
		return header;
	}	
}
