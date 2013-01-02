package baby.pages.content;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import baby.database.ArticleStore;
import baby.pages.BabyPage;

import samoyan.apps.system.TypeAhead;

public class RegionTypeAhead extends TypeAhead
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/regions.typeahed";
	
	@Override
	protected void doQuery(String q) throws SQLException, Exception
	{
		List<String> regions = ArticleStore.getInstance().getRegions();
		for (String r : regions)
		{
			if (r.toLowerCase(Locale.US).indexOf(q.toLowerCase(Locale.US))>=0)
			{
				addOption(r);
			}
		}
	}
}
