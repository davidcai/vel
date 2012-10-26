package samoyan.servlet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import samoyan.core.StringBundle;

public class Setup
{
	private static boolean app = true;
	private static Map<String, String> cache = new ConcurrentHashMap<String, String>();
	private static String NULL_VALUE = "null"; 
	
	private static String getString(String key)
	{
		return getString(key, Locale.getDefault());
	}
	
	private static String getString(String key, Locale locale)
	{
		if (cache.containsKey(key))
		{
			String val = cache.get(key);
			if (val==NULL_VALUE)
			{
				return null;
			}
		}
		
		String value = null;
		if (app)
		{
			try
			{
				value = StringBundle.getString(locale, TimeZone.getDefault(), "deployment:" + key);
			}
			catch (MissingResourceException mre)
			{
				// Running as platform, no application
				app = false;
				value = null;
			}
		}
		if (value==null)
		{
			value = StringBundle.getString(locale, TimeZone.getDefault(), "platform:" + key);
		}
		
		if (value==null)
		{
			cache.put(key, NULL_VALUE);
		}
		else
		{
			cache.put(key, value);
		}
		return value;
	}

	private static int getInt(String propName)
	{
		String val = getString(propName);
		return Integer.parseInt(val);
	}

	private static boolean getBoolean(String propName)
	{
		String val = getString(propName);
		return Boolean.parseBoolean(val);
	}
	
	// - - - - -
	// APP
	
	public static String getAppTitle(Locale locale)
	{
		return getString("App.Title", locale);
	}
	public static String getAppOwner(Locale locale)
	{
		return getString("App.Owner", locale);
	}
	public static String getAppID()
	{
		return getString("App.ID");
	}
	public static String getAppAddress(Locale locale)
	{
		return getString("App.Address", locale);
	}
	public static String getAppEmail(Locale locale)
	{
		return getString("App.Email", locale);
	}

	// - - - - -
	// SERVER
	
	public static int getCacheCapacity()
	{
		return getInt("Server.CacheCapacity");
	}

	public static long getClientCacheExpires()
	{
		return 1000L * getInt("Server.ClientCacheExpires");
	}
	
	public static long getCookieExpires()
	{
		return 1000L * getInt("Server.CookieExpires");
	}

	public static long getSessionLength()
	{
		return 1000L * getInt("Server.SessionLength");
	}
	
	public static boolean isDebug()
	{
		return getBoolean("Server.Debug");
	}
	
	public static boolean isSSL()
	{
		return getBoolean("Server.SSL");
	}

	public static String getHost()
	{
		return getString("Server.Host");
	}
	
	public static int getPort()
	{
		return getInt("Server.Port");
	}
	
	public static int getPortSSL()
	{
		return getInt("Server.PortSSL");
	}
	
	public static String getPath()
	{
		return getString("Server.Path");
	}

	// - - - - -
	// DATABASE
	
	public static String getDatabaseDriver()
	{
		return getString("Database.Driver");
	}
	public static String getDatabaseURL()
	{
		return getString("Database.URL");
	}
	public static String getDatabaseUser()
	{
		return getString("Database.User");
	}
	public static String getDatabasePassword()
	{
		return getString("Database.Password");
	}	
}
