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
package org.ogema.util.directresourcegui.jsonkpi;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.util.jsonresult.kpi.JSONMultiKPIEvalResult;
import org.ogema.util.jsonresult.management.JsonOGEMAFileManagementImpl;
import org.ogema.util.jsonresult.management.api.JsonOGEMAFileManagement;

import de.iwes.timeseries.eval.api.extended.util.MultiEvaluationUtils;

public class KPIStatisticsJSONFileManagement {
	private Map<String, JSONMultiKPIEvalResult> data = new HashMap<>();
	private final JsonOGEMAFileManagement<?, ?> jsonMgmt;
	private final int defaultStatus;
	//private final ApplicationManager appMan;
	
	public KPIStatisticsJSONFileManagement(JsonOGEMAFileManagement<?, ?> jsonMgmt) {
		this(jsonMgmt, 10);
	}
	public KPIStatisticsJSONFileManagement(JsonOGEMAFileManagement<?, ?> jsonMgmt, int defaultStatus) {
		this.jsonMgmt = jsonMgmt;
		this.defaultStatus = defaultStatus;
		//this.appMan = appMan;
	}

	/** Save or update kpiResult
	 * 
	 * @param kpiResult
	 * @return file path saved
	 */
	public String saveJSONMultiKPIEvalResult(JSONMultiKPIEvalResult kpiResult, Integer status) {
		//String baseFileName = ResourceUtils.getValidResourceName(kpiResult.multiConfigLocation);
		//String destPath = JsonOGEMAFileManagementImpl.getJSONFilePath(jsonMgmt.getFilePath(null, status!=null?status:defaultStatus, null), baseFileName, true);
		String destPath = getFileName(kpiResult.evalProviderId, status);
		System.out.println("Saving KPI result to "+destPath);
		MultiEvaluationUtils.exportToJSONFile(destPath, kpiResult);
		if(!data.containsKey(kpiResult.evalProviderId)) data.put(kpiResult.evalProviderId, kpiResult);
		return destPath;
	}
	
	public void saveJSONMultiKPIEvalResult(MultiKPIEvalConfiguration evalConfig, int status) {
		JSONMultiKPIEvalResult kpiResult = getResult(evalConfig);
		saveJSONMultiKPIEvalResult(kpiResult, status);
	}

	public JSONMultiKPIEvalResult getResult(MultiKPIEvalConfiguration config) {
		String loc = config.evaluationProviderId().getValue(); //config.getLocation();
		return data.get(loc);
	}
	
	public JSONMultiKPIEvalResult loadResult(MultiKPIEvalConfiguration config) {
		return loadResult(config, null);
	}
	public JSONMultiKPIEvalResult loadResult(MultiKPIEvalConfiguration config, Integer status) {
		String loc = config.evaluationProviderId().getValue(); //config.getLocation();
		String destPath = getFileName(loc, status);
		Path file = Paths.get(destPath);
		if(!file.toFile().exists()) {
			System.out.println("KPI file not found:"+destPath);
			return null;
		}
		System.out.println("Loading KPI result from "+destPath);
		JSONMultiKPIEvalResult kpiResult = MultiEvaluationUtils.importFromJSON(destPath, JSONMultiKPIEvalResult.class);
		if(kpiResult.evalProviderId == null) {
			//MultiKPIEvalConfiguration config = appMan.getResourceAccess().getResource(kpiResult.multiConfigLocation);
			kpiResult.evalProviderId = config.evaluationProviderId().getValue();
		}
		data.put(kpiResult.evalProviderId, kpiResult);
		return data.get(loc);
	}
	public JSONMultiKPIEvalResult getOrLoadResult(MultiKPIEvalConfiguration config) {
		JSONMultiKPIEvalResult kpiResult = getResult(config);
		if(kpiResult != null) return kpiResult;
		return loadResult(config);
	}
	public JSONMultiKPIEvalResult getOrLoadOrCreateResult(MultiKPIEvalConfiguration config) {
		JSONMultiKPIEvalResult kpiResult = getResult(config);
		if(kpiResult != null) return kpiResult;
		kpiResult = loadResult(config);
		if(kpiResult != null) return kpiResult;
		kpiResult = new JSONMultiKPIEvalResult();
		//kpiResult.multiConfigLocation2 = config.getLocation();
		kpiResult.evalProviderId = config.evaluationProviderId().getValue();
		data.put(kpiResult.evalProviderId, kpiResult);
		return kpiResult;
	}
	
	public String getFileName(String evalProviderId, Integer status) {
		String baseFileName = ResourceUtils.getValidResourceName(evalProviderId)+"_KPI.json";
		String destPath = JsonOGEMAFileManagementImpl.getJSONFilePath(jsonMgmt.getFilePath(null, status!=null?status:defaultStatus, null), baseFileName, true);
		return destPath;
	}
	public Collection<String> getGateways(MultiKPIEvalConfiguration config) {
		JSONMultiKPIEvalResult jmul = getOrLoadResult(config);
		if(jmul == null || jmul.individualResultKPIs == null) return Collections.emptyList();
		
		return new ArrayList<String>(jmul.individualResultKPIs.keySet());
	}
}
