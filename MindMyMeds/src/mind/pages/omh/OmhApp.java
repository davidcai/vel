package mind.pages.omh;

import samoyan.servlet.Dispatcher;

public class OmhApp
{
	public static void init()
	{
		Dispatcher.bindPage(AuthenticatePage.COMMAND, AuthenticatePage.class);
		Dispatcher.bindPage(ReadPage.COMMAND, ReadPage.class);
	}
}
