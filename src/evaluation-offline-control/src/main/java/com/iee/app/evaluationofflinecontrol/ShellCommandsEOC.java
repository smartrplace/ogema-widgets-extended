package com.iee.app.evaluationofflinecontrol;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ForkJoinPool;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.joda.time.DateTimeZone;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.iee.app.evaluationofflinecontrol.gui.element.RemoteSlotsDBBackupButton;
import com.iee.app.evaluationofflinecontrol.util.StandardConfigurations;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

@Descriptor("Evaluation Offline Control commands")
class ShellCommandsEOC {
	
	private final OfflineEvaluationControlController app;
	private final ServiceRegistration<ShellCommandsEOC> ownService;
	
	public ShellCommandsEOC(OfflineEvaluationControlController app, BundleContext ctx) {
		this.app = app; 
		final Dictionary<String, Object> props = new Hashtable<>();
    	props.put("osgi.command.scope", "evaloff");
    	props.put("osgi.command.function", new String[] {
    			"createSlotsZip",
    			"listAutoEvalConfigs"
    			});
		this.ownService = ctx.registerService(ShellCommandsEOC.class, this, props);

	}

	void close() {
		ForkJoinPool.commonPool().submit(ownService::unregister);
	}
	
	@Descriptor("Create Zip file containing slots data for some days for SCP transfer to server")
    public void createSlotsZip(
    		@Parameter(names= {"-d", "--days"}, absentValue="-1")
    		@Descriptor("Days to include into the backup. If negative a zip file for the current day is created.")
    		int daysFromNow) throws IOException {
		long now = app.appMan.getFrameworkTime();
		//slotsDB is aligned according to UTC
		long endTime = AbsoluteTimeHelper.getIntervalStart(now, DateTimeZone.UTC.getID(), AbsoluteTiming.DAY);
		long startTime = AbsoluteTimeHelper.addIntervalsFromAlignedTime(endTime, -1-daysFromNow, DateTimeZone.UTC.getID(), AbsoluteTiming.DAY);
		if(daysFromNow < 0) {
			startTime = endTime;
			endTime = now;
		} else {
			//we set the window as noon to noon to have exactly one folder time (=day start) in the interval
			endTime -= 12*StandardConfigurations.HOUR_MILLIS;
		}
		startTime -= 12*StandardConfigurations.HOUR_MILLIS;
		RemoteSlotsDBBackupButton.performSlotsBackup(Paths.get("./data/"), Paths.get("./data/backupzip/remoteSlots"+StringFormatHelper.getDateForPath(now)+".zip"),
				startTime, endTime, Arrays.asList(new String[] {""}), new File(OfflineEvaluationControlController.generalBackupSource), true);
    }
	
	@Descriptor("List all currently scheduled auto evaluations, including the planned time of execution")
	public void listAutoEvalConfigs() {
		Map<String, Long> execTimes = app.serviceAccess.evalResultMan().getEvalScheduler().getNextExecutionTimes();
		System.out.println(execTimes.size() + " evaluation providers are scheduled to run:");
		
		TimeZone tz = TimeZone.getTimeZone("UTC"); // avoid any time zone ambiguity
		DateFormat df = new SimpleDateFormat("yyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(tz);
		
		execTimes.forEach( (providerId, time) -> {
			System.out.println(providerId + " is scheduled to run at " + df.format(time) + " (" + time + ")");
		} );
	}
}
