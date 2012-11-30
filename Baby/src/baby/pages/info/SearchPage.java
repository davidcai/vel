package baby.pages.info;

import samoyan.core.Util;
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
		writeFormOpen("GET", null);
		writeTextInput("q", null, 60, 256);
		write(" ");
		writeButton(getString("controls:Button.Search"));
		writeFormClose();
		write("<br><br>");
		
		String q = getParameterString("q");
		if (!Util.isEmpty(q))
		{
			// !$! Render results
			write("Not yet implemented");
		}
	}
}
