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
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Configuration;

import com.iee.app.evaluationofflinecontrol.gui.OfflineEvaluationControl;

/** 
 * The global configuration resource type for this app.
 */
public interface OfflineEvaluationControlConfig extends Configuration {

	ResourceList<ProviderEvalOfflineConfig> knownProviders();
	StringArrayResource dataProvidersToUse();
	BooleanResource writeCsv();
	
	/** If true required pre-evaluation will be found and added automatically. In this case no
	 * pre-evaluation file has to be selected. Writing CSV is not supported then.
	 */
	BooleanResource autoPreEval();
	
	ResourceList<KPIPageConfig> kpiPageConfigs();
	
	/** If active and not empty only the gateways in this resource shall be used
	 * in page {@link OfflineEvaluationControl}
	 */
	StringArrayResource gatewaysToUse();
	StringArrayResource gatewaysToShow();
	
	/** If true in the eval start page a backup button will be offered*/
	BooleanResource showBackupButton();
	
	/** For the Zip-SCP-based Remote Supervision*/
	TimeResource lastAutoZipBackup();
}
