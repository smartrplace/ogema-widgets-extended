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
package de.iwes.timeseries.eval.api.semaextension.variant;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.garo.api.base.GaRoEvalProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.CSVArchiveExporter;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting.PreEvaluationRequested;
import de.iwes.timeseries.eval.garo.multibase.GenericGaRoMultiProvider;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GaRoTestStarter;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GatewayBackupAnalysisAccess;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.ResultHandler;

public class GaRoEvalHelperJAXB_V2 {
	public static final String TEST = "Test";
	
    public static <P extends GaRoSingleEvalProvider> GaRoTestStarter<GaRoMultiResult> performGenericMultiEvalOverAllData(Class<P> singleEvalProvider,
				GatewayBackupAnalysisAccess gatewayParser, long startTime,
				long endTime, ChronoUnit resultStepSize, CSVArchiveExporter doExportCSV, boolean doBasicEval) {
    	return performGenericMultiEvalOverAllData(singleEvalProvider, gatewayParser, startTime, endTime,
    			resultStepSize, doExportCSV, doBasicEval, null);
    }
	public static <P extends GaRoSingleEvalProvider> GaRoTestStarter<GaRoMultiResult> performGenericMultiEvalOverAllData(Class<P> singleEvalProvider,
			GatewayBackupAnalysisAccess gatewayParser, long startTime,
			long endTime, ChronoUnit resultStepSize, CSVArchiveExporter doExportCSV, boolean doBasicEval,
			GaRoPreEvaluationProvider[] preEvalProviders) {
		return performGenericMultiEvalOverAllData(singleEvalProvider, gatewayParser,
				startTime, endTime, resultStepSize, doExportCSV, doBasicEval,
				preEvalProviders, null);
	}
	public static <P extends GaRoSingleEvalProvider> GaRoTestStarter<GaRoMultiResult> performGenericMultiEvalOverAllData(Class<P> singleEvalProvider,
				GatewayBackupAnalysisAccess gatewayParser, long startTime,
				long endTime, ChronoUnit resultStepSize, CSVArchiveExporter doExportCSV, boolean doBasicEval,
				GaRoPreEvaluationProvider[] preEvalProviders, List<ResultType> resultsRequested) {
    	return performGenericMultiEvalOverAllData(singleEvalProvider, gatewayParser, startTime, endTime, resultStepSize, doExportCSV, doBasicEval, preEvalProviders, resultsRequested, null);
    }
	public static <P extends GaRoSingleEvalProvider> GaRoTestStarter<GaRoMultiResult> performGenericMultiEvalOverAllData(Class<P> singleEvalProvider,
				GatewayBackupAnalysisAccess gatewayParser, long startTime,
				long endTime, ChronoUnit resultStepSize, CSVArchiveExporter doExportCSV, boolean doBasicEval,
				GaRoPreEvaluationProvider[] preEvalProviders, List<ResultType> resultsRequested, List<String> gwIds) {
    	return performGenericMultiEvalOverAllData(singleEvalProvider, gatewayParser, startTime, endTime,
    			resultStepSize, doExportCSV, doBasicEval, preEvalProviders, resultsRequested, gwIds, null, null);
    }
    /** Perform multi-evaluation on JAXB resources for a GaRoSingleEvalProvider class
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
     * @param resultFileName should usually end on ".json". Only relevant if not resultHandler is provided
     * @param resultHandler may be null
     * @return
     */
	public static <P extends GaRoSingleEvalProvider> GaRoTestStarter<GaRoMultiResult> performGenericMultiEvalOverAllData(Class<P> singleEvalProvider,
				GatewayBackupAnalysisAccess gatewayParser, long startTime,
				long endTime, ChronoUnit resultStepSize, CSVArchiveExporter doExportCSV, boolean doBasicEval,
				GaRoPreEvaluationProvider[] preEvalProviders, List<ResultType> resultsRequested, List<String> gwIds,
				String resultFileName, ResultHandler<GaRoMultiResult> resultHandler) {
		try {
			P singleProvider = singleEvalProvider.newInstance();
			if(singleProvider instanceof GaRoSingleEvalProviderPreEvalRequesting) {
				GaRoSingleEvalProviderPreEvalRequesting preEval = (GaRoSingleEvalProviderPreEvalRequesting)singleProvider;
				int i= 0;
				if(preEval.preEvaluationsRequested() != null) for(PreEvaluationRequested req: preEval.preEvaluationsRequested()) {
					preEval.preEvaluationProviderAvailable(i, req.getSourceProvider(), preEvalProviders[i]);
					i++;
				}
			}
			GenericGaRoMultiProvider<P> multiProvider2 = new GenericGaRoMultiProvider<P>(singleProvider, doBasicEval); //gatewayParser.getMultiEvalProvider(singleProvider, doBasicEval);
			GaRoTestStarter<GaRoMultiResult> result = new GaRoTestStarter<GaRoMultiResult>((
					multiProvider2 ),
				startTime, endTime, resultStepSize,
				resultFileName!=null?resultFileName:singleEvalProvider.getSimpleName()+"Result.json", doExportCSV, resultsRequested, gwIds,
						resultHandler, null);
			Executors.newSingleThreadExecutor().submit(result);
			return result;
		} catch(InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
    }
}
