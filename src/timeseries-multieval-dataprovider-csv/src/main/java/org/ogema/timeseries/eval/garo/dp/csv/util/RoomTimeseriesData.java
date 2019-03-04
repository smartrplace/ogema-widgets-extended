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
import org.ogema.tools.timeseries.implementations.FloatTreeTimeSeries;

import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;

public class RoomTimeseriesData {
	public String roomId;
	/** timeseriesId -> data*/
	public Map<String, TimeSeriesDataExtendedImpl> tsData = new HashMap<>();
	
	public boolean addTimeseriesData(String id, ReadOnlyTimeSeries ts) {
		TimeSeriesDataExtendedImpl tsExist = tsData.get(id);
		if(tsExist == null) {
			FloatTreeTimeSeries ftts;
			if(ts instanceof FloatTreeTimeSeries)
				ftts = (FloatTreeTimeSeries) ts;
			else {
				ftts = new FloatTreeTimeSeries();
				ftts.add(ts);
			}
			tsExist = new TimeSeriesDataExtendedImpl(ts, id, id, null);
			tsData.put(id, tsExist);
			return true;
		}
		FloatTreeTimeSeries ftts = (FloatTreeTimeSeries) tsExist.getTimeSeries();
		ftts.add(ts);
		return false;
	}
}
