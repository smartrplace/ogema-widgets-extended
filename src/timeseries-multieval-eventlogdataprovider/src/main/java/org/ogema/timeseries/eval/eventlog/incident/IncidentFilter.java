package org.ogema.timeseries.eval.eventlog.incident;

import java.util.Map;

import org.ogema.timeseries.eval.eventlog.util.EventLogFileParser.EventLogResult;


/**
 * Incidents without an IncidentFilter will be accounted for immediately after having
 * been found in the logfile line.
 * 
 * An IncidentFilter associated with a type of incident provides further filtering capabilities.
 * A filter lets an element pass by having exec() return true. If an incident has passed the filter,
 * it will be further processed, i. e. counted.
 * 
 * @author jruckel
 *
 */
public interface IncidentFilter {
	
	public static final String FLAG_PREFIX_HAS_OCCURRED = "has_occurrred_";
	public static final String FLAG_PREFIX_LAST_OCCURRENCE_TIMESTAMP = "last_occurrence_timestamp_";
	
	public boolean exec(EventLogResult elr);

	
	/**
	 * This filter lets everything pass.
	 * @author jruckel
	 *
	 */
	public class AllPassFilter implements IncidentFilter {
		@Override
		public boolean exec(EventLogResult elr) {
			return true;
		}
	}
	
	/**
	 * This filter rejects everything.
	 * @author jruckel
	 *
	 */
	public class AllRejectFilter implements IncidentFilter {
		@Override
		public boolean exec(EventLogResult elr) {
			return false;
		}
	}
	
	/**
	 * Sets a flag indicating that an event occurred
	 * @author jruckel
	 *
	 */
	public class OccurrenceFlagFilter implements IncidentFilter {
		
		Map<String, Object> flags;
		boolean returnValue;
		boolean hasOccurred;
		String eventName = "";
		String flagKey;

		/**
		 * 
		 * @param flags
		 * @param returnValue whether or not to let the incident pass
		 */
		public OccurrenceFlagFilter(Map<String, Object> flags, boolean returnValue) {
			this(flags, returnValue, true, "");
		}
		
		/**
		 * 
		 * @param flags
		 * @param returnValue whether or not to let the incident pass
		 * @param hasOccured (optional) true -> set the flag; false -> unset the flag
		 * @param eventName (optional) set Occurrence flag for another type of incident
		 */
		public OccurrenceFlagFilter(Map<String, Object> flags, boolean returnValue, boolean hasOccured, 
				String eventName) {
			this.flags = flags;
			this.returnValue = returnValue;
			this.hasOccurred = hasOccured;
			this.eventName = eventName;
		}

		@Override
		public boolean exec(EventLogResult elr) {
			if ("".equals(eventName)) {
				eventName = elr.eventId;
			}
			this.flagKey = FLAG_PREFIX_HAS_OCCURRED + eventName;
			flags.put(flagKey, hasOccurred);
			return returnValue;
		}
	
	}
	
	/**
	 * This filter checks if an event of another type has previously occurred.
	 * If it is, flagToCheck will be reset and the filter lets the incident pass.
	 * @author jruckel
	 *
	 */
	public class CheckOccurrenceFlagFilter implements IncidentFilter {
		
		Map<String, Object> flags;
		String eventIdToCheck;
		String flagKey;
		
		public CheckOccurrenceFlagFilter(Map<String, Object> flags, String eventIdToCheck) {
			this.flags = flags;
			this.eventIdToCheck = eventIdToCheck;
			this.flagKey= FLAG_PREFIX_HAS_OCCURRED + eventIdToCheck;
		}
		
		@Override
		public boolean exec(EventLogResult elr) {
			
			if (! this.flags.containsKey(flagKey)) return false;
			
			if (! (this.flags.get(flagKey) instanceof Boolean)) return false;
			
			if ((boolean)this.flags.get(flagKey)) {
				this.flags.remove(flagKey);
				return true;
			}

			return false;
			
		}
		
	}
	
	/**
	 * Cooldown Filter: incidents of the same type that occur within a minimum duration (default: 1 minute)
	 * can be ignored.
	 * @author jruckel
	 *
	 */
	public class CooldownFilter implements IncidentFilter {
		
		Map<String, Object> flags;
		long minDuration;
		
		/**
		 * 
		 * @param minDuration minimum duration between two incidents that is to be counted seperately [ms]
		 */
		public CooldownFilter(Map<String, Object> flags, long minDuration) {
			this.flags = flags;
			this.minDuration = minDuration;
		}
		
		/**
		 * minDuration defaults to 1 minute
		 */
		public CooldownFilter(Map<String, Object> flags) {
			this(flags, 60000);
		}
		
		/**
		 * Reporting is on a cooldown
		 */
		@Override
		public boolean exec(EventLogResult elr) {
			
			final String KEY = FLAG_PREFIX_LAST_OCCURRENCE_TIMESTAMP + elr.eventId;
			
			if (! flags.containsKey(KEY)) {
				flags.put(KEY, elr.eventTime);
				return true;
			}
			
			long lastErr = (long) flags.get(KEY);
			
			if (elr.eventTime - lastErr < minDuration) {
				return false;
			}
			
			flags.put(KEY, elr.eventTime);
			return true;
		}
	}



}