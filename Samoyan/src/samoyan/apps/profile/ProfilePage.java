package samoyan.apps.profile;

import samoyan.servlet.WebPage;

public abstract class ProfilePage extends WebPage
{
	public final static String COMMAND = "profile";

	@Override
	public boolean isAuthorized() throws Exception
	{
		return getContext().getUserID()!=null;
	}
}
