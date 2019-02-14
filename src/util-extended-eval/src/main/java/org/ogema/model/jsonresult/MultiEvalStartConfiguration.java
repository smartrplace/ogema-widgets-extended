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

import org.ogema.core.model.Resource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Data;

import de.iwes.util.timer.AbsoluteTiming;

/** Basic data for multi-evaluation KPI tasks that should be sufficient to run such a task once. Information that is
 * usually relevant to store for consecutive evaluations is stored in {@link MultiKPIEvalConfiguration}.
 * Note that currently most evaluation utils only support MultiKPIEvalConfiguration and that the
 * information provided there is not only required for auto-scheduling.
 */
public interface MultiEvalStartConfiguration extends Data {
	StringResource evaluationProviderId();
	/** For each EvaluationProvider several configurations may exist, e.g. for
	 * different users or separate evaluations on different sets of gateways. If
	 * this is not set only a single configuration for the EvaluationProvider should
	 * exist.
	 */
	StringResource subConfigId();
	
	/** Multi-evaluation interval step size
	 * see {@link AbsoluteTiming} for interval type values<br>
	 * If Auto-Queuing is active a new evaluation is queued at the beginning of the next stepInterval*/
	IntegerResource stepInterval();
	
	/**Number of intervals to evaluation from a startTime that is not configured here. If not
	 * present the evaluation shall be performed until the last completed interval before the
	 * current time is calculated.*/
	IntegerResource intervalsToEvaluate();
	
	/**
	 * 0 : Do not take care of pre-evaluation providers being executed. If the necessary pre-
	 * 		evaluation data is not available, the evaluation will not start or fail. The evaluation
	 * 		will be removed from queue anyways when execution is foreseen
	 * 1: Check for availability of pre-evaluation when execution is foreseen and move to end of
	 * 		queue if not all necessary data is available (not supported yet)
	 * 2: Queue pre-evaluation automatically before the evaluation
	 */
	IntegerResource preEvaluationSchedulingMode();
	
	/** -1: Do not check for existing data, execute and write result anyways<br>
	 *  0: Only execute for part of interval for which no data is available yet (recommended
	 *  		in most cases)<br>
	 *  1: Only execute if no data for the interval is available at all 
	 */
	IntegerResource overwriteMode();
	
	/** If not set all results of the provider shall be calculated*/
	StringArrayResource resultsRequested();
	
	/** If not set all available gateways shall be used*/
	StringArrayResource gwIds();
	
	BooleanResource isRunning();
	
	/**If this resource is active it will be given to the evaluation as configuration*/
	Resource configurationResource();
}
