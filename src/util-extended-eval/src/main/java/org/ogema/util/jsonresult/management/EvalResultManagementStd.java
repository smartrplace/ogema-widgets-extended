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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.model.jsonresult.JSONResultFileData;
import org.ogema.model.jsonresult.JsonOGEMAFileManagementData;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;
import org.ogema.util.resourcebackup.ResourceImportExportManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.smartrplace.util.format.ValueFormat;

import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GaRoTestStarter;
import de.iwes.util.timer.AbsoluteTiming;

/** Standard Eval result management*/
@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class EvalResultManagementStd implements Application {
	public static final String FILE_PATH = GaRoTestStarter.FILE_PATH; //System.getProperty("de.iwes.tools.timeseries-multieval.resultpath", "../evaluationresults");
	
	public static final int STANDARD_MULTIEVAL_INTERVAL_STEP = Integer.getInteger("org.ogema.multieval.base_interval_type", 
			AbsoluteTiming.DAY);
	public static final int DEFAULT_INTERVALS_TO_CALCULATE = 3;
	
	//@Reference
	//GatewayBackupAnalysisAccess gatewayParser;
	
	private ApplicationManager appMan;
	private EvalResultManagementImpl evalResultMan;
	
	private BundleContext bc;
	protected ServiceRegistration<EvalResultManagement> sr = null;
	
	@Activate
    void activate(BundleContext bc) {
		this.bc = bc;
    }
	
	//private static volatile EvalResultManagementStd myInstance = null;
	//private static String synch = "";
	
	/*public static EvalResultManagementStd getInstance(ApplicationManager appMan) {
		synchronized(synch) {
			if(myInstance == null) {
				JsonOGEMAFileManagementData mgmtRes = ValueFormat.getStdTopLevelResource(
						JsonOGEMAFileManagementData.class, appMan.getResourceManagement());
				myInstance = new EvalResultManagementStd(mgmtRes, FILE_PATH);
			}
		}
		return myInstance;
	}
	
	protected EvalResultManagementStd(JsonOGEMAFileManagementData appData) {
		super(appData);
	}
	protected EvalResultManagementStd(JsonOGEMAFileManagementData appData, String basePath) {
		super(appData, basePath);
	}
	
	protected EvalResultManagementStd(Resource parent, String managementName, String basePath) {
		super(parent, managementName, basePath);
	}*/

	@Override
	public void start(ApplicationManager appManager) {
		this.appMan = appManager;
		
		JsonOGEMAFileManagementData mgmtRes = ValueFormat.getStdTopLevelResource(
				JsonOGEMAFileManagementData.class, appMan.getResourceManagement());
		
		//EvalSchedulerImpl evalSched = new EvalSchedulerImpl(schedRes, STANDARD_MULTIEVAL_INTERVAL_STEP);

		evalResultMan = new EvalResultManagementImpl(mgmtRes, FILE_PATH, STANDARD_MULTIEVAL_INTERVAL_STEP,
				appMan);
		sr = bc.registerService(EvalResultManagement.class, evalResultMan, null);
		
		//TODO: Direct import would be better
		ResourceImportExportManager.moveImportedIntoList(evalResultMan.currentWorkspace.fileData(), JSONResultFileData.class, appManager);
	}

	@Override
	public void stop(AppStopReason reason) {
		if(evalResultMan != null) evalResultMan.close();
		if (sr != null) {
			sr.unregister();
		}
	}
}
