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

package org.ogema.timeseries.eval.garo.dp.csv.util;

import java.util.HashMap;
import java.util.Map;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;

public class GatewayTimeseriesData {
	public String gwId;
	public Map<String, RoomTimeseriesData> roomData = new HashMap<>();
	
	public boolean addTimeSeries(String roomId, String tsId, ReadOnlyTimeSeries ts) {
		if(roomId == null)
			roomId = GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID;
		RoomTimeseriesData rdd = roomData.get(gwId);
		boolean result = false;
		if(rdd == null) {
			rdd = new RoomTimeseriesData();
			rdd.roomId = roomId;
			roomData.put(gwId, rdd);
			result = true;
		}
		rdd.addTimeseriesData(tsId, ts);
		return result;
	}
}
