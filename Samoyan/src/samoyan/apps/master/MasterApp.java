package samoyan.apps.master;

import samoyan.servlet.Dispatcher;

public final class MasterApp
{
	public static void init()
	{
		// Login
		Dispatcher.bindPage(LoginPage.COMMAND,					LoginPage.class);
		Dispatcher.bindPage(LogoutPage.COMMAND,					LogoutPage.class);
		Dispatcher.bindPage(JoinPage.COMMAND,					JoinPage.class);
		Dispatcher.bindPage(WelcomePage.COMMAND,				WelcomePage.class);
		Dispatcher.bindPage(GoodbyePage.COMMAND,				GoodbyePage.class);
		Dispatcher.bindPage(PasswordResetPage.COMMAND,			PasswordResetPage.class);
		Dispatcher.bindPage(PasswordResetNotif.COMMAND,			PasswordResetNotif.class);
		Dispatcher.bindPage(InitPasswordPage.COMMAND,			InitPasswordPage.class);
		
		Dispatcher.bindPage(RootPage.COMMAND,					RootPage.class);
		Dispatcher.bindPage(TermsPage.COMMAND,					TermsPage.class);
		Dispatcher.bindPage(PrivacyPage.COMMAND,				PrivacyPage.class);
		Dispatcher.bindPage(HelpPage.COMMAND,					HelpPage.class);

		Dispatcher.bindPage(OverrideUserAgentPage.COMMAND,		OverrideUserAgentPage.class);
		
		Dispatcher.bindPage(LessStylesheetPage.COMMAND,			LessStylesheetPage.class);
		
		// Notifs
		Dispatcher.bindPage(JoinNotif.COMMAND,				JoinNotif.class);
	}
}
