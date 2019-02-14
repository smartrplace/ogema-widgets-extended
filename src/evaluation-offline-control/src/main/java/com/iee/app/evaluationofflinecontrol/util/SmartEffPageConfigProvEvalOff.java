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
package com.iee.app.evaluationofflinecontrol.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.smartrplace.smarteff.access.api.ConfigInfoExt;
import org.smartrplace.smarteff.access.api.DefaultGenericPageConfigurationProvider;
import org.smartrplace.smarteff.access.api.GenericPageConfigurationProvider;

@Service(GenericPageConfigurationProvider.class)
@Component
public class SmartEffPageConfigProvEvalOff extends DefaultGenericPageConfigurationProvider {
	public static final String PROVIDER_ID = "SmartEff_OfflineEvaluationControlSP";

	protected static Map<String, ConfigInfoExt> configs = new HashMap<>();
	protected static int lastConfig = 0;

	@Override
	protected Map<String, ConfigInfoExt> configs() {
		return configs;
	}
	
	@Override
	public String id() {
		return PROVIDER_ID;
	}

	@Override
	protected String getNextId() {
		lastConfig = super.getNextId(lastConfig, MAX_ID);
		return ""+lastConfig;
	}
	
	public static DefaultGenericPageConfigurationProvider getInstance() {
		if(instance == null) instance = new SmartEffPageConfigProvEvalOff();
		return instance;
	}

	protected static volatile SmartEffPageConfigProvEvalOff instance = null;
	@Override
	protected DefaultGenericPageConfigurationProvider getInstanceObj() {
		return instance;
	}

	@Override
	protected void setInstance(DefaultGenericPageConfigurationProvider instanceIn) {
		instance = (SmartEffPageConfigProvEvalOff) instanceIn;
		
	}

}
