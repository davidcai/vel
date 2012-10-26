package elert.pages.physician;

import samoyan.servlet.exc.RedirectException;
import elert.pages.ElertPage;

public class PhysicianHomePage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_PHYSICIAN;
	
	@Override
	public void renderHTML() throws Exception
	{
		throw new RedirectException(UpcomingPatientsPage.COMMAND, null);
	}
}
