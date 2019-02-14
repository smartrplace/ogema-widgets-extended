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
package org.ogema.util.directresourcegui.jsonkpi;

import org.ogema.util.jsonresult.kpi.JSONStatisticalAggregation;

import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;

/** Extension of KPIStatisticsManagement for the KPIMonitoringReport template GUI class*/
public class KPIResultTypeJSON extends KPIStatisticsManagementJSON {

	public final boolean isHeader;
	public final String specialLindeId;

 	public KPIResultTypeJSON(String resultTypeId, GaRoSingleEvalProvider provider, JSONStatisticalAggregation sAgg,
			boolean forceCalculations) {
		super(resultTypeId , provider.id(), sAgg, forceCalculations);
		this.isHeader = false;
		this.specialLindeId = null;
	}
 	public KPIResultTypeJSON(KPIStatisticsManagementJSON kpiStatMan) {
		this(kpiStatMan, null);
	}
 	public KPIResultTypeJSON(KPIStatisticsManagementJSON kpiStatMan, String specialLindeId) {
		super(kpiStatMan.resultTypeId, kpiStatMan.providerId, kpiStatMan.sAgg, kpiStatMan.forceCalculations);
		this.isHeader = false;
		this.specialLindeId = specialLindeId;
	}

	/** Use this constructor only to generate a header line object*/
	public KPIResultTypeJSON(boolean isHeader) {
		super(null, null, null, false);
		this.isHeader = isHeader;
		this.specialLindeId = null;
	}

}
