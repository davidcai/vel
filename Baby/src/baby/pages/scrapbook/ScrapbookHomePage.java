package baby.pages.scrapbook;

import baby.pages.BabyPage;
import samoyan.servlet.exc.RedirectException;

public class ScrapbookHomePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_SCRAPBOOK;
	
	@Override
	public void init() throws Exception
	{
		throw new RedirectException(JournalPage.COMMAND, null);
	}
}
