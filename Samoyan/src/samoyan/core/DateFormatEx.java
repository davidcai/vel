package samoyan.core;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class DateFormatEx
{
	private static ConcurrentHashMap<String, String> patterns = new ConcurrentHashMap<String, String>();
	
	public final static DateFormat getISO8601Instance()
	{
		return applyTimeZone(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US), TimeZoneEx.GMT);
	}

	public final static DateFormat getISO8601MillisInstance()
	{
		return applyTimeZone(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'", Locale.US), TimeZoneEx.GMT);
	}

	public final static DateFormat getSimpleInstance(String pattern, Locale locale, TimeZone tz)
	{
		return applyTimeZone(new SimpleDateFormat(pattern, locale), tz);
	}
	
	public final static DateFormat getDateInstance(Locale locale, TimeZone tz)
	{
		return applyTimeZone(DateFormat.getDateInstance(DateFormat.SHORT, locale), tz);
	}
	
	public final static DateFormat getMediumDateInstance(Locale locale, TimeZone tz)
	{
		return applyTimeZone(DateFormat.getDateInstance(DateFormat.MEDIUM, locale), tz);
	}
	
	public final static DateFormat getLongDateInstance(Locale locale, TimeZone tz)
	{
		return applyTimeZone(DateFormat.getDateInstance(DateFormat.LONG, locale), tz);
	}
	
	public final static DateFormat getTimeInstance(Locale locale, TimeZone tz)
	{
		return applyTimeZone(DateFormat.getTimeInstance(DateFormat.SHORT, locale), tz);
	}

	public final static DateFormat getDateTimeInstance(Locale locale, TimeZone tz)
	{
		return applyTimeZone(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale), tz);
	}


	/**
	 * Returns a <code>DateFormat.SHORT</code> date format, with a 4 digit year.
	 * @param locale
	 * @param tz
	 * @return
	 */
	public final static DateFormat getY4DateInstance(Locale locale, TimeZone tz)
	{
		if (locale.equals(Locale.US))
		{
			return applyTimeZone(new SimpleDateFormat("M/d/yyyy"), tz);
		}
		
		String pattern = patterns.get("date." + locale.toString());
		if (pattern!=null)
		{
			return applyTimeZone(new SimpleDateFormat(pattern), tz);
		}
		
		DateFormat formatter = DateFormat.getDateInstance(DateFormat.SHORT, locale);
		if (formatter instanceof SimpleDateFormat)
		{
			SimpleDateFormat simple = (SimpleDateFormat) formatter;
			pattern = simple.toLocalizedPattern();
			if (pattern.indexOf("yyyy")<0 && pattern.indexOf("yy")>=0)
			{
				pattern = Util.strReplace(pattern, "yy", "yyyy");
				simple.applyLocalizedPattern(pattern);
			}
			patterns.put("date." + locale.toString(), pattern);
		}
		return applyTimeZone(formatter, tz);
	}
	
	/**
	 * Returns a <code>DateFormat.SHORT</code> date and time format, with a 4 digit year.
	 * @param locale
	 * @param tz
	 * @return
	 */
	public final static DateFormat getY4DateTimeInstance(Locale locale, TimeZone tz)
	{
		if (locale.equals(Locale.US))
		{
			SimpleDateFormat simple = new SimpleDateFormat("M/d/yyyy h:mm:ss a");
//			DateFormatSymbols dfs = simple.getDateFormatSymbols();
//			String[] ampm = {"a", "p"};
//			dfs.setAmPmStrings(ampm);
//			simple.setDateFormatSymbols(dfs);
			return applyTimeZone(simple, tz);
		}
		
		String pattern = patterns.get("datetime." + locale.toString());
		if (pattern!=null)
		{
			return applyTimeZone(new SimpleDateFormat(pattern), tz);
		}
		
		DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
		if (formatter instanceof SimpleDateFormat)
		{
			SimpleDateFormat simple = (SimpleDateFormat) formatter;
			pattern = simple.toLocalizedPattern();
			if (pattern.indexOf("yyyy")<0 && pattern.indexOf("yy")>=0)
			{
				pattern = Util.strReplace(pattern, "yy", "yyyy");
				simple.applyLocalizedPattern(pattern);
			}
			patterns.put("datetime." + locale.toString(), pattern);
		}
		return applyTimeZone(formatter, tz);
	}

	public final static DateFormat getMiniDateInstance(Locale locale, TimeZone tz)
	{
		if (locale.equals(Locale.US))
		{
			return applyTimeZone(new SimpleDateFormat("M/d"), tz);
		}
		
		String pattern = patterns.get("minidate." + locale.toString());
		if (pattern!=null)
		{
			return applyTimeZone(new SimpleDateFormat(pattern), tz);
		}
		
		DateFormat formatter = DateFormat.getDateInstance(DateFormat.SHORT, locale);
		if (formatter instanceof SimpleDateFormat)
		{
			SimpleDateFormat simple = (SimpleDateFormat) formatter;
			pattern = simple.toLocalizedPattern();
			
			// Remove year
			pattern = Util.strReplace(pattern, "y", "");

			// Remove leading symbols
			while (Character.isLetter(pattern.charAt(0))==false)
			{
				pattern = pattern.substring(1);
			}

			// Remove trailing symbols
			while (Character.isLetter(pattern.charAt(pattern.length()-1))==false)
			{
				pattern = pattern.substring(0, pattern.length()-1);
			}
			
			simple.applyLocalizedPattern(pattern);
			patterns.put("minidate." + locale.toString(), pattern);
		}
		return applyTimeZone(formatter, tz);
	}

	public static DateFormat getMiniTimeInstance(Locale locale, TimeZone tz)
	{
		if (locale.equals(Locale.US))
		{
			SimpleDateFormat simple = new SimpleDateFormat("h:mma");
			
			DateFormatSymbols dfs = simple.getDateFormatSymbols();
			String[] ampm = {"A", "P"};
			dfs.setAmPmStrings(ampm);
			simple.setDateFormatSymbols(dfs);
			
			return applyTimeZone(simple, tz);
		}
		
		String pattern = patterns.get("minitime." + locale.toString());
		if (pattern!=null)
		{
			return applyTimeZone(new SimpleDateFormat(pattern), tz);
		}

		DateFormat formatter = DateFormat.getDateInstance(DateFormat.SHORT, locale);
		if (formatter instanceof SimpleDateFormat)
		{
			SimpleDateFormat simple = (SimpleDateFormat) formatter;
			pattern = simple.toLocalizedPattern();
			
			// Remove seconds
			pattern = Util.strReplace(pattern, "s", "");

			// Remove leading symbols
			while (Character.isLetter(pattern.charAt(0))==false)
			{
				pattern = pattern.substring(1);
			}

			// Remove trailing symbols
			while (Character.isLetter(pattern.charAt(pattern.length()-1))==false)
			{
				pattern = pattern.substring(0, pattern.length()-1);
			}
			
			simple.applyLocalizedPattern(pattern);
			patterns.put("minitime." + locale.toString(), pattern);
		}
		return applyTimeZone(formatter, tz);
	}
	
	public static DateFormat getMiniDateTimeInstance(Locale locale, TimeZone tz)
	{
		if (locale.equals(Locale.US))
		{
			SimpleDateFormat simple = new SimpleDateFormat("M/d h:mma");
			
			DateFormatSymbols dfs = simple.getDateFormatSymbols();
			String[] ampm = {"A", "P"};
			dfs.setAmPmStrings(ampm);
			simple.setDateFormatSymbols(dfs);
			
			return applyTimeZone(simple, tz);
		}
		
		String pattern = patterns.get("minidatetime." + locale.toString());
		if (pattern!=null)
		{
			return applyTimeZone(new SimpleDateFormat(pattern), tz);
		}

		SimpleDateFormat df = (SimpleDateFormat) getMiniDateInstance(locale, tz);
		SimpleDateFormat tf = (SimpleDateFormat) getMiniTimeInstance(locale, tz);

		pattern = df.toLocalizedPattern() + " " + tf.toLocalizedPattern();
		patterns.put("minidatetime." + locale.toString(), pattern);
		
		SimpleDateFormat result = new SimpleDateFormat(pattern, locale);
		return applyTimeZone(result, tz);
	}
	
	private static DateFormat applyTimeZone(DateFormat formatter, TimeZone tz)
	{
		formatter.setTimeZone(tz);
		formatter.setLenient(false);
		return formatter;
	}
	
	public static String getPattern(DateFormat df)
	{
		SimpleDateFormat simple = (SimpleDateFormat) df;
		return simple.toLocalizedPattern();
	}	
}
