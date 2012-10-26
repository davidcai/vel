package samoyan.core;

import java.text.DateFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class StringBundle
{
	private static Map<String, ResourceBundle> localCache = new ConcurrentHashMap<String, ResourceBundle>(32);
	
	/*
	 * Can't instantiate directly.
	 */
	private StringBundle()
	{
	}
	
	private static ResourceBundle getBundle(Locale loc, String bundleID)
	{		
		// Look up in cache first
		String key = bundleID + "." + loc.toString();
		ResourceBundle cached = localCache.get(key);
		if (cached!=null)
		{
			return cached;
		}
				
		// Create new and cache for future use
		ResourceBundle result = Utf8ResourceBundle.getBundle(bundleID, loc);
		localCache.put(key, result);
		
		return result;
	}

	/**
	 * Get a static string from the resource bundle.
	 * @param id The ID of the string as it appears in the .properties file.
	 * @return The string from the ResourceBundle or <code>null</code> if not found.
	 */
	public static String getString(Locale loc, TimeZone tz, String id)
	{
		return getString(loc, tz, id, null);
	}

	/**
	 * Get a pattern string from the resource bundle and apply a <code>MessageFormat</code> to it
	 * using the variables.
	 * @param id The ID of the string as it appears in the .properties file.
	 * @param vars The variables to apply to the pattern.
	 * @return The string from the ResourceBundle or <code>null</code> if not found.
	 */
	public static String getString(Locale loc, TimeZone tz, String id, Object[] vars)
	{
		int p = id.indexOf(":");
		if (p<0) return null;
		String bundleID = id.substring(0, p);
		String stringID = id.substring(p+1);

		ResourceBundle bundle = getBundle(loc, bundleID);
		if (bundle==null)
		{
			Debug.logln("String '" + id + "' not found");
			return null;
		}
				
		if (bundle.containsKey(stringID)==false)
		{
			Debug.logln("String '" + id + "' not found");
			return null;
		}

		String pattern = null;
		try
		{
			pattern = bundle.getString(stringID);
		}
		catch (java.util.MissingResourceException exc1)
		{
			Debug.logln("String '" + id + "' not found");
			return null;
		}
		
		if (vars==null || vars.length==0)
		{
			return Util.strReplace(pattern, "''", "'");
		}
		else
		{
			return formatString(loc, tz, pattern, vars);
		}
	}
	
	/**
	 * Format a pattern string applying a <code>MessageFormat</code> to it
	 * using the variables.
	 * @param pattern The pattern.
	 * @param vars The variables to apply to the pattern.
	 * @return The string from the ResourceBundle or <code>null</code> if not found.
	 */
	public static String formatString(Locale loc, TimeZone tz, String pattern, Object[] vars)
	{
		if (vars==null || vars.length==0)
		{
			return Util.strReplace(pattern, "''", "'");
		}
		
		MessageFormat formatter = new MessageFormat(pattern, loc);
		
		// Set timezone on date formats
		boolean hasDate = false;
		for (int v=0; v<vars.length; v++)
		{
			if (vars[v]!=null && vars[v] instanceof Date)
			{
				hasDate = true;
				break;
			}
		}
		
		if (hasDate)
		{
			DateFormat dateFormatter = null;
			if (tz==null) tz = TimeZone.getDefault();

			// Format date params as {0,date,short} if not specified in the pattern
			Format[] formats = formatter.getFormatsByArgumentIndex();
			if (formats.length==vars.length) // Should be true
			{
				for (int f=0; f<formats.length; f++)
				{
					if (!(formats[f] instanceof DateFormat) &&
						vars[f]!=null &&
						vars[f] instanceof Date)
					{
						if (dateFormatter==null)
						{
							dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT, loc);
							dateFormatter.setTimeZone(tz);
						}
						vars[f] = dateFormatter.format((Date) vars[f]);
					}
				}
			}
			
			// Set correct time zone on date formats
			formats = formatter.getFormats();
			for (int f=0; f<formats.length; f++)
			{
				if (formats[f] instanceof DateFormat)
				{
					DateFormat df = (DateFormat) formats[f];
					df.setTimeZone(tz);
				}
			} 
		}
		
		return formatter.format(vars);
	}
	
	/**
	 * Returns a <code>Map</code> of all the properties with keys that begin with the
	 * specified prefix. The map contains new keys equivalent to the original key
	 * less the prefix. For example, if a property is defined as "db.color.0.FG1" and
	 * this method is called with "db.color.0." the result is a property named "FG1".
	 * @param idPrefix The prefix to search for and eliminate from the original keys.
	 * @return
	 */
	public static Map<String, String> getStrings(Locale loc, String idPrefix)
	{
		int p = idPrefix.indexOf(":");
		if (p<0) return null;
		String bundleID = idPrefix.substring(0, p);
		String stringID = idPrefix.substring(p+1);

		try
		{
			ResourceBundle bundle = getBundle(loc, bundleID);
			Map<String, String> result = new HashMap<String, String>();
			Enumeration<String> keys = bundle.getKeys();
			while (keys.hasMoreElements())
			{
				String key = (String) keys.nextElement();
				if (key.startsWith(stringID))
				{
					result.put(key.substring(stringID.length()), bundle.getString(key));
				}
			}
			
			return result;
		}
		catch (java.util.MissingResourceException exc)
		{
			return null;
		}
	}

	/**
	 * @see #getString(String, Object[])
	 */
	public static  String getString(Locale loc, TimeZone tz, String id, Object var1)
	{
		Object[] objs = new Object[1];
		objs[0] = var1;
		return getString(loc, tz, id, objs);
	}
	
	/**
	 * @see #getString(String, Object[])
	 */
	public static  String getString(Locale loc, TimeZone tz, String id, Object var1, Object var2)
	{
		Object[] objs = new Object[2];
		objs[0] = var1;
		objs[1] = var2;
		return getString(loc, tz, id, objs);
	}

	/**
	 * @see #getString(String, Object[])
	 */
	public static String getString(Locale loc, TimeZone tz, String id, Object var1, Object var2, Object var3)
	{
		Object[] objs = new Object[3];
		objs[0] = var1;
		objs[1] = var2;
		objs[2] = var3;
		return getString(loc, tz, id, objs);
	}

}
