package org.ogema.timeseries.eval.garo.dp.csv.app;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.timeseries.eval.garo.dp.csv.GaRoMultiEvalDataProviderCSV1;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import de.iwes.timeseries.eval.api.DataProvider;

@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class GaRoMultiEvalDataProviderAppCSV implements Application {

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
		
		this.dataProvider = new GaRoMultiEvalDataProviderCSV1();
		
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
