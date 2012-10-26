package elert.pages.schedule;

import elert.pages.ElertPage;
import samoyan.servlet.exc.RedirectException;

public class ScheduleHomePage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_SCHEDULE;
	
	@Override
	public void renderHTML() throws Exception
	{
		throw new RedirectException(UnresolvedOpeningsPage.COMMAND, null);
	}
}
