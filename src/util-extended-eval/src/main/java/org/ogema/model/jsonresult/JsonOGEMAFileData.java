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

import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Data;

/** Information on JSON result files to be stored in the OGEMA system. Each Workspace shall be a set
 * of data with dependencies that works usually without outside dependencies. So it is is possible
 * to copy an entire workspace like an Excel file with several tables inside. Usually each workspace
 * contains three major subdirectories:<br>
 * - Temporary files that usually shall be clean up on every clean start or on every evaluation run.
 * These files also may be deleted by the Multi-Evaluation itself as they are just used to transfer
 * information between evaluation steps
 * - Exmperimental files that usually shall be cleaned up when a development process or
 *   an "experiment" is finished
 * - Major result files that shall be kept for further evaluations, documentation etc.*/
public interface JsonOGEMAFileData extends Data {
	/** Absolute file path on the system. This information is only provided
	 * as an exception for performance optimization. Usually the path
	 * should be determined based on a root directory system property or
	 * a specific setting of the management instance, the workspace,
	 * the status (possibly) and
	 * the path relative to the workspace. In this way files and the
	 * respective resources (as OGEMA-JSON/XML-Export) can be transferred
	 * between systems easily.
	 * Note: Workspace information shall be obtained from the parent-parent-resource
	 */
	//StringResource filePath();
	//StringResource workSpace();
	IntegerResource status();
	/** For smaller workspaces without a further sub-structure except
	 * separation in temporary/experimental/major results this is just the
	 * file name
	 */
	StringResource workSpaceRelativePath();
	
	/** In general an indication should provided here which class extending LabeledItem generated
	 * the object. It may not be set.
	 */
	StringResource evaluationProviderId();
	StringResource resultClassName();
}
