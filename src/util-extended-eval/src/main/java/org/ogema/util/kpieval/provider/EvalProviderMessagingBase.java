package org.ogema.util.kpieval.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.util.directresourcegui.kpi.KPIMonitoringReport;
import org.ogema.util.kpieval.KPIEvalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.timeseries.eval.api.EvaluationInput;
import de.iwes.timeseries.eval.api.EvaluationInstance.EvaluationListener;
import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoEvaluationCore;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoResultType;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoSingleEvalProviderPreEval;
import de.iwes.util.timer.AbsoluteTiming;

/**
 * Evaluate basic time series qualities per gateway including gap evaluation
 */
public abstract class EvalProviderMessagingBase extends GenericGaRoSingleEvalProviderPreEval {
	public static final int COLUMN_WIDTH = 15;
	public static final String DEFAULT_QUALITY_EVALPROVIDER_ID = "adefault-quality_eval_provider";
    	
	/** Default set of resultIds of the default quality evaluation provider used by
	 * 		{@link #sampleQualityDefaultDefinition(String[], String, boolean, String)} as default*/
	public final static String[] qualityResults = new String[]{"TS_TOTAL", "TS_WITH_DATA", "TS_GOOD", "TS_GOLD",
			"OVERALL_GAP_REL", "DURATION_HOURS", "timeOfCalculation"};
	/** Another default set of resultIds of the default quality evaluation provider*/
	public final static String[] qualitySensorResults = new String[]{"TS_TOTAL", "TS_WITH_DATA", "TS_GOOD", "TS_GOLD",
			"OVERALL_GAP_REL", "DURATION_HOURS", "$GAP_SENSORS", "timeOfCalculation"};

	protected static final Logger logger = LoggerFactory.getLogger(EvalProviderMessagingBase.class);

	/** Provide your data types here*/
	public static abstract class KPIPageDefinitionWithEmail {
		/** If you want to define a message generation for the KPI page you have to set 
		 *  kpiPageDefinition.messageProvider to a unique String value on the system. If no email message
		 *  shall be provided then set the messageProvider to null. In this case the other members are
		 *  not relevant.<br>
		 *  The following providerIds are supported by default:
		 *  <li>adefault-quality_eval_provider: Used by {@link EvalProviderMessagingBase#sampleQualityDefaultDefinition(String[], String, boolean, String)}</li> 
		 *  <li>basic-humidity_eval_provider<li>: Provider for the evaluation of room and inwall humidity,
		 *       requires humidity sensors in the relevant rooms. See PrimaryHumidityEvalProvider for details.
		 */
		public KPIPageDefinition kpiPageDefinition;
		/** Title of email message*/
		public abstract String getTitle();
		/** Message body of message*/
		public abstract String getMessage(Collection<KPIStatisticsManagementI> kpis, long currentTime);
		/** If true a separate evaluation message is generated. If false an email message
		 * ist still defined, but shall not be sent out automatically
		 */
		public boolean sendDailyMessage = true;
		
		public List<String> gatewayIdsToEvaluate;
	}
	
	/** Define pages with email transmission.
	 */
	protected abstract List<KPIPageDefinitionWithEmail> getPages();
	
	/** Register Email messaging providers*/
	protected abstract void addOrUpdatePageConfigFromProvider(KPIPageDefinitionWithEmail def, GaRoSingleEvalProvider eval);
	
	protected Map<String, KPIMessageDefinitionProvider> providers = new HashMap<>();
	protected List<KPIPageDefinition> pageDefs = new ArrayList<>();
	
    public EvalProviderMessagingBase(String id, String label, String description) {
		super(id, label, description);
		updatePageAndMessageProviders();
	}

    public void updatePageAndMessageProviders() {
    	pageDefs.clear();
    	providers.clear();
		for(KPIPageDefinitionWithEmail kpem: getPages()) {
			pageDefs.add(kpem.kpiPageDefinition);
			if(kpem.kpiPageDefinition.messageProvider == null)
				continue;
			KPIMessageDefinitionProvider prov = new KPIMessageDefinitionProvider() {

				@Override
				public MessageDefinition getMessage(Collection<KPIStatisticsManagementI> kpis, long currentTime) {
					return new MessageDefinition() {
						@Override
						public String getTitle() {
							return kpem.getTitle();
						}

						@Override
						public String getMessage() {
							return kpem.getMessage(kpis, currentTime);
						}
					};
				}
			};
			providers.put(kpem.kpiPageDefinition.messageProvider, prov);
			addOrUpdatePageConfigFromProvider(kpem, this);
		}    	
    }
    
