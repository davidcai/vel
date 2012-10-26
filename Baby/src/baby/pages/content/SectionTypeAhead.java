package baby.pages.content;

import java.sql.SQLException;
import java.util.List;

import baby.database.ArticleStore;
import baby.pages.BabyPage;

import samoyan.apps.system.TypeAhead;

public final class SectionTypeAhead extends TypeAhead
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/section.typeahead";
	
	@Override
	protected void doQuery(String q) throws SQLException, Exception
	{
		List<String> sections = ArticleStore.getInstance().getSections();
		for (String section : sections)
		{
			if (section.toLowerCase(getLocale()).indexOf(q.toLowerCase(getLocale()))>=0)
			{
				addOption(section);
			}
		}
	}
}
