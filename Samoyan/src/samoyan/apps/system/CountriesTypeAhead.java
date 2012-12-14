package samoyan.apps.system;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import samoyan.core.Util;
import samoyan.database.Country;
import samoyan.database.CountryStore;

public class CountriesTypeAhead extends TypeAhead
{
	public final static String COMMAND = "countries.typeahead";
	
	@Override
	protected void doQuery(String q) throws SQLException, Exception
	{
		q = q.toLowerCase(getLocale());
		
		List<UUID> countryIDs = CountryStore.getInstance().queryAll();
		for (UUID id : countryIDs)
		{
			Country country = CountryStore.getInstance().load(id);
			if (Util.isEmpty(country.getPhonePrefix()) || Util.isEmpty(country.getCodeISO2())) continue;
			
			// Perform search
			if (country.getName(getLocale()).toLowerCase(getLocale()).indexOf(q)>=0 ||
				(country.getPhonePrefix()).indexOf(q)>=0)
			{
				StringBuffer html = new StringBuffer();
				html.append("<table><tr valign=middle><td>");
				html.append("<img src='");
				html.append(getResourceURL("icons/flags/" + country.getCodeISO2().toLowerCase(Locale.US) + ".gif"));
				html.append("'>");
				html.append("</td><td>");
				html.append(Util.htmlEncode(country.getName(getLocale())));
				html.append("</td></tr></table>");				
				
				addOption(country.getCodeISO2(), country.getName(getLocale()), html.toString());
			}
		}		
	}
}
