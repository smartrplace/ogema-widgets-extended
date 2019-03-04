/**
 * ﻿Copyright 2018-2019 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
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

package org.ogema.timeseries.eval.garo.dp.csv.app;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.timeseries.eval.garo.dp.csv.GaRoMultiEvalDataProviderCSV1;
import org.ogema.tools.timeseriesimport.api.TimeseriesImport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import de.iwes.timeseries.eval.api.DataProvider;

@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class GaRoMultiEvalDataProviderAppCSV implements Application {

	@Reference
	TimeseriesImport csvImport;

	//private ApplicationManager appMan;
	private GaRoMultiEvalDataProviderCSV1 dataProvider;
	
	private BundleContext bc;
	@SuppressWarnings("rawtypes")
	protected ServiceRegistration<DataProvider> sr = null;
	
	@Activate
    void activate(BundleContext bc) {
		this.bc = bc;
    }
	
	@Override
	public void start(ApplicationManager appManager) {
		//this.appMan = appManager;
		
		this.dataProvider = new GaRoMultiEvalDataProviderCSV1(csvImport);
		
		sr = bc.registerService(DataProvider.class, dataProvider, null);
	}

	@Override
	public void stop(AppStopReason reason) {
		if(dataProvider != null) dataProvider.close();
		if (sr != null) {
			sr.unregister();
		}
	}

}
