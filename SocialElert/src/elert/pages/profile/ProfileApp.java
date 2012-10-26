package elert.pages.profile;

import samoyan.servlet.Dispatcher;

public class ProfileApp
{
	public static void init()
	{
		Dispatcher.bindPage(PersonalExtraPage.COMMAND,		PersonalExtraPage.class);
	}
}
