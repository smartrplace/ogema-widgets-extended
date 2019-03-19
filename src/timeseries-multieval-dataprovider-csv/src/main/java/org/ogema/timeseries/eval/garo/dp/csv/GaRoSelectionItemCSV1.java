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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.timeseries.eval.garo.dp.csv.util.CSVFileReader;
import org.ogema.timeseries.eval.garo.dp.csv.util.RoomTimeseriesData;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSelectionItem;

public class GaRoSelectionItemCSV1 extends GaRoSelectionItem {
	//only relevant for level GW_LEVEL
	private String gwId;
	//only relevant for TS_LEVEL
	private String realRoomId;
	
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
	public GaRoSelectionItemCSV1(String tsId, String roomId, GaRoSelectionItemCSV1 superSelectionItem,
			String realRoomId) {
		super(GaRoMultiEvalDataProvider.TS_LEVEL, tsId);
		this.gwId = superSelectionItem.gwId;
		this.gwSelectionItem = superSelectionItem.gwSelectionItem;
		this.roomSelectionItem = superSelectionItem;
		this.reader = superSelectionItem.reader;
		this.realRoomId = realRoomId;
	}
	
	@Override
	/** Note that in contrast to the GaRoMultiEvalDataProviderJAXB we only return the time
	 * series here that belong to the actual super item here and that we do not perform a
	 * further sorting out in the DataProvider#getOptions method here.
	 */
	protected List<String> getDevicePaths(GaRoSelectionItem roomSelItem) {
		return new ArrayList<String>(getDevicePathsInternal(roomSelItem).keySet());
		/*List<String> result = new ArrayList<>();
		if(level == GaRoMultiEvalDataProvider.GW_LEVEL ||
				roomSelItem.id().equals(GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID)) {
			for(RoomTimeseriesData room: reader.getData().roomData.values()) {
				result.addAll(room.tsData.keySet());
			}
		} else {
			RoomTimeseriesData room = reader.getData().roomData.get(id);
			if(room != null) result.addAll(room.tsData.keySet());
		}
		return result;*/
	}
	
	Map<String, String> getDevicePathsInternal() {
		return getDevicePathsInternal(roomSelectionItem);
	}
	/** Internal implementation of {@link #getDevicePaths(GaRoSelectionItem)} with extended result
	 * @return Map deviceId -> realRoomId that provides the time series*/
	Map<String, String> getDevicePathsInternal(GaRoSelectionItem roomSelItem) {
		Map<String, String> result = new HashMap<>();
		if(level == GaRoMultiEvalDataProvider.GW_LEVEL ||
				roomSelItem.id().equals(GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID)) {
			for(RoomTimeseriesData room: reader.getData().roomData.values()) {
				putAll(result, room.tsData.keySet(), room.roomId);
			}
		} else {
			RoomTimeseriesData room = reader.getData().roomData.get(id);
			if(room != null) putAll(result, room.tsData.keySet(), room.roomId);
		}
		return result;
	}
	
	private void putAll(Map<String, String> myMap, Set<String> keys, String value) {
		for(String key: keys) myMap.put(key, value);
	}
	
	@Override
	public TimeSeriesData getTimeSeriesData() {
		if(level == GaRoMultiEvalDataProvider.TS_LEVEL) {
			RoomTimeseriesData room;
			if(realRoomId != null)
				room = reader.getData().roomData.get(realRoomId);
			else
				room = reader.getData().roomData.get(roomSelectionItem.id());
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
