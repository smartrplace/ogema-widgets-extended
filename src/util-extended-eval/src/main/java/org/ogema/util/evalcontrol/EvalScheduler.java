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
package org.ogema.util.evalcontrol;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.ogema.core.model.Resource;
import org.ogema.model.jsonresult.MultiEvalStartConfiguration;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;

import de.iwes.timeseries.eval.api.extended.MultiResult;
import de.iwes.timeseries.eval.api.extended.util.AbstractSuperMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.ResultHandler;

/** This API is based on the  {@link EvalResultManagement} API. It does not extend it, but
 * an implementation will most likely use/require an implementation of EvalResultManagement.
 * This API defines a management for evaluation configurations that can be queued once or
 * for which can be auto-queuing defined, which will cause evaluation runs to be performed on
 * a regular base, e.g. once per day or once per hour.<br>
 * The basic version supports only a single entry per evaluation provider*/
public interface EvalScheduler {
	
	/** Register provider instance that usually was registered as OSGi service. Auto-evaluation is only activated
	 * here if {@link MultiKPIEvalConfiguration#a}*/
	void registerEvaluationProvider(GaRoSingleEvalProvider evalProvider);
	void unregisterEvaluationProvider(GaRoSingleEvalProvider evalProvider);
	GaRoSingleEvalProvider getProvider(String evalProviderId);
	
	/** Register provider for standard KPI auto-calculation with a standard step interval type.
	 * This information is stored persistently, so this
	 * call shall only be made when the configuration shall be created or updated.
	 * The standard KPI calculation interval step size of the instance is
	 * determined in the constructor as it is assumed to be the same value for all KPI calculations.
	 * @param subConfigId see {@link #getConfig(String, String)}
	 * @param baseIntervalType if null the standard base interval will be used. This is required for
	 * 		auto-scheduling. If the baseInterval is specified no auto-scheduling will be configured,
	 * 		but the configuration is just created
	 * @param additionalIntervalTypes the standard interval type will always be evaluated, but additional
	 * 		interval types that should fit the requirements set in {@link MultiKPIEvalConfiguration#kpisToCalculate()}
	 * 		can be given here, may be null if only standard interval shall be configured
	 * @param queueOnStartup if true a first evaluation will be queued immediately and always
	 * 		on startup in addition to the regular timer events. If the standard start/end time
	 * 		calculation does not give any new intervals to be calculated no evaluation
	 * 		will be queued (see {@link #queueEvalConfig(String, Boolean, ResultHandler)})
	 * @return MultiKPIEvalConfiguration created*/
	MultiKPIEvalConfiguration registerProviderForKPI(GaRoSingleEvalProvider eval,
			String subConfigId, boolean recursive, int[] additionalIntervalTypes, boolean queueOnStartup, String[] gwIds);
	MultiKPIEvalConfiguration unregisterProviderForKPI(GaRoSingleEvalProvider eval, String subConfigId, boolean recursive);
	
	/** Get configuration if existing
	 * 
	 * @param evalProviderId
	 * @param subConfigId see {@link MultiKPIEvalConfiguration#subConfigId()}. If null or empty String
	 * 		any configuration for the respective EvaluationProvider is returned
	 * @return matching configuration or null if no such configuration exists
	 */
	MultiKPIEvalConfiguration getConfig(String evalProviderId, String subConfigId);
	
	/** Get all configurations matching the input
	 *  
	 * @param evalProviderId if null all configurations known are returned.
	 * @return
	 */
	List<MultiKPIEvalConfiguration> getConfigs(String evalProviderId);
	
	/** In this version the result resource will not be activated if created
	 * @param subConfigId see {@link #getConfig(String, String)}*/
	MultiKPIEvalConfiguration getOrCreateConfig(String evalProviderId, String subConfigId);
	
	MultiKPIEvalConfiguration getOrCreateConfig(String evalProviderId, String subConfigId, Integer stepInterval, boolean forceValues);
	
	/**
	 * 
	 * @param evalPrvoviderId
	 * @param subConfigId see {@link #getConfig(String, String)}
	 * @param stepInterval value to set if step interval is not set. If null the standard step interval
	 * 		is used.
	 * @param forceValues parmeter values will be set to the given values even if already present.
	 * 		parameters that are null are ignored
	 * @param additionalIntervalTypes see {@link #registerProviderForKPI(GaRoSingleEvalProvider, String, boolean, int[], boolean, String[])}
	 * 		default is null, which means that only the stepInterval (= base interval) shall be evaluated 
	 * @return
	 */	
	MultiKPIEvalConfiguration getOrCreateConfig(String evalProviderId, String subConfigId, Integer stepInterval,
			int[] additionalIntervalTypes, boolean forceValues);
	
