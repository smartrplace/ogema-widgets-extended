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
package org.ogema.util.jsonresult.management.api;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.ogema.model.jsonresult.JSONResultFileData;
import org.ogema.model.jsonresult.JsonOGEMAFileData;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.util.evalcontrol.EvalScheduler;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.api.extended.util.AbstractSuperMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSuperEvalResult;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GaRoEvalHelperGeneric;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GaRoTestStarter;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.ResultHandler;

/** This API defines a management for storing Java Objects and especially object of type AbstractSuperMultiResult
 * as JSON files. File descriptor information including the class information to read back the JSON file
 * is stored in OGEMA resources of type JSONResultFileData.<br>
 * The API also defines how to trigger evaluation runs that store their result as a JSON file.
 *
 */
public interface EvalResultManagement extends JsonOGEMAFileManagement<AbstractSuperMultiResult<?>, JSONResultFileData> {
	<M extends AbstractSuperMultiResult<?>, N extends M> JSONResultFileData saveResult(M result,
			Class<N> typeToUse, int status,
			String baseFileName, boolean overwriteIfExisting, String providerId,
			List<JsonOGEMAFileData> preEvaluationsUsed);
	
	 /** Perform multi-evaluation on JAXB resources for a GaRoSingleEvalProvider class and save the result
     * 
     * @param singleEvalProvider evaluation provider class to use. Note that this method is
     * 		usually not applicable for providers that require initialization. In this case reflective construction will fail.
     * @param gatewayParser object providing source data
     * @param startTime
     * @param endTime
     * @param resultStepSize
     * @param doExportCSV if not null the result will be exported as zipped csv file
     * @param doBasicEval if true the basic evaluation provider will be executed for quality checks (not recommended anymore)
     * @param preEvalProviders if singleEvalProvider requests pre evaluation data provide the respective providers here
     * @param resultsRequested if null all results offered by the provider will be calculated
     * @param gwIds gateways to be evaluated. If null no filtering of input will be applied.
     * @param result file name, should usually end on ".json". If null it will be generated automatically.
     * @param exportDescriptor if true an ogj file with the descriptor resource information will be
     *  	saved with the JSON result file for later import after a clean start or when file
     *  	shall be transferred to a different system
     *  @param dataProvidersToUse if null default data provider shall be used
     * @return
     */
	<P extends GaRoSingleEvalProvider> GaRoTestStarter<GaRoMultiResult> performGenericMultiEvalOverAllData(Class<P> singleEvalProvider,
			long startTime,
			long endTime, ChronoUnit resultStepSize,
			GaRoPreEvaluationProvider[] preEvalProviders, List<ResultType> resultsRequested, List<String> gwIds,
			String resultFileName, String providerId,
			Class<? extends GaRoSuperEvalResult<?>> superResultClassForDeserialization,
			boolean exportDescriptor,
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse);
	
	/** See {@link #performGenericMultiEvalOverAllData(Class, GatewayBackupAnalysis, long, long, ChronoUnit, GaRoPreEvaluationProvider[], List, List, String, String, Class)}
	 * @param externalResultHandler if not null the external result handler will be notified when the evaluation finished
	 * @param writeJSON if false no JSON result is written. Note that the latter usually only makes sense when an
	 * 		external ResultHandler uses the result
	 * @param additionalConfigurations see {@link GaRoEvalHelperGeneric#performGenericMultiEvalOverAllData(Class, GatewayBackupAnalysis, long, long, ChronoUnit, de.iwes.timeseries.eval.garo.helper.jaxb.GaRoEvalHelperGeneric.CSVArchiveExporter, boolean, GaRoPreEvaluationProvider[], List, List, String, ResultHandler, List, java.util.Collection)}
	 * @return
	 */
	public <P extends GaRoSingleEvalProvider> GaRoTestStarter<GaRoMultiResult> performGenericMultiEvalOverAllData(Class<P> singleEvalProvider,
			long startTime,
			long endTime, ChronoUnit resultStepSize,
			GaRoPreEvaluationProvider[] preEvalProviders, List<ResultType> resultsRequested, List<String> gwIds,
			String resultFileName, String providerId,
			Class<? extends GaRoSuperEvalResult<?>> superResultClassForDeserialization,
			ResultHandler<GaRoMultiResult> externalResultHandler, boolean writeJSON,
			boolean exportDescriptor,
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse,
			List<ConfigurationInstance> additionalConfigurations);
	
	/** See {@link #performGenericMultiEvalOverAllData(Class, GatewayBackupAnalysis, long, long, ChronoUnit, GaRoPreEvaluationProvider[], List, List, String, String, Class, ResultHandler, boolean, boolean, List, List)}.
	 * This is currently the most generic call, so this might be used for further developments in the future
	 * @param performBlocking if true the evaluation is not started in a new Thread, but in the current Thread. This should only be
	 * 		called from a specific evaluation thread, e.g. from a queuing thread.
	 */
	public <P extends GaRoSingleEvalProvider> GaRoTestStarter<GaRoMultiResult> performGenericMultiEvalOverAllDataBlocking(Class<P> singleEvalProvider,
			long startTime,
			long endTime, ChronoUnit resultStepSize,
			GaRoPreEvaluationProvider[] preEvalProviders, List<ResultType> resultsRequested, List<String> gwIds,
			String resultFileName, String providerId,
			Class<? extends GaRoSuperEvalResult<?>> superResultClassForDeserialization,
			ResultHandler<GaRoMultiResult> externalResultHandler, boolean writeJSON,
			boolean exportDescriptor,
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse,
			List<ConfigurationInstance> additionalConfigurations,
			boolean performBlocking);
	
	/** Get all file descriptor resources that are indicated to be generated by certain provider in a certain interval
	 * @param startTime start of interval
	 * @param endTime end of interval
	 * @param includeOverlap if true all files that contribute to the interval at least partially are returned, 
	 * 		otherwise only files that are completely inside the interval*/
	List<JSONResultFileData> getDataOfProvider(String providerId, long startTime, long endTime, boolean includeOverlap);
	List<JSONResultFileData> getDataOfConfig(MultiKPIEvalConfiguration startConfig, long startTime, long endTime,
			boolean includeOverlap);
	
	/** Get a super result containing all evaluation intervals of a certain interval even from different files.
	 * See {@link #getDataOfProvider(String, long, long, boolean)} for details. If the result intervals overlap
	 * always the newst result shall be used.
	 */
	GaRoSuperEvalResult<?> getAggregatedResult(String providerId, long startTime, long endTime, boolean includeOverlap);
	GaRoSuperEvalResult<?> getAggregatedResult(List<JSONResultFileData> descs,
			long startTime, long endTime, boolean includeOverlap);
	
	/**If not supported by the implementation return null*/
	public EvalScheduler getEvalScheduler();
}