    @Override
	public List<KPIPageDefinition> getPageDefinitionsOffered() {
		return pageDefs;
	}
	
	@Override
	public KPIMessageDefinitionProvider getMessageProvider(String messageProviderId) {
		return providers.get(messageProviderId);
	}
	
	public static String getMessageStdTable(Collection<KPIStatisticsManagementI> kpis, long currentTime,
			String[] resultIds) {
		String mes = "";
		boolean init = false;
		for(KPIStatisticsManagementI gw: kpis) {
			if(!init) {
				mes += getRightAlignedString("BoxID", COLUMN_WIDTH)+" | ";
				int idx = 0;
				for(KPIStatisticsManagementI kpi2: gw.ksmList()) {
					if(resultIds[idx].equals("timeOfCalculation"))
						mes += getRightAlignedString("timeOfCalculation", COLUMN_WIDTH)+" | ";
					else
						mes += getRightAlignedString(kpi2.resultTypeId(), COLUMN_WIDTH)+" | ";
					idx++;
				}
				mes += "\r\n";
				init = true;
			}
			boolean firstDone = false;
			mes += getRightAlignedString(gw.specialLindeId(), COLUMN_WIDTH);
			int idx = 0;
			for(KPIStatisticsManagementI kpi2: gw.ksmList()) {
				if(!firstDone)
					mes += " | ";
				else firstDone = true;
				//Schedule sched = kpi2.getIntervalSchedule(AbsoluteTiming.DAY);
				//SampledValue sv = sched.getPreviousValue(Long.MAX_VALUE);
				if(resultIds[idx].equals("timeOfCalculation")) {
					SampledValue sv = kpi2.getTimeOfCalculationNonAligned(AbsoluteTiming.DAY, currentTime, 1);
					if(sv == null) {
						mes += getRightAlignedString("NO VALUE", COLUMN_WIDTH);
					} else {
						long tcalc = sv.getValue().getLongValue();
						String text = TimeUtils.getDateAndTimeString(tcalc);
						mes += getRightAlignedString(text, COLUMN_WIDTH);
					}
				} else {
					if(kpi2.resultTypeId().startsWith("$")) {
						String text = kpi2.getStringValue(currentTime, 1);
						if(text == null)
							mes += getRightAlignedString("n/a", COLUMN_WIDTH);									
						else
							mes += getRightAlignedString(text, COLUMN_WIDTH);									
					} else {
						SampledValue sv = kpi2.getValueNonAligned(AbsoluteTiming.DAY, currentTime, 1);
						if(sv == null)
							mes += getRightAlignedString("NO VALUE", COLUMN_WIDTH);
						else {
							final GaRoDataTypeI dataType;
							GaRoSingleEvalProvider prov = kpi2.getEvalProvider();
							ResultType r = null;
							if(prov != null ) {
								r  = KPIMonitoringReport.getResultTypeById(prov, kpi2.resultTypeId());
							}
							if(r != null && r instanceof GaRoDataTypeI) dataType = (GaRoDataTypeI) r;
							else dataType = null;
							mes += getRightAlignedString(KPIMonitoringReport.formatValue(sv, dataType), COLUMN_WIDTH);
						}
					}
				}
				idx++;
			}
			ReadOnlyTimeSeries leadingCol = gw.getIntervalSchedule(AbsoluteTiming.DAY);
			mes += " ("+leadingCol.size()+")\r\n";
		}
		return mes;
		
	}
	
	public static String getRightAlignedString(String in, int len) {
		if(in.length() >= len) return in.substring(0, len);
		return StringUtils.repeat(' ', len-in.length())+in;
	}
	
	public static String getDevicePath(String tsId) {
		String[] subs = tsId.split("/");
		if(subs.length > 3) return subs[2];
		else return tsId; //throw new IllegalStateException("Not a valid tsId for Homematic:"+tsId);
	}
	
