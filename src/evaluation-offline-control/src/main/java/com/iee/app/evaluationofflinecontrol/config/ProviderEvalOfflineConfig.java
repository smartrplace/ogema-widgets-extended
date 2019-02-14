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
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.alignedinterval.StatisticalAggregation;
import org.ogema.model.prototypes.Configuration;

/** 
 * Put an instance of this resource type for each program into a ResourceList
 */
public interface ProviderEvalOfflineConfig extends Configuration {

	StringResource providerId();
	
	BooleanResource includeIntoStandardEval();
	
	ResourceList<StatisticalAggregation> resultKPIs();
}
