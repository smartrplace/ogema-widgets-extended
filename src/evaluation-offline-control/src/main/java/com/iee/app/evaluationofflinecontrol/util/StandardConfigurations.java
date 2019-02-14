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

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.externalviewer.extensions.IntervalConfiguration;

import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

public class StandardConfigurations {
	public static final long MINUTE_MILLIS = 60000;
	public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
	public static final long DAY_MILLIS = 24 * HOUR_MILLIS;

	static long[] startEnd = {1483228800000l, 1485907200000l, 1488326400000l,
            1491004800000l, 1493596800000l, 1496275200000l,1498867200000l,1501545600000l,1504224000000l,1506816000000l,1509494400000l,1512086400000l,1514764800000l,1517443200000l};
    static long startEnd1Norm = 1485903600000l;
	
       
    //From 01.02.2017 00:00 CET to 01.02.2018 00:00
    static long[] fullYearFeb2018KS = {1485903600000l, 1517439600000l}; 
    static long[] FebMai2017KS = {1485903600000l, 1496268000000l};
    static long[] JuneSept2017KS = {1496268000000l, 1506808800000l};
    static long[] OctJan2017_18KS = {1506808800000l, 1517439600000l}; 
    static long[] fullYearJan2018KSPlus = {1483225200000l, 1517439600000l}; 
    static long[] startEndTotal = {startEnd[0] + 600000, startEnd[startEnd.length - 1]};
    static long[] OctFeb2017_18KS = {1506808800000l,1519858800000l};

    public static List<String> getConfigOptions() {
		List<String> configOptions = new ArrayList<>();
		
		configOptions.add("ThreeDays");
		configOptions.add("TestOneWeek");
		configOptions.add("TestOneMonth");
		configOptions.add("Test From Feb-June");
		configOptions.add("Test from Feb-Mai");
		configOptions.add("Test from June-Sep");
		configOptions.add("Test from Oct-Jan");
		configOptions.add("Test from Oct-Feb");
		configOptions.add("TestFiveMonths");
		configOptions.add("One Year");
		configOptions.add("One Year (3 Sections)");
		configOptions.add("6 Days (3 Sections)");
		configOptions.add("Calendar Year");
		configOptions.add("OneFullDayBeforeNow");
		configOptions.add("OneFullDayBeforeNow(+1)");
		configOptions.add("ThreeFullDaysBeforeNow");
		configOptions.add("14FullDaysBeforeNow");
		configOptions.add("OneFullWeekBeforeNow");
		configOptions.add("OneFullMonthBeforeNow");
		configOptions.add("OneFullYearBeforeNow");
		configOptions.add("Jan2018");

    	return configOptions;
    }

    /*public static class IntervalConfiguration {
    	public long start = 0;
    	public long end = 0;
    	public long[] multiStart = null;
    	public long[] multiEnd = null;
    	public String[] multiFileSuffix = null;
    }*/
    
