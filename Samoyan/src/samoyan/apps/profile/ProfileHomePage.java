package samoyan.apps.profile;

import samoyan.servlet.exc.RedirectException;

public class ProfileHomePage extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND;

	@Override
	public void renderHTML() throws Exception
	{
		throw new RedirectException(PersonalInfoPage.COMMAND, null);
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return getContext().isSecureSocket();
	}
}
