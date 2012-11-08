package samoyan.database;

import java.util.Comparator;
import java.util.Locale;

import samoyan.core.SortStringsAlphabetically;

public final class MobileCarrier extends DataBean
{
	public final static int MAXSIZE_NAME = 64;
	
	public static class SortByName implements Comparator<MobileCarrier>
	{
		private SortStringsAlphabetically alphaSorter;
		
		public SortByName(Locale locale)
		{
			alphaSorter = new SortStringsAlphabetically(locale);
		}

		@Override
		public int compare(MobileCarrier o1, MobileCarrier o2)
		{
			return alphaSorter.compare(o1.getName(), o2.getName());
		}		
	}
		
	public static class SortByMinor implements Comparator<MobileCarrier>
	{
		private SortStringsAlphabetically alphaSorter;
		
		public SortByMinor(Locale locale)
		{
			alphaSorter = new SortStringsAlphabetically(locale);
		}

		@Override
		public int compare(MobileCarrier o1, MobileCarrier o2)
		{
			if (o1.isMinor()!=o2.isMinor())
			{
				return o1.isMinor()? 1 : -1;
			}
			else
			{
				return alphaSorter.compare(o1.getName(), o2.getName());
			}
		}		
	}
		
	public String getName()
	{
		return (String) get("Name");
	}
	
	public String getSMSEmail()
	{
		return (String) get("SMSEmail");
	}
	
	public String getMMSEmail()
	{
		return (String) get("MMSEmail", getSMSEmail());
	}
	
	public String getCountryCode()
	{
		return (String) get("CountryCode");
	}
	
	/**
	 * Indicates if this carrier is not one of the major carriers in its country.
	 * @return
	 */
	public boolean isMinor()
	{
		return (Boolean) get("Minor", false);
	}
}