	/** Define page for quality evaluation and optionally also email generation
	 * 
	 * @param resultIds
	 * @param configName
	 * @param addEmailPage if false no email messaging will be defined
	 * @return
	 */
	public static KPIPageDefinition getQualityDefaultDefinition(String[] resultIds,
			String configName, boolean addEmailPage) {
		KPIPageDefinition def = new KPIPageDefinition();
		def.resultIds.add((resultIds!=null)?resultIds:qualityResults);
		def.providerId = Arrays.asList(new String[] {DEFAULT_QUALITY_EVALPROVIDER_ID});
		def.configName = configName; // "Multi-GW Project Quality Report";
		def.urlAlias = WidgetHelper.getValidWidgetId(configName);
		def.specialIntervalsPerColumn.put("DURATION_HOURS", 1);
		def.specialIntervalsPerColumn.put("timeOfCalculation", 1);
		def.specialIntervalsPerColumn.put("TS_GOLD", 1);
		def.hideOverallLine = true;
		//Note: The same message provider cannot be used for two different pages
		if(addEmailPage)
			def.messageProvider = "qualityMes_"+def.urlAlias;
		else
			def.messageProvider = null;
		return def;
	}
	
	/** Define/Add quality evaluation page. It should be possible to call this from various apps adding 
	 *   gateways to the evaluation. In this case leave all possible parameters to null
	 *   TODO: At the moment probably the last email definition will be used for the common email of all
	 *   	projects if the same messageProvider is used. Otherwise separate emails are generated.
	 * 
	 * @param resultIds if null the default set of quality defined in {@link #qualityResults} are used
	 * @param configName must be a unique name for the page. If null the default name for the common quality evaluation is used
	 * @param addEmailPage see {@link #getQualityDefaultDefinition(String[], String, boolean)}
	 * @param mode the way the quality results are evaluated
	 * 		0 : no result evaluation
	 *      1 : only standard evaluation with current values per day
	 *      2 : only diff evaluation
	 *      3 : both standard and diff evaluation
	 * @param messageTitle if null configName will be used as title
	 * @param lines if not null the lines provided here will be added after the initial body line
	 * 		of the email before the result lines. Typically these lines contain links relevant for
	 * 		manual analysis of the data provided by this email.
	 * @return
	 */
	public static KPIPageDefinitionWithEmail sampleQualityDefaultDefinitionDiff(String[] resultIds,
			String configNameIn, boolean addEmailPage, int mode, String messageTitle,
			List<String> gatewayIds,
			String... lines) {
		final String[] resultIdsLoc;
		final String configName;
		if(configNameIn == null)
			configName = "Common Data Quality Report";
		else
			configName = configNameIn;
		if(resultIds == null)
			resultIdsLoc = qualityResults;
		else
			resultIdsLoc = resultIds;
		KPIPageDefinitionWithEmail result = new KPIPageDefinitionWithEmail() {
			@Override
			public String getTitle() {
				if(messageTitle == null)
					return configName;
				return messageTitle;
			}

			@Override
			public String getMessage(Collection<KPIStatisticsManagementI> kpis, long currentTime) {
				String mes = "Time of message creation: "+TimeUtils.getDateAndTimeString(currentTime)+"\r\n";
				if(lines != null) for(String line: lines) {
					mes += (line+"\r\n");
				}
				if(mode == 1 || mode == 3)
					mes += getMessageStdTable(kpis, currentTime, qualityResults);
				if(mode == 2 || mode == 3)
					mes += KPIEvalUtil.detectKPIChanges(kpis, currentTime, resultIdsLoc);
				return mes;
			}

		};
		result.kpiPageDefinition = getQualityDefaultDefinition(resultIdsLoc, configName, addEmailPage);
		result.gatewayIdsToEvaluate = gatewayIds;
		return result;
	}

	
	/** The provider may not provide a real evaluation, so the default implementation
	 * should not be started*/
	@Override
	public GaRoDataTypeI[] getGaRoInputTypes() {
		return new GaRoDataTypeI[] {};
	}

	@Override
	protected List<GenericGaRoResultType> resultTypesGaRo() {
		return Collections.emptyList();
	}

	@Override
	protected GenericGaRoEvaluationCore initEval(List<EvaluationInput> input, List<ResultType> requestedResults,
			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time, int size,
			int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
		return null;
	}
}