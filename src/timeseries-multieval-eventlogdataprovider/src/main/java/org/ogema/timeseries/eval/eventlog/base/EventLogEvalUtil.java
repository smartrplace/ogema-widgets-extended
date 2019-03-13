package org.ogema.timeseries.eval.eventlog.base; // TODO: Move to org.ogema.timeseries.eval.eventlog.util

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.util.directresourcegui.kpi.KPIMonitoringReport;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.KPIStatisticsManagementI;
import de.iwes.util.timer.AbsoluteTiming;


public class EventLogEvalUtil {
	
	public static final int COLUMN_WIDTH = 15;
	
	/** Report KPI columns that either dropped significantly or were increased significantly compared
	 * to the day before the current day evaluated.
	 * @param kpis
	 * @param currentTime
	 * @param resultIds resultIds for the kpis, must be same index
	 * @param downThreshold if the current value is below downThreshold * <previous Value> a warning
	 * 		line in the message is generated 
	 * @param upThreshold
	 * @param idsToCheckAlways if a warning was reported for a gateway (table line) usually no more
	 * 		columns are checked. The ids given here will still be checked and potentially additional
	 * 		lines will be generated.
	 * @return
	 */
	public static String detectKPIChanges(Collection<KPIStatisticsManagementI> kpis, long currentTime,
			String[] resultIds, float downThreshold, float upThreshold, List<String> idsToCheckAlways) {

			String mes = "";
			for(KPIStatisticsManagementI gw: kpis) {
				if(gw.specialLindeId().startsWith("Overall")) continue;
				int idx = 0;
				boolean checkStillRexoOnly = false;
				for(KPIStatisticsManagementI kpi2: gw.ksmList()) {
					if(checkStillRexoOnly && (!idsToCheckAlways.contains(kpi2.resultTypeId())))
						continue;
					if(resultIds[idx].equals("timeOfCalculation")) {
						//we do not care about this
						continue;
					} else {
						if(kpi2.resultTypeId().startsWith("$")) {
							//we do not care about String values here
							continue;									
						} else {
							SampledValue sv = kpi2.getValueNonAligned(AbsoluteTiming.DAY, currentTime, 1);
							SampledValue svPrev = kpi2.getValueNonAligned(AbsoluteTiming.DAY, currentTime, 2);
							if((sv == null) && (svPrev != null)) {
								mes += (getRightAlignedString(gw.specialLindeId(), COLUMN_WIDTH)+" has NO value ANYMORE in "+kpi2.resultTypeId());
							} else if((sv != null) && (svPrev == null)) {
								mes += (getRightAlignedString(gw.specialLindeId(), COLUMN_WIDTH)+" got back value for "+kpi2.resultTypeId())+" : "+sv.getValue().getFloatValue();
							} else if(sv != null) {
								float val = sv.getValue().getFloatValue();
								float valPrev = svPrev.getValue().getFloatValue();
								int mode = 0;
								if(val < downThreshold*valPrev) mode = -1;
								else if(val > upThreshold*valPrev) mode = 1;
								if(mode != 0) {
									final GaRoDataTypeI dataType;
									GaRoSingleEvalProvider prov = kpi2.getEvalProvider();
									ResultType r = null;
									if(prov != null ) {
										r  = KPIMonitoringReport.getResultTypeById(prov, kpi2.resultTypeId());
									}
									if(r != null && r instanceof GaRoDataTypeI) dataType = (GaRoDataTypeI) r;
									else dataType = null;
									mes += (getRightAlignedString(gw.specialLindeId(), COLUMN_WIDTH)+"#"+kpi2.resultTypeId()+
											((mode>0)?" jumped up ":" fell down ")+" from "+KPIMonitoringReport.formatValue(svPrev, dataType)+
											" to "+KPIMonitoringReport.formatValue(sv, dataType)+"\r\n");
									checkStillRexoOnly = true;
								}
							}
						}
					}
					idx++;
				}
			}
			return mes;
			
		}
	
	public static String getRightAlignedString(String in, int len) {
		if(in.length() >= len) return in.substring(0, len);
		return StringUtils.repeat(' ', len-in.length())+in;
	}
	

}
