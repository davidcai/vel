package elert.pages.govern;

import samoyan.servlet.exc.RedirectException;
import elert.pages.ElertPage;


public final class GovernHomePage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_GOVERN;

	@Override
	public void renderHTML() throws Exception
	{
		throw new RedirectException(OpeningsReportPage.COMMAND, null);
	}
}
