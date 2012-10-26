package samoyan.core;

import java.util.Locale;
import java.util.TimeZone;

public class TimeZoneEx
{
	private final static String[] primaryIDs = {
		"Pacific/Samoa",
		"America/Adak",
		"Pacific/Honolulu",
		"America/Anchorage",
		"America/Los_Angeles",
		"America/Denver",
		"America/Phoenix",
		"America/Chicago",
		"America/Costa_Rica",
		"America/Mexico_City",
		"America/Regina",
		"America/New_York",
		"America/Halifax",
		"America/St_Johns",
		"America/Buenos_Aires",
		"America/Sao_Paulo",
		"Europe/London",
		"Europe/Paris",
		"Africa/Cairo",
		"Asia/Jerusalem",
		"Europe/Istanbul",
		"Asia/Baghdad",
		"Asia/Tehran",
		"Asia/Dubai",
		"Europe/Moscow",
		"Asia/Karachi",
		"Asia/Calcutta",
		"Asia/Katmandu",
		"Asia/Rangoon",
		"Asia/Bangkok",
		"Asia/Hong_Kong",
		"Asia/Singapore",
		"Asia/Taipei",
		"Australia/Perth",
		"Australia/Eucla",
		"Asia/Seoul",
		"Asia/Tokyo",
		"Australia/Adelaide",
		"Australia/Darwin",
		"Australia/Brisbane",
		"Australia/Sydney",
		"Pacific/Guam",
		"Australia/Lord_Howe",
		"Pacific/Auckland"
	};
	
	private final static String[] northAmericaIDs = {
		"America/St_Johns",
		"America/Halifax",
		"America/New_York",
		"America/Chicago",
		"America/Regina",
		"America/Denver",
		"America/Phoenix",
		"America/Los_Angeles",
		"America/Anchorage",
		"Pacific/Honolulu"
	};
	
	public final static TimeZone GMT = TimeZone.getTimeZone("GMT");
	
	/**
	 * Returns the IDs of the most important time zones, such as US, Europe, etc.
	 * @return
	 */
	public static String[] getPrimaryIDs()
	{
		return primaryIDs;
	}

	/**
	 * Returns the IDs of Canada and US time zones, ordered from east to west.
	 * @return
	 */
	public static String[] getNorthAmericaIDs()
	{
		return northAmericaIDs;
	}

	/**
	 * Attempts to figure out the named time zone of an unnamed timzezone. This method is not a very efficient and should be called with caution.
	 * @param tz The offset from GMT at current time (now)
	 * @return The best guess for the time zone, or <code>null</code> if cannot guess with high certainty.
	 */
	public static TimeZone getByOffsetNow(int offset)
	{
		// !$! Use caching here if this method proves to be a bottleneck.
	    // Check if DST offset is same 24hrs from now, then can cache for 24 hours, otherwise, for 15min blocks.
		
		long now = System.currentTimeMillis();
		
		// The order of the timezones in this list matters because first match is taken.
		// Timezones here should be "primary" only.
		String[] importantIDs = {
				"America/New_York",
				"America/Los_Angeles",
				"America/Chicago",
				"America/Denver",
				"Europe/London",
				"Europe/Paris",
				"America/Anchorage",
				"Pacific/Honolulu",
				"Australia/Sydney",
				"Australia/Darwin",
				"Australia/Perth",
				"Australia/Brisbane", // Does not observe DST
				"America/St_Johns",
				"America/Halifax",
				"Pacific/Auckland",
				"Asia/Calcutta"};
		for (String tzName : importantIDs)
		{
			TimeZone namedTz = TimeZone.getTimeZone(tzName);
			if (namedTz.getOffset(now)==offset)
			{
				return namedTz;
			}
		}
		// !$! The logic above can be improved to take locale into account, e.g. to figure out country from locale.
		
		for (String tzName : primaryIDs)
		{
			TimeZone namedTz = TimeZone.getTimeZone(tzName);
			if (namedTz.getOffset(now)==offset)
			{
				return namedTz;
			}
		}
		
		for (String tzName : TimeZone.getAvailableIDs())
		{
			TimeZone namedTz = TimeZone.getTimeZone(tzName);
			if (tzName.startsWith("Etc/GMT") && namedTz.getOffset(now)==offset)
			{
				return namedTz;
			}
		}
		
		return TimeZone.getTimeZone("GMT"); // Should not happen for valid offsets		
	}
	
	public static String getDisplayString(TimeZone tz, Locale loc)
	{
		String tzID = tz.getID().replace('_', ' ');
		String desc = tz.getDisplayName(loc);
		int p = tzID.lastIndexOf("/");
		if (p>=0 && desc.indexOf("(")<0)
		{
			desc += " (" + tzID.substring(p+1) + ")";
		}
		return desc;
	}
}
