package baby.pages.journey;

import baby.pages.BabyPage;
import samoyan.servlet.exc.RedirectException;

public class JourneyHomePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY;
	
	@Override
	public void init() throws Exception
	{
		throw new RedirectException(JournalPage.COMMAND, null);
	}
}
