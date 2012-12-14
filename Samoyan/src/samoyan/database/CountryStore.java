package samoyan.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import samoyan.core.Util;

public final class CountryStore extends CsvDataBeanStore<Country>
{
	private static CountryStore instance = new CountryStore();

	protected CountryStore()
	{
	}
	public final static CountryStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Country> getBeanClass()
	{
		return Country.class;
	}	
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("Countries");
		
		td.defineCol("Name", String.class).invariant();
		td.defineCol("ISO2", String.class).invariant();
		td.defineCol("ISO3", String.class).invariant();
		td.defineCol("PhonePrefix", String.class).invariant();
		
		return td;
	}

	// - - -
		
	public Country loadByCodeISO2(String iso2) throws Exception
	{
		return getInstance().loadByColumn("ISO2", iso2);
	}

	public List<String> getAllPhonePrefixes() throws Exception
	{
		List<String> result = new ArrayList<String>();

		List<UUID> all = getInstance().queryAll();
		for (UUID id : all)
		{
			Country c = load(id);
			if (!Util.isEmpty(c.getPhonePrefix()))
			{
				result.add(c.getPhonePrefix());
			}
		}
		
		// Sort
		class Sorter implements Comparator<String>
		{
			@Override
			public int compare(String o1, String o2)
			{
				return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
			}
		}
		Collections.sort(result, new Sorter());
		
		// Remove duplicates
		for (int i=result.size()-1; i>=1; i--)
		{
			if (result.get(i).equals(result.get(i-1)))
			{
				result.remove(i);
			}
		}
		
		return result;
	}
	
	/**
	 * Locates the countries in which this phone number may be listed based on its international prefix.
	 * Can result in multiple countries, for example, "14085551234" will return USA, Canada and Puerto Rico.
	 * @param phoneNumber
	 * @return
	 * @throws Exception 
	 */
	public List<UUID> queryByPhoneNumber(String phoneNumber) throws Exception
	{
		List<UUID> result = new ArrayList<UUID>();
		if (Util.isEmpty(phoneNumber)) return result;

		int longestMatch = 0;
		List<UUID> all = getInstance().queryAll();
		for (UUID id : all)
		{
			Country country = load(id);
			String prefix = country.getPhonePrefix();
			if (prefix!=null && phoneNumber.startsWith(prefix))
			{
				if (prefix.length()>longestMatch)
				{
					// Found better match
					result = new ArrayList<UUID>();
					result.add(id);
				}
				else if (prefix.length()==longestMatch)
				{
					// Found equal match
					result.add(id);
				}
				longestMatch = prefix.length();
			}
		}
		return result;
	}
	
	public String extractPhonePrefix(String phoneNumber) throws Exception
	{
		String result = "";
		if (Util.isEmpty(phoneNumber)) return result;

		List<UUID> all = getInstance().queryAll();
		for (UUID id : all)
		{
			Country country = load(id);
			String prefix = country.getPhonePrefix();
			if (prefix!=null && phoneNumber.startsWith(prefix))
			{
				if (prefix.length()>result.length())
				{
					// Found better match
					result = prefix;
				}
			}
		}
		return result;
	}
	
	public List<UUID> searchByName(String q, Locale loc) throws Exception
	{
		List<UUID> result = new ArrayList<UUID>(); 
		
		String lcQ = q.toLowerCase(loc);
		List<UUID> all = getInstance().queryAll();
		for (UUID id : all)
		{
			Country country = load(id);
			String lcName = country.getName(loc).toLowerCase(loc);
			if (q.length()<3)
			{
				if (lcName.indexOf(lcQ)>=0)
				{
					result.add(id);
				}
			}
			else
			{
				if (lcName.startsWith(lcQ))
				{
					result.add(id);
				}
			}
		}
		
		return result;
	}
}