	MultiKPIEvalConfiguration removeConfig(String evalProviderId, String subConfigId);
	
	/** Get configuration resource for evalConfig. The configuration resource may be virtual, but has the correct
	 * type for the evaluationProvider.
	 * 
	 * @param evalConfig
	 * @param eval evaluationProvider configured by evalConfig
	 * @return null if the configuration resource or the type cannot be determined.
	 */
	<T extends Resource> T getConfigurationResource(MultiKPIEvalConfiguration evalConfig, GaRoSingleEvalProvider eval);
	
	public interface MultiEvalRunI {
		MultiKPIEvalConfiguration getConfig();
		Boolean getSaveJsonResult();
		ResultHandler<?> getListener();
		/** Start time of evaluation interval*/
		long getStartTime();
		/** End time of evaluation interval*/
		long getEndTime();
		/** Time when evaluation calculated was started. Shall be zero or negative
		 * when calculation has not been started yet.
		 */
		long startTimeOfCalculation();
		/** Actual duration of evaluation calculation in ms. Shall be negative if
		 * evaluation is not yet finished.
		 */
		long calculationDuration();
		List<GaRoMultiEvalDataProvider<?>> getDataProvidersToUse();
	}
	
	/** Get elements currently waiting in the queue. This does not include the element currently executed.
	 * Returns null if no execution ongoing.*/
	
	List<MultiEvalRunI> getQueueElements();
	/** Return null if no execution ongoing*/
	MultiEvalRunI getElementCurrentlyExecuted();
	
	/** see {@link MultiKPIEvalConfiguration#autoQueueInterval()}. The method just
	 * changes the activation status of the resource
	 * @param subConfigId see {@link #getConfig(String, String)}
	 */
	MultiKPIEvalConfiguration activateAutoEvaluation(String evalProviderId, String subConfigId);
	/** see {@link MultiKPIEvalConfiguration#autoQueueInterval()}. The method just
	 * changes the activation status of the resource
	 * @param subConfigId see {@link #getConfig(String, String)}
	 */
	MultiKPIEvalConfiguration deactivateAutoEvaluation(String evalProviderId, String subConfigId);
	
	/** Perform evaluation on configuration like auto-queuing would do at this moment*/
	public void queueAutoEvalConfig(MultiKPIEvalConfiguration config);
	
	/** Queue evaluation that will be performed when all other evaluations before in the
	 * queue are evaluated. At finishing of the evaluation saving to JSON, calculation and
	 * resource storage of KPIs and listener evaluation will be done according to the 
	 * specification of the MultiKPIEvalConfiguration. In this basic version no pre-evaluations
	 * are queued.<br>
	 * @param evalProvider
	 * @param subConfigId see {@link #getConfig(String, String)}
	 * @param saveJsonResult if not null overwrites the setting in the {@link MultiEvalStartConfiguration}.
	 * 		Usually for ongoing intervals no JSON result should be written. Ongoing intervals may
	 * 		be calculated on the fly
	 * @param listener my be null if no listener required
	 * @param defaultIntervalsToCalculate see {@link #getStandardStartEndTime(MultiKPIEvalConfiguration, int)}
	 * @return [0]: start time, [1]: end time, null if evaluation failed
	 */
	long[] queueEvalConfig(String evalProvider, String subConfigId, Boolean saveJsonResult,
			ResultHandler<?> listener, int defaultIntervalsToCalculate);
	
