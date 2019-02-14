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
package org.ogema.model.jsonresult;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.array.IntegerArrayResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.model.alignedinterval.StatisticalAggregation;

/** See {@link MultiEvalStartConfiguration} for documentation.
 * TODO: Check if elements that are not relevant for auto-queuing should be moved to separate resource type.
 * As MultiKPIEvalConfiguation is already used mostly for evaluation utils etc. it probably would
 * be best to create a new model for this. {@link MultiEvalStartConfiguration} could be integrated here
 * and be removed, this should not be very difficult and there should be no resources of the type anywhere.
 */
public interface MultiKPIEvalConfiguration extends MultiEvalStartConfiguration {
	/** Interval types for which KPIs shall be calculated. The intervals given
	 * here should be longer then {@link #stepInterval()} and it should be
	 * possible to calculate them from the stepInterval. If the resource is not active or empty
	 * no auto-scheduling takes place.*/
	IntegerArrayResource kpisToCalculate();
	
	/**If false only the KPIs are stored persistently*/
	BooleanResource saveJsonResults();

	/**Only the elements given in {@link #kpisToCalculate()} are actually updated. Usually the
	 * KPI resources are created here.
	 */
	ResourceList<StatisticalAggregation> resultKPIs();

	/* Note: The following elements are usually only relevant for auto-scheduling*/
	
	/**If true the evaluation will  after every start-up of
	 * the app (if there are new intervals to be calculated). This will only take
	 * effect if the criteria for auto-queuing are fulfilled (see {@link #performAutoQueuing()})
	 */
	BooleanResource queueOnStartup();
	/** Perform queueing after fixed interval, non-aligned. Aligned queueing might be 
	 * supported in the future, but might not be necessary at all. If interval is
	 * negative or zero no auto-queueing is performed. If this Resource or the parent resource of
	 * type MultiKPIEvalConfiguration is not active or
	 * {@link MultiKPIEvalConfiguration#kpisToCalculate()} is not active
	 * also no auto-Queuing is performed.*/
	BooleanResource performAutoQueuing();
	
	/** If true and more than one gateway is selected then the results will be calculated for all
	 * gateways individually as well as for the overall result*/
	BooleanResource queueGatewaysIndividually();
	/** Shall only be used if {@link #queueGatewaysIndividually()} is true. The names of the elements shall be
	 * the gateways id for which the sub-resource list is relevant.
	 */
	//ResourceList<ResourceList<StatisticalAggregation>> individualResultKPIs();
	ResourceList<IndividualGWResultList> individualResultKPIs();
}
