package elert.pages.master;

import samoyan.servlet.Dispatcher;

public class MasterApp
{
	public static void init()
	{
		Dispatcher.bindPage(RootPage.COMMAND,				RootPage.class);
		Dispatcher.bindPage(LoginPage.COMMAND,				LoginPage.class);
		Dispatcher.bindPage(LessStylesheetPage.COMMAND,		LessStylesheetPage.class);
		Dispatcher.bindPage(TermsOfUsePage.COMMAND,			TermsOfUsePage.class);
		Dispatcher.bindPage(PrivacyPolicyPage.COMMAND,		PrivacyPolicyPage.class);
		Dispatcher.bindPage(WelcomePage.COMMAND,			WelcomePage.class);
		Dispatcher.bindPage(PasswordResetExtraPage.COMMAND,	PasswordResetExtraPage.class);
		Dispatcher.bindPage(HelpPage.COMMAND,				HelpPage.class);
	}	
}
