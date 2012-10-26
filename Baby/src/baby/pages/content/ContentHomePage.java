package baby.pages.content;

import baby.pages.BabyPage;

public final class ContentHomePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT;
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("content:Home.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		writeEncode("To be implemented..."); // !$!
	}
}
