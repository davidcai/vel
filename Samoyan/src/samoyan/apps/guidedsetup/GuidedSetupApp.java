package samoyan.apps.guidedsetup;

import samoyan.servlet.Dispatcher;

public class GuidedSetupApp
{
	public static void init()
	{
		Dispatcher.bindPage(StartPage.COMMAND,				StartPage.class);
		Dispatcher.bindPage(CompletePage.COMMAND,			CompletePage.class);
	}
}
