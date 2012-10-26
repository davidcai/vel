package baby.pages.profile;

import samoyan.apps.profile.ProfilePage;
import samoyan.servlet.exc.RedirectException;
import baby.pages.BabyPage;

public class BabyProfileHomePage extends BabyPage
{
	public final static String COMMAND = ProfilePage.COMMAND;

	@Override
	public void renderHTML() throws Exception
	{
		throw new RedirectException(ConsolidatedProfilePage.COMMAND, null);
	}
}
