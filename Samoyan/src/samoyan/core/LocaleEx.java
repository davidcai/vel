package samoyan.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocaleEx
{
	public static Locale bestMatch(List<Locale> availableLocales, Locale requestedLocale)
	{
		List<Locale> requestedLocales = new ArrayList<Locale>(1);
		requestedLocales.add(requestedLocale);
		return bestMatch(availableLocales, requestedLocales);
	}
	
	/**
	 * Find the locale, out of the available ones, that best matches any of the requested ones.
	 * The matching is done down to the country level of the locales, ignoring any variants.
	 * @param availableLocales The locales available in the system, ordered with the most important one first.
	 * @param requestedLocales The locales indicated in the request, ordered with the most important one first.
	 * @return One of the available locales or <code>null</code> if none were provided.
	 */
	public static Locale bestMatch(List<Locale> availableLocales, List<Locale> requestedLocales)
	{
		if (availableLocales==null || availableLocales.size()==0)
		{
			return null;
		}
		if (availableLocales.size()==1 || requestedLocales==null || requestedLocales.size()==0)
		{
			return availableLocales.get(0);
		}
		
		// Look for exact match and language match
		Locale languageMatch = null;
		for (Locale r : requestedLocales)
		{
			for (Locale a : availableLocales)
			{
				if (Util.objectsEqual(a.getLanguage(), r.getLanguage()) &&
					Util.objectsEqual(a.getCountry(), r.getCountry()))
				{
					return a;
				}
				if (languageMatch==null && Util.objectsEqual(a.getLanguage(), r.getLanguage()))
				{
					languageMatch = a;
				}
			}
		}
		return languageMatch==null? availableLocales.get(0) : languageMatch;
	}

	public static Locale fromString(String locStr)
	{
		if (locStr==null)
		{
			return null;
		}
		else
		{
			Locale loc;
			
			int p = locStr.indexOf("_");
			int q = locStr.lastIndexOf("_");
			
			if (p<0)
			{			
				loc = new Locale(locStr);
			}
			else if (q==p)
			{
				loc = new Locale(locStr.substring(0,p), locStr.substring(p+1));
			}
			else
			{
				loc = new Locale(locStr.substring(0,p), locStr.substring(p+1,q), locStr.substring(q+1));
			}
			
			for (Locale l : Locale.getAvailableLocales())
			{
				if (l.equals(loc))
				{
					return l;
				}
			}
			
			return null;
		}
	}
}
