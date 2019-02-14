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
package org.ogema.timeseries.eval.eventlog.dataprovider;

import java.util.List;

import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.channelmanager.measurements.StringValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.tools.timeseries.api.FloatTimeSeries;
import org.ogema.tools.timeseries.implementations.FloatTreeTimeSeries;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSelectionItem;

/** Selection item for {@link GaRoEvalDataProviderGateway}
 * 
 */
public class GaRoSelectionItemEventlog extends GaRoSelectionItem {
	//only relevant for level GW_LEVEL
	private String gwId;
	//public Resource resource;
	public boolean isRoomLevel = false;
	public String eventId;
	
	public GaRoSelectionItemEventlog(String gwId) {
		super(GaRoMultiEvalDataProvider.GW_LEVEL, gwId);
		//this.appMan = appMan;
		this.gwId = gwId;
	}
	public GaRoSelectionItemEventlog(String name, GaRoSelectionItemEventlog superSelectionItem) {
		super(GaRoMultiEvalDataProvider.ROOM_LEVEL, name);
		this.gwSelectionItem = superSelectionItem;
		//this.gwId = superSelectionItem.gwId;
		//this.roomId = room.getKey();
		//this.resource = room;
		this.isRoomLevel = true;
	}
	public GaRoSelectionItemEventlog(String name, String eventId, GaRoSelectionItemEventlog superSelectionItem) {
		super(GaRoMultiEvalDataProvider.TS_LEVEL, name);
		this.gwId = superSelectionItem.gwId;
		this.gwSelectionItem = superSelectionItem.gwSelectionItem;
		this.roomSelectionItem = superSelectionItem;
		//this.tsId = tsId;
		//this.resource = singleValue;
		this.eventId = eventId;
		//this.appMan = appMan;
	}
	
	@Override
	protected List<String> getDevicePaths(GaRoSelectionItem roomSelItem) {
		throw new IllegalStateException("not supported!");
	}

	@Override
	public TimeSeriesData getTimeSeriesData() {
		if(level == GaRoMultiEvalDataProvider.TS_LEVEL) {
			//RecordedDataStorage recData = getLogRecorder().getRecordedDataStorage(tsId);
			FloatTimeSeries recData = new FloatTreeTimeSeries();
			//example
			long timeStamp = 0;
			String stringV = "test";
			Value value = new StringValue(stringV); //new StringFloatValue(stringV , -1f);
			recData.addValue(new SampledValue(value, timeStamp, Quality.GOOD));
			return new TimeSeriesDataImpl(recData, id,
					id, InterpolationMode.STEPS);
		}
		return null;
	}
	
	@Override
	public Integer getRoomType() {
		throw new IllegalStateException("not relevant - provider only works on Overall room!");
	}

	@Override
	public String getRoomName() {
		throw new IllegalStateException("not relevant - provider only works on Overall room!");
	}

	@Override
	public String getPath() {
		if(gwId == null) return null;
		if(eventId == null) return gwId;
		return gwId+"#"+eventId;
	}

}
