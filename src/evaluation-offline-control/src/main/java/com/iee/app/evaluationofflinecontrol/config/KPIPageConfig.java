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
package com.iee.app.evaluationofflinecontrol.config;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Data;

import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;

public interface KPIPageConfig extends Data {
	ResourceList<ResultToShow> resultsToShow();
	StringResource pageId();
	IntegerResource defaultColumnsIntoPast();
	/** If active and not empty only the gateways in this resource shall be shown in the
	 * respective page
	 */
	StringArrayResource gatewaysToShow();
	
	/**If active and true only lines for the single gateways shall be shown*/
	BooleanResource hideOverallLine();
	
	/**Id of provider that defines the page config if existing. This is relevant to
	 * identify the place where a messageProvider can be accessed.
	 */
	StringResource sourceProviderId();
	/**Key of message provider when accessed via {@link GaRoSingleEvalProvider#getMessageProvider(String)}*/
	StringResource messageProviderId();
}
