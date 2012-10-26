package baby.pages.scrapbook;

import baby.pages.BabyPage;

public class ChartsPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_SCRAPBOOK + "/charts";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:Charts.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write("This is a test"); // !$!
	}
}
