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
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.TimeResource;

/** Information on JSON result files to be stored in the OGEMA system.*/
public interface JSONResultFileData extends JsonOGEMAFileData {
	StringArrayResource gatewaysIncluded();
	
	/** TODO: Offer both options here ?*/
	/** The list shall contain references on the data of the files used for
	 * pre-evaluation. Providing file names may be much more ambigious, but it
	 * should not be necessary to create a JSONResultFileData-resource for every
	 * file referenced here.
	 */
	ResourceList<JSONResultFileData> preEvaluationsUsed();
	StringArrayResource preEvaluationFilesUsed();
	
	TimeResource startTime();
	TimeResource endTime();
	TimeResource stepSize();
	/** If no gaps occur timeIntervalNum = (endTime - startTime)/stepSize*/
	IntegerResource timeIntervalNum();
	
	/** Reference on start configuration may be provided*/
	MultiEvalStartConfiguration startConfiguration();
}