	public enum OverwriteMode {
		NO_OVERWRITE,
		ONLY_PROVIDER_REQUESTED,
		ALL_INCLUDING_PRE_EVALUATIONS
	}
	/** See {@link #queueEvalConfig(String, String, Boolean, ResultHandler, int)}. This version supports
	 * checking and auto-calculation of required Pre-Evaluations.<br>
	 * This version requires that #setStandardDataProvidersToUse(List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse);
	 * is called before this method is called.
	 * 
	 * @param queuePreEvals if true the Pre-evaluation provider will be checked whether results are
	 * 		available even if {@link MultiEvalStartConfiguration#preEvaluationSchedulingMode()} is not
	 * 		set. If the value is active and set to zero then pre-evaluation is blocked completely
	 * 		Missing results will be queued before running the actual evaluation requested.<br>
	 * 		Note that each evaluation provider will only be queued once by one call of queueEvalConfig even
	 * 		if it is requested as pre-evaluation from different levels of the pre-evalution dependency tree.
	 * @param overwriteMode if overwrite is activated no check will be performed if the results requested
	 * 		are already available. Otherwise the evaluation will only be performed for missing KPI
	 * 		intervals.<br>
	 * 		Note that currently only the stdStepSize KPIs are evaluated, which is assumed to be the base
	 * 		interval type of all evaluations. This can currently only be set in the constructor of
	 * 		the implementation.
	 * @param overWritePreEvalSettings if an existing MultiKPIEvalConfiguration is found for a required
	 * 		pre-evaluation provider with the same subConfigId this is relevant. If true the existing resource
	 * 		will be set to the required input values. If false and the existing resource has at least one
	 * 		parameter not fitting the requirements a new setting will be created. In the true-mode the
	 * 		creation of a lot of configurations that might not be used anymore is avoided, but if two
	 * 		evaluations with the same subConfigIf with different gateway or timestep settings need
	 * 		pre-evaluation data from the same provider this mechanism may mess up the caculation.
	 * @return
	 */
	long[] queueEvalConfig(MultiKPIEvalConfiguration startConfig, boolean saveJsonResult, ResultHandler<?> listener, int defaultIntervalsToCalculate,
			boolean queuePreEvals, OverwriteMode overwriteMode, boolean overWritePreEvalSettings);
	/**
	 * 
	 * @param startTime use this start time instead of the default time interval
	 * @param endTime use this end time instead of the default time interval
	 * @param dataProvidersToUse use these data providers instead of data providers selected via
	 * 		{@link #setStandardDataProvidersToUse(List)}
	 * @return
	 */
	long[] queueEvalConfig(MultiKPIEvalConfiguration startConfig, boolean saveJsonResult, ResultHandler<?> listener,
			long startTime, long endTime, List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse,
			boolean queuePreEvals, OverwriteMode overwriteMode, boolean overWritePreEvalSettings);
	
	/**
	 * 
	 * @param runSingleGw if not null: perform evaluation for a single sub-gateway, which is only relevant
	 * 		if {@link MultiKPIEvalConfiguration#queueGatewaysIndividually()} is true. The result
	 * 		will be saved in the sub-KPI structure.
	 * @param evalProvidersQueued Each evaluation provider is only queued once by one call of queueEvalConfig even
	 * 		if it is requested as pre-evaluation from different levels of the pre-evalution dependency tree
	 * 		(this is a general behaviour, see {@link #queueEvalConfig(MultiKPIEvalConfiguration, boolean, ResultHandler, int, boolean, OverwriteMode, boolean)}.
	 * 		If this check shall be extended to several consecutive calls to queueEvalConfig, give the same Set<String>
	 * 		to all calls here and the list will be updated over all calls.<br>
	 * 		If null the default behaviour is applied.
	 * @param resultFileName may be null even if saveJsonResult is true. If null the default file name is generated.
	 * 		The system makes sure that no existing file is overwritten (as long as there are less than a hundred files
	 * 		that have been generated with the same resultFilename or default for the same evaluation provider).
	 * 		For automated pre-evaluation scheduling the default result will be used.
	 * @return
	 */
	long[] queueEvalConfig(MultiKPIEvalConfiguration startConfig, boolean saveJsonResult,
			String resultFileName, ResultHandler<?> listener,
			long startTime, long endTime, List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse,
			boolean queuePreEvals, OverwriteMode overwriteMode, boolean overWritePreEvalSettings,
			String runSingleGw, Set<String> evalProvidersQueued);
	
	/** Works like {@link #queueEvalConfig(MultiKPIEvalConfiguration, boolean, ResultHandler, long, long, List, boolean, OverwriteMode, boolean, String, Set)},
	 * but does not take into account any pre-evaluation. So this can be used to start an evaluation without
	 * taking case whether pre-evaluation is available or not.
	 * @return
	 */
	public long[] queueEvalConfigWithoutPreEval(MultiKPIEvalConfiguration startConfig, boolean saveJsonResult,
			String resultFileName,
			ResultHandler<?> listener, long startTime, long endTime,
			List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse, String runSingleGw);
	
	/** The automated determination of startTime and endTime works as follows: If a result for
	 * the evalProvider is found (as a JSON result descriptor) all fully available time steps
	 * before the current time are calculated. If no existing results are found the
	 * last full time intervals (number: defaultIntervalsToCalculate) before now are calculated.<br>
	 * Note that the evaluation will not be queued if startTime >= endTime. So this method can
	 * also be used to check whether a new evaluation can be calculated.
	 * 
	 * @param startConfig
	 * @param defaultIntervalsToCalculate see description above.
	 * @param forceRecalculation if true and under the condition described above no evaluation would
	 * 		take place the start end time is returned as if no results would be available
	 * @return [0]: start time, [1]: end time, null if no evaluation necessary
	 */
	long[] getStandardStartEndTime(MultiKPIEvalConfiguration startConfig, int defaultIntervalsToCalculate,
			boolean forceRecalculation);
	
