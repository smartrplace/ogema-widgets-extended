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
package org.ogema.util.jsonresult.management;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.ogema.model.jsonresult.JSONResultFileData;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance.GenericObjectConfiguration;
import de.iwes.timeseries.eval.api.extended.util.AbstractSuperMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoStdPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSuperEvalResult;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GaRoEvalHelperGeneric;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GaRoTestStarter;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.ResultHandler;

public class EvalHandlerStd<P extends GaRoSingleEvalProvider> {
	public final GaRoTestStarter<GaRoMultiResult> starter;
	public EvalHandlerStd(Class<P> singleEvalProvider,
			long startTime,
			long endTime, ChronoUnit resultStepSize,
			GaRoPreEvaluationProvider[] preEvalProviders, List<ResultType> resultsRequested, List<String> gwIds,
			String resultFileName, String providerId,
			Class<? extends GaRoSuperEvalResult<?>> superResultClassForDeserialization,
			EvalResultManagement evalResultMan,
			ResultHandler<GaRoMultiResult> externalResultHandler, boolean writeJSON,
			boolean exportDescriptor,
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse,
			List<ConfigurationInstance> additionalConfigurations,
			boolean performBlocking) {
		ResultHandler<GaRoMultiResult> resH = new ResultHandler<GaRoMultiResult>() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void resultAvailable(AbstractSuperMultiResult<GaRoMultiResult> result, String jsonFileNameSpecifiedInCall) {
				if(writeJSON) {
					List<String> preEvalFilesWithoutInfo = new ArrayList<>();
					List<JSONResultFileData> preEvaluationsUsed = new ArrayList<>();
					if(preEvalProviders != null) {
						for(GaRoPreEvaluationProvider pre: preEvalProviders) {
							if(pre instanceof GaRoStdPreEvaluationProvider) {
								String preFileName = ((GaRoStdPreEvaluationProvider)pre).jsonInputFile;
								JSONResultFileData preInfo = evalResultMan.getFileInfo(preFileName);
								if(preInfo != null) preEvaluationsUsed.add(preInfo);
								else preEvalFilesWithoutInfo.add(preFileName);
							} else {
								throw new UnsupportedOperationException("Currently we have to get pre-file into from STD-PreEvalProvider!");
							}
						}
					}
					String baseName;
					if(resultFileName == null) baseName = singleEvalProvider.getSimpleName();
					else baseName = resultFileName;
					
					Class<? extends GaRoSuperEvalResult<?>> typeToUse = null;
					if(superResultClassForDeserialization != null)
						typeToUse = superResultClassForDeserialization;
					
					//Avoid trying to write objects with unknown structure to JSON
					List<ConfigurationInstance> toRemove = new ArrayList<>();
					for(ConfigurationInstance c: result.configurations) {
						if(c instanceof GenericObjectConfiguration) toRemove.add(c);
					}
					result.configurations.removeAll(toRemove);
					
					JSONResultFileData info = evalResultMan.saveResult(result, typeToUse, 10, baseName, false, providerId, (List)preEvaluationsUsed);
					if(!preEvalFilesWithoutInfo.isEmpty()) {
						info.preEvaluationFilesUsed().create();
						info.preEvaluationFilesUsed().setValues(preEvalFilesWithoutInfo.toArray(new String[0]));
					}
					if(exportDescriptor) evalResultMan.exportFileData(info, null, false);
				}
				if(externalResultHandler != null) externalResultHandler.resultAvailable(result, jsonFileNameSpecifiedInCall);
			}
		};
		starter = GaRoEvalHelperGeneric.performGenericMultiEvalOverAllData(singleEvalProvider,
				startTime, endTime, resultStepSize, null, false, preEvalProviders, resultsRequested,
				gwIds, resultFileName, resH, dataProvidersToUse, additionalConfigurations, performBlocking);
		
	}
}
