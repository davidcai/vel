package samoyan.database;

import java.util.Comparator;
import java.util.Locale;

import samoyan.core.SortStringsAlphabetically;

public final class Country extends DataBean
{
	public final static int MAXSIZE_NAME = 64;

	public final static String UNITED_STATES = "US";
	public final static String CANADA = "CA";
	public final static String AUSTRALIA = "AU";
	public final static String UNITED_KINGDOM = "GB";
	public final static String GERMANY = "DE";
	public static final String ITALY = "IT";
	public static final String SPAIN = "ES";
	public static final String SOUTH_AFRICA = "ZA";
	
	public static class SortByName implements Comparator<Country>
	{
		private SortStringsAlphabetically alphaSorter;
		private Locale locale;
		
		public SortByName(Locale locale)
		{
			this.alphaSorter = new SortStringsAlphabetically(locale);
			this.locale = locale;
		}

		@Override
		public int compare(Country o1, Country o2)
		{
			return alphaSorter.compare(o1.getName(this.locale), o2.getName(this.locale));
		}		
	}
	
	public static class SortByPhonePrefix implements Comparator<Country>
	{
		@Override
		public int compare(Country o1, Country o2)
		{
			if (o1.getPhonePrefix()==null && o2.getPhonePrefix()==null)
			{
				return 0;
			}
			else if (o1.getPhonePrefix()==null)
			{
				return 1;
			}
			else if (o2.getPhonePrefix()==null)
			{
				return -1;
			}
			else
			{
				Integer p1 = Integer.parseInt(o1.getPhonePrefix());
				Integer p2 = Integer.parseInt(o2.getPhonePrefix());
				if (p1-p2==0)
				{
					if (o1.getCodeISO2()==null && o2.getCodeISO2()==null)
					{
						return 0;
					}
					else if (o1.getCodeISO2()==null)
					{
						return 1;
					}
					else if (o2.getCodeISO2()==null)
					{
						return -1;
					}
					else if (o1.getCodeISO2().equalsIgnoreCase(UNITED_STATES))
					{
						return -1;
					}
					else if (o2.getCodeISO2().equalsIgnoreCase(UNITED_STATES))
					{
						return 1;
					}
					else
					{
						return o1.getCodeISO2().compareTo(o2.getCodeISO2());
					}
				}
				else
				{
					return p1-p2;
				}
			}
		}		
	}

	/**
	 * The common name of the country.
	 * @return
	 */
	public String getName(Locale loc)
	{
		// Currently, only the English common name is in the CSV file.
		// !$! Need to return name based on locale.
		return (String) get("Name");
	}
	
	/**
	 * The two letter ISO code of the country, e.g. "US" for the United States.
	 * @return
	 */
	public String getCodeISO2()
	{
		return (String) get("ISO2");
	}
	
	/**
	 * The three letter ISO code of the country, e.g. "USA" for the United States.
	 * @return
	 */
	public String getCodeISO3()
	{
		return (String) get("ISO3");
	}
	
	/**
	 * The international dialing prefix of the country, e.g. "1" for the United States.
	 * @return
	 */
	public String getPhonePrefix()
	{
		return (String) get("PhonePrefix");
	}
}