	/** Re-activate auto evaluation after it has been suspended. Note that scheduling configured
	 * auto-evaluations if the default mode of the EvalScheduler.
	 * @return success status
	 * */
	boolean activateAutoEvaluation();

	/** If Auto-evaluation is deactivated no more auto-evaluations will take place until
	 * it is re-activated. Note that any evaluations already scheduled are not affected.
	 * @return success status
	 * */
	boolean deactivateAutoEvaluation();
	
	/** Get overall auto-evaluation status*/
	boolean isAutoEvaluationActive();
	
	void registerAutoEvalResultListener(ResultHandler<?> listener);
	
	/**Configure instances of KPIStatisticsManagement
	 * @param providerId all ResultType-KPIs configured in all MultiKPIEvalConfigurations for the provider will
	 * be configured
	 * @return management objects for each ResultType-KPI*/
	List<KPIStatisticsManagementI> configureKPIManagement(String providerId);
	
	/** See {@link #configureKPIManagement(String)}. Only the KPIs for a single MultiKPIEvalConfiguration
	 * are configured
	 */
	List<KPIStatisticsManagementI> configureKPIManagement(MultiKPIEvalConfiguration config, GaRoSingleEvalProvider eval);
	/** This version is only relevant if {@link MultiKPIEvalConfiguration#queueGatewaysIndividually()}
	 * 	is true. Note that sub-gateway KPIStatisticsManagement resources are only created if
	 * {@link MultiKPIEvalConfiguration#queueGatewaysIndividually() is true even if the following specification
	 * says that it should be created.
	 * @param subGateway id of individual gateway, if null the overall evaluation is used. If the subGateway is set
	 * 		only the KPIStatisticsManagement for the subGateway is created. Otherwise the overall KPIStatisticsManagement
	 * 		and resources for the subGateways in the evaluation shall be created.
	 * @param forceResultIds if false a cashed List of type {@link KPIStatisticsManagementI} may
	 * 		be used. If a list is configured all given resultIds will be configured even if no such
	 * 		data is stored yet. This is required when new data is written and potentially new resultIds
	 * 		shall be added.
	 */
	List<KPIStatisticsManagementI> configureKPIManagement(MultiKPIEvalConfiguration config, GaRoSingleEvalProvider eval,
			String subGateway, Collection<String> forceResultIds);
	
	Collection<String> getIndividualGatewayList(MultiKPIEvalConfiguration config);
	
	/**Configures instances of KPIStatisticsManagement and also uses existing JSON files for first
	 * calculation or update of the values. If {@link MultiKPIEvalConfiguration#queueGatewaysIndividually()} is true
	 * then also subGW KPIs are calculated (see parameter gwIds for details).
	 * @param evalConfig
	 * @param provider
	 * @param result data structure read obtained from JSON file or from evaluation
	 * @param baseInterval baseInterval to be used
	 * @param runSingleGw see {@link #queueEvalConfig(MultiKPIEvalConfiguration, boolean, ResultHandler, long, long, List, boolean, OverwriteMode, boolean, String, Set)}
	 * @param gwIds may be null. Note that if this null and evalConfig contains no gatewayIds then sub-KPIs
	 * 		are not calculated even if {@link MultiKPIEvalConfiguration#queueGatewaysIndividually()} is true
	 * @return
	 */
	List<KPIStatisticsManagementI> calculateKPIs(MultiKPIEvalConfiguration evalConfig,
			GaRoSingleEvalProvider provider,
			AbstractSuperMultiResult<MultiResult> result,
			int baseInterval,
			String runSingleGw, String[] gwIds);
	
	/**If no data providers are given with an evaluation queued or started via auto-evaluation
	 * the set of data providers given here is used. If
	 * {@link #queueEvalConfig(MultiKPIEvalConfiguration, boolean, ResultHandler, int, boolean, OverwriteMode, boolean)}
	 * is used then a standard DataProvider has to be set before the call otherwise you will get a NullPointerException.
	 */
	public void setStandardDataProvidersToUse(List<GaRoMultiEvalDataProvider<?>> dataProvidersToUse);
	
	/** Determine whether the resource describing a JSON result file shall be exported as ogj file
	 * so that the
	 * JSON result file can be used also on other systems or after clean start. Default behaviour shall
	 * be false if not specified by implementation otherwise.
	 * @param exportDescriptor if true the export is activated
	 */
	public void setDescriptorExportMode(boolean exportDescriptor);
}
