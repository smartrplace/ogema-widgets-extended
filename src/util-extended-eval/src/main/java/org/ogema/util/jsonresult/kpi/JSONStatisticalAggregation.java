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
package org.ogema.util.jsonresult.kpi;

import java.util.Map;

import org.ogema.model.alignedinterval.StatisticalAggregation;
import org.ogema.tools.timeseries.api.FloatTimeSeries;
import org.ogema.tools.timeseries.implementations.FloatTreeTimeSeries;

import de.iwes.timeseries.eval.api.helper.EfficientTimeSeriesArray;

/** See {@link StatisticalAggregation}*/
public class JSONStatisticalAggregation {
	public JSONStatisticalAggregation() {
		super();
	}

	public String resourceName;
	public Map<Long, String> stringTimeSeries; 
	
	FloatTimeSeries secondValue;
	public EfficientTimeSeriesArray getSecondValue() {
		return EfficientTimeSeriesArray.getInstance(secondValue);
	}
	public void setSecondValue(EfficientTimeSeriesArray value) {
		secondValue = value.toFloatTimeSeries(); //EfficientTimeSeriesArray.setValue(value);
	}
	public FloatTimeSeries secondValue() {
		if(secondValue == null) secondValue = new FloatTreeTimeSeries();
		return secondValue;
	}

	FloatTimeSeries minuteValue;
	public EfficientTimeSeriesArray getMinuteValue() {
		return EfficientTimeSeriesArray.getInstance(minuteValue);
	}
	public void setMinuteValue(EfficientTimeSeriesArray value) {
		minuteValue = value.toFloatTimeSeries(); //EfficientTimeSeriesArray.setValue(value);
	}
	public FloatTimeSeries minuteValue() {
		if(minuteValue == null) minuteValue = new FloatTreeTimeSeries();
		return minuteValue;
	}

	FloatTimeSeries hourValue;
	public EfficientTimeSeriesArray getHourValue() {
		return EfficientTimeSeriesArray.getInstance(hourValue);
	}
	public void setHourValue(EfficientTimeSeriesArray value) {
		hourValue = value.toFloatTimeSeries(); //EfficientTimeSeriesArray.setValue(value);
	}
	public FloatTimeSeries hourValue() {
		if(hourValue == null) hourValue = new FloatTreeTimeSeries();
		return hourValue;
	}
	
	FloatTimeSeries dayValue;
	public EfficientTimeSeriesArray getDayValue() {
		return EfficientTimeSeriesArray.getInstance(dayValue);
	}
	public void setDayValue(EfficientTimeSeriesArray value) {
		dayValue = value.toFloatTimeSeries(); //EfficientTimeSeriesArray.setValue(value);
	}
	public FloatTimeSeries dayValue() {
		if(dayValue == null) dayValue = new FloatTreeTimeSeries();
		return dayValue;
	}
	
	FloatTimeSeries weekValue;
	public EfficientTimeSeriesArray getWeekValue() {
		return EfficientTimeSeriesArray.getInstance(weekValue);
	}
	public void setWeekValue(EfficientTimeSeriesArray value) {
		weekValue = value.toFloatTimeSeries(); //EfficientTimeSeriesArray.setValue(value);
	}
	public FloatTimeSeries weekValue() {
		if(weekValue == null) weekValue = new FloatTreeTimeSeries();
		return weekValue;
	}

	FloatTimeSeries monthValue;
	public EfficientTimeSeriesArray getMonthValue() {
		return EfficientTimeSeriesArray.getInstance(monthValue);
	}
	public void setMonthValue(EfficientTimeSeriesArray value) {
		monthValue = value.toFloatTimeSeries(); //EfficientTimeSeriesArray.setValue(value);
	}
	public FloatTimeSeries monthValue() {
		if(monthValue == null) monthValue = new FloatTreeTimeSeries();
		return monthValue;
	}

	FloatTimeSeries yearValue;
	public EfficientTimeSeriesArray getYearValue() {
		return EfficientTimeSeriesArray.getInstance(yearValue);
	}
	public void setYearValue(EfficientTimeSeriesArray value) {
		yearValue = value.toFloatTimeSeries(); //EfficientTimeSeriesArray.setValue(value);
	}
	public FloatTimeSeries yearValue() {
		if(yearValue == null) yearValue = new FloatTreeTimeSeries();
		return yearValue;
	}

	/** Last time any aggregation field was updated*/
	FloatTimeSeries lastUpdate;
	public EfficientTimeSeriesArray getlastUpdate() {
		return EfficientTimeSeriesArray.getInstance(lastUpdate);
	}
	public void setlastUpdate(EfficientTimeSeriesArray value) {
		lastUpdate = value.toFloatTimeSeries(); //EfficientTimeSeriesArray.setValue(value);
	}
	public FloatTimeSeries lastUpdate() {
		if(lastUpdate == null) lastUpdate = new FloatTreeTimeSeries();
		return lastUpdate;
	}
	
	public int minimalInterval() {
		int type = -1;
		if(secondValue != null) {
			type = 102;
		} else if(minuteValue != null) {
			type = 101;
		} else if(hourValue != null) {
			type = 100;
		} else if(dayValue != null) {
			type = 10;
		} else if(weekValue != null) {
			type = 6;
		} else if(monthValue != null) {
			type = 3;
		} else if(yearValue != null) {
			type = 1;
		}
		return type;
	}

}
