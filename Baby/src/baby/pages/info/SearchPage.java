package baby.pages.info;

import java.util.List;
import java.util.UUID;

import samoyan.core.Util;
import baby.controls.ArticleListControl;
import baby.database.ArticleStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;

public final class SearchPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/search";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:Search.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		boolean phone = getContext().getUserAgent().isSmartPhone();
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		
		writeFormOpen("GET", null);
		writeTextInput("q", null, phone?30:60, 128);
		write(" ");
		writeButton(getString("controls:Button.Search"));
		writeFormClose();
		
		String q = getParameterString("q");
		if (!Util.isEmpty(q))
		{
			List<UUID> articleIDs = ArticleStore.getInstance().searchByText(q, mother.getRegion());
			writeEncode(getString("information:Search.ResultsFound", articleIDs.size()));
			write("<br><br>");
			
			new ArticleListControl(this, articleIDs).showSummary(!phone).render();
		}
	}
}
