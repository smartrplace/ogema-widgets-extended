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

import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;

/** See {@link MultiKPIEvalConfiguration}*/
public class JSONMultiKPIEvalResult {
	/** ResultId -> Result*/
	public Map<String, JSONStatisticalAggregation> resultKPIs;
	/** GwId -> <Map of Results>*/
	public Map<String, JSONIndividualGWResultList> individualResultKPIs;
	
	/** Location of the respective {@link MultiKPIEvalConfiguration}*/
	//Still supported for legacy JSON files
	public String multiConfigLocation;
	//Use this identifier now
	public String evalProviderId;
}
