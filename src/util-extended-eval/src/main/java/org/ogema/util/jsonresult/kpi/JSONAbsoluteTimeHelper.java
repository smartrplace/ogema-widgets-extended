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

import org.ogema.tools.timeseries.api.FloatTimeSeries;

public class JSONAbsoluteTimeHelper {
	public static FloatTimeSeries getIntervalTypeStatistics(int intervalType, JSONStatisticalAggregation sAgg) {
		switch(intervalType) {
		case 1:
			return sAgg.yearValue();
		case 2:
			throw new IllegalStateException("intervaltype "+intervalType+" not supported yet for JSON!");
		case 3:
			return sAgg.monthValue();
		case 6:
			return sAgg.weekValue();
		case 10:
			return sAgg.dayValue();
		case 15:
			throw new IllegalStateException("intervaltype "+intervalType+" not supported yet for JSON!");
		case 100:
			return sAgg.hourValue();
		case 101:
			return sAgg.minuteValue();
		case 102:
			return sAgg.secondValue();
		case 220:
			throw new IllegalStateException("intervaltype "+intervalType+" not supported yet for JSON!");
		case 240:
			throw new IllegalStateException("intervaltype "+intervalType+" not supported yet for JSON!");
		case 320:
			throw new IllegalStateException("intervaltype "+intervalType+" not supported yet for JSON!");
		case 1000:
			throw new IllegalStateException("intervaltype "+intervalType+" not supported yet for JSON!");
		case 1020:
			throw new IllegalStateException("intervaltype "+intervalType+" not supported yet for JSON!");
		default:
			throw new UnsupportedOperationException("Interval type "+intervalType+" not supported!");
		}	
	}

}
