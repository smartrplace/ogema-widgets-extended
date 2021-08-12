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
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Data;

public interface JsonOGEMAFileManagementData extends Data {
	ResourceList<JsonOGEMAWorkspaceData> workspaceData();
	
	@Deprecated //use path defined by property only
	StringResource basePath();
	/** Reference to last workspace used*/
	JsonOGEMAWorkspaceData lastWorkspaceUsed();
}
