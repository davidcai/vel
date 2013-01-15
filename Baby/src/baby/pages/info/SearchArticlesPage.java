package baby.pages.info;

import java.util.List;
import java.util.UUID;

import samoyan.core.Util;
import baby.app.BabyConsts;
import baby.controls.ArticleListControl;
import baby.database.ArticleStore;
import baby.pages.BabyPage;

public class SearchArticlesPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/search-articles";
	
	public final static String PARAM_QUERY = "q";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:SearchArticles.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		boolean phone = getContext().getUserAgent().isSmartPhone();
		
		writeFormOpen("GET", null);
		writeTextInput(PARAM_QUERY, null, phone?30:60, 128);
		write(" ");
		writeButton(getString("controls:Button.Search"));
		writeFormClose();
		
		String q = getParameterString(PARAM_QUERY);
		if (!Util.isEmpty(q))
		{
			List<UUID> articleIDs = ArticleStore.getInstance().searchByText(q, BabyConsts.SECTION_INFO, null);
			
			write("<br>");
			if (articleIDs.size()>0)
			{
				writeEncode(getString("information:SearchArticles.ResultsFound", articleIDs.size()));
			}
			else
			{
				writeEncode(getString("information:SearchArticles.NoResultsFound"));
			}
			write("<br><br>");
			
			new ArticleListControl(this, articleIDs).showSummary(!phone).render();
		}
	}
}
