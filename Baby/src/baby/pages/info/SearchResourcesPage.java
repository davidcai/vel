package baby.pages.info;

import java.util.List;
import java.util.UUID;

import samoyan.core.Util;
import baby.app.BabyConsts;
import baby.controls.ArticleListControl;
import baby.database.ArticleStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;

public final class SearchResourcesPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/search-resources";
	
	public final static String PARAM_QUERY = "q";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:SearchRes.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		boolean phone = getContext().getUserAgent().isSmartPhone();
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		
		writeFormOpen("GET", null);
		writeTextInput(PARAM_QUERY, null, phone?30:60, 128);
		write(" ");
		writeButton(getString("controls:Button.Search"));
		writeFormClose();
		
		String q = getParameterString(PARAM_QUERY);
		if (!Util.isEmpty(q))
		{
			List<UUID> articleIDs = ArticleStore.getInstance().searchByText(q, BabyConsts.SECTION_RESOURCE, mother.getRegion());
			
			write("<br>");
			if (articleIDs.size()>0)
			{
				writeEncode(getString("information:SearchRes.ResultsFound", articleIDs.size(), mother.getRegion()));
			}
			else
			{
				writeEncode(getString("information:SearchRes.NoResultsFound", mother.getRegion()));
			}
			write("<br><br>");
			
			new ArticleListControl(this, articleIDs).showSummary(!phone).render();
		}
	}
}
