/**
 * ﻿Copyright 2018-2019 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
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

package org.ogema.timeseries.eval.garo.dp.csv;

import java.util.ArrayList;
import java.util.List;

import org.ogema.timeseries.eval.garo.dp.csv.util.CSVFileReader;
import org.ogema.timeseries.eval.garo.dp.csv.util.RoomTimeseriesData;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSelectionItem;

public class GaRoSelectionItemCSV1 extends GaRoSelectionItem {
	//only relevant for level GW_LEVEL
	private String gwId;
	
	protected CSVFileReader reader;
	
	//only relevant for level ROOM_LEVEL, TS_LEVEL
	public GaRoSelectionItemCSV1 getGwSelectionItem() {
		return (GaRoSelectionItemCSV1) gwSelectionItem;
	}
	
	public GaRoSelectionItemCSV1(String gwId,  CSVFileReader reader) {
		super(GaRoMultiEvalDataProvider.GW_LEVEL, gwId);
		this.gwId = gwId;
		this.reader = reader;
	}
	public GaRoSelectionItemCSV1(String name, GaRoSelectionItemCSV1 superSelectionItem) {
		super(GaRoMultiEvalDataProvider.ROOM_LEVEL, name);
		this.reader = superSelectionItem.reader;
		this.roomSelectionItem = this;
	}
	public GaRoSelectionItemCSV1(String tsId, String roomId, GaRoSelectionItemCSV1 superSelectionItem) {
		super(GaRoMultiEvalDataProvider.TS_LEVEL, tsId);
		this.gwId = superSelectionItem.gwId;
		this.gwSelectionItem = superSelectionItem.gwSelectionItem;
		this.roomSelectionItem = superSelectionItem;
		this.reader = superSelectionItem.reader;
	}
	
	@Override
	protected List<String> getDevicePaths(GaRoSelectionItem roomSelItem) {
		List<String> result = new ArrayList<>();
		if(level == GaRoMultiEvalDataProvider.GW_LEVEL) {
			for(RoomTimeseriesData room: reader.getData().roomData.values()) {
				result.addAll(room.tsData.keySet());
			}
		} else {
			RoomTimeseriesData room = reader.getData().roomData.get(id);
			if(room != null) result.addAll(room.tsData.keySet());
		}
		return result;
	}
	
	@Override
	public TimeSeriesData getTimeSeriesData() {
		if(level == GaRoMultiEvalDataProvider.TS_LEVEL) {
			RoomTimeseriesData room = reader.getData().roomData.get(roomSelectionItem.id());
			if(room == null)
				throw new IllegalStateException("Data for room "+roomSelectionItem.id()+" not found!");
			return room.tsData.get(id);
		}
		return null;
	}
	
	@Override
	public String getRoomName() {
		if(roomSelectionItem == null) return id;
		return roomSelectionItem.id();
	}

	@Override
	public String getPath() {
		if(roomSelectionItem == null) return gwId;
		else if(level == GaRoMultiEvalDataProvider.ROOM_LEVEL) return gwId+"/"+id;
		return gwId+"/"+roomSelectionItem.id()+"/"+id;
	}

	@Override
	public Integer getRoomType() {
		//we cannot provide this information here
		return null;
	}
}
