package samoyan.apps.system;

import java.sql.SQLException;
import java.util.Locale;


public class LocaleTypeAhead extends TypeAhead
{
	public final static String COMMAND = "locale.typeahead";
	
	@Override
	protected void doQuery(String q) throws SQLException, Exception
	{
		q = q.toLowerCase(getLocale());
		
		for (Locale loc : Locale.getAvailableLocales())
		{
			if (loc.getDisplayName(getLocale()).toLowerCase(getLocale()).indexOf(q)>=0)
			{
				addOption(loc.toString(), loc.getDisplayName(getLocale()));
			}
		}
	}
}
