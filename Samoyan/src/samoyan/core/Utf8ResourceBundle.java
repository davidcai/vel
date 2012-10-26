package samoyan.core;

import java.io.UnsupportedEncodingException;
import java.util.*;

public final class Utf8ResourceBundle
{
	private Utf8ResourceBundle()
	{	
	}
	
	public static final ResourceBundle getBundle(String baseName)
	{
		ResourceBundle bundle = ResourceBundle.getBundle(baseName);
		return createUtf8PropertyResourceBundle(bundle);
	}

	public static final ResourceBundle getBundle(String baseName, Locale locale)
	{
		ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);
		return createUtf8PropertyResourceBundle(bundle);
	}

	public static ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader)
	{
		ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);
		return createUtf8PropertyResourceBundle(bundle);
	}

	private static ResourceBundle createUtf8PropertyResourceBundle(ResourceBundle bundle)
	{
		if (!(bundle instanceof PropertyResourceBundle)) return bundle;
		return new Utf8PropertyResourceBundle((PropertyResourceBundle) bundle);
	}

	private final static class Utf8PropertyResourceBundle extends ResourceBundle
	{
		PropertyResourceBundle bundle;

		private Utf8PropertyResourceBundle(PropertyResourceBundle bundle)
		{
			this.bundle = bundle;
		}

		public Enumeration<String> getKeys()
		{
			return bundle.getKeys();
		}

		protected Object handleGetObject(String key)
		{
 			String value = (String) bundle.getString(key);
 			if (value==null) return null;
			try
			{
				return new String(value.getBytes("ISO-8859-1"), "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
				// Shouldn't fail
				Debug.logStackTrace(e);
				return null;
			}
		}
	}
}