    /** Get evaluation parameter from fixed option.
     * 
     * @param config
     * @param appMan may be null if no option depending on current time is used
     * @return
     */
    public static IntervalConfiguration getConfigDuration(String config, ApplicationManager appMan) {
    	IntervalConfiguration r = new IntervalConfiguration();
    	switch(config) {
		case "ThreeDays":					//3 days
			r.start = startEnd[1];
			r.end = startEnd[1]+2*24*60*60000-1;
			break;
		case "OneFullDayBeforeNow":					//3 days
			long now = appMan.getFrameworkTime();
			r.end = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY)-1;
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -1, AbsoluteTiming.DAY);
			//end = startEnd[1]+2*24*60*60000-1;
			break;
		case "OneFullDayBeforeNow(+1)":					//3 days
			now = appMan.getFrameworkTime();
			r.end = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY)-1;
			r.end = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -1, AbsoluteTiming.DAY);
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -1, AbsoluteTiming.DAY);
			//end = startEnd[1]+2*24*60*60000-1;
			break;
		case "ThreeFullDaysBeforeNow":					//3 days
			now = appMan.getFrameworkTime();
			r.end = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY)-1;
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -3, AbsoluteTiming.DAY)-1;
			//end = startEnd[1]+2*24*60*60000-1;
			break;
		case "14FullDaysBeforeNow":					//14 days
			now = appMan.getFrameworkTime();
			r.end = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY)-1;
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -14, AbsoluteTiming.DAY)-1;
			//end = startEnd[1]+2*24*60*60000-1;
			break;
		case "OneFullWeekBeforeNow":					//3 days
			now = appMan.getFrameworkTime();
			r.end = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.WEEK)-1;
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -1, AbsoluteTiming.WEEK);
			//end = startEnd[1]+2*24*60*60000-1;
			break;
		case "OneFullMonthBeforeNow":					//3 days
			now = appMan.getFrameworkTime();
			r.end = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.MONTH)-1;
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -1, AbsoluteTiming.MONTH);
			//end = startEnd[1]+2*24*60*60000-1;
			break;
		case "OneFullYearBeforeNow":					//3 days
			now = appMan.getFrameworkTime();
			r.end = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.YEAR)-1;
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(r.end+1, -1, AbsoluteTiming.YEAR);
			//end = startEnd[1]+2*24*60*60000-1;
			break;
		case "TestOneWeek":					//9 days
			r.start = startEnd[1];
			r.end = startEnd[1]+(startEnd[2]-startEnd[1])/3;
			break;
			
		case "TestOneMonth":				//28 days
			r.start = startEnd[1];
			r.end = startEnd[2];
			break;
			
		case "Test From Feb-June":
			r.start = startEnd[1];
			r.end	= startEndTotal[1];
			break;
			
		case "Test from Feb-Mai":
			r.start = FebMai2017KS[0];
			r.end = FebMai2017KS[1];
			break;
			
		case "Test from June-Sep":
			r.start = JuneSept2017KS[0];
			r.end = JuneSept2017KS[1];
			break;
			
		case "Test from Oct-Jan":
			r.start = OctJan2017_18KS[0];
			r.end = OctJan2017_18KS[1];
			break;	
		case "Oct2018":
			r.end = OctJan2017_18KS[1];			
			r.start = r.end - 31*24*60*60000;
			break;
		case "One Year":
			r.start = fullYearFeb2018KS[0];
			r.end	= fullYearFeb2018KS[1];
			break;
		case "One Year (3 Sections)":
			r.multiStart = new long[] {FebMai2017KS[0], JuneSept2017KS[0], OctJan2017_18KS[0]};
			r.multiEnd = new long[] {FebMai2017KS[1], JuneSept2017KS[1], OctJan2017_18KS[1]};
			r.multiFileSuffix = new String[] {"_FebMai", "_JuneSept", "_OctJan"};
			break;
		case "6 Days (3 Sections)":
			r.multiStart = new long[] {startEnd1Norm, startEnd1Norm+2*24*60*60000, startEnd1Norm+4*24*60*60000};
			r.multiEnd = new long[] {startEnd1Norm+2*24*60*60000, startEnd1Norm+4*24*60*60000, startEnd1Norm+6*24*60*60000};
			r.multiFileSuffix = new String[] {"_FebMai", "_JuneSept", "_OctJan"};
			break;
		case "Calendar Year":
			r.start = fullYearJan2018KSPlus[0];
			r.end	= fullYearJan2018KSPlus[1];
			break;
		case "Test from Oct-Feb":
			r.start = OctFeb2017_18KS[0];
			r.end = OctFeb2017_18KS[1];
			break;	
		case "Last 12 Hours":
			now = appMan.getFrameworkTime();
			r.end = now;
			r.start = r.end - 12*HOUR_MILLIS;
			break;	
		default: 
			throw new IllegalArgumentException("Invalid Argument " + config);
		}
    	return r;
    }
}
