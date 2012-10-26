package samoyan.apps.system;

import java.util.List;
import java.util.UUID;

import samoyan.database.Country;
import samoyan.database.CountryStore;

public class CountryTypeAhead extends TypeAhead
{
	public final static String COMMAND = "country.typeahead";

	@Override
	protected void doQuery(String q) throws Exception
	{
		List<UUID> countryIDs = CountryStore.getInstance().searchByName(q, getLocale());
		for (UUID countryID : countryIDs)
		{
			Country country = CountryStore.getInstance().load(countryID);
			if (country!=null && country.getCodeISO2()!=null)
			{
				addOption(country.getCodeISO2(), country.getName(getLocale()));
			}
		}
	}
}
