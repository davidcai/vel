package samoyan.apps.profile;

import samoyan.servlet.Dispatcher;

public final class ProfileApp
{
	public static void init()
	{
		Dispatcher.bindPage(ProfileHomePage.COMMAND,		ProfileHomePage.class);

		// Login
		Dispatcher.bindPage(ChangePasswordPage.COMMAND,		ChangePasswordPage.class);
		Dispatcher.bindPage(ChangeLoginNamePage.COMMAND,	ChangeLoginNamePage.class);
		Dispatcher.bindPage(CloseAccountPage.COMMAND,		CloseAccountPage.class);
		Dispatcher.bindPage(PasswordChangedNotif.COMMAND,	PasswordChangedNotif.class);
		Dispatcher.bindPage(LoginNameChangedNotif.COMMAND,	LoginNameChangedNotif.class);
		
		// Settings
		Dispatcher.bindPage(PersonalInfoPage.COMMAND,		PersonalInfoPage.class);
		Dispatcher.bindPage(ContactInfoPage.COMMAND,		ContactInfoPage.class);
		Dispatcher.bindPage(MobilePage.COMMAND,				MobilePage.class);
		Dispatcher.bindPage(PhonePage.COMMAND,				PhonePage.class);
		Dispatcher.bindPage(EmailPage.COMMAND,				EmailPage.class);
		Dispatcher.bindPage(TwitterPage.COMMAND,			TwitterPage.class);
		Dispatcher.bindPage(AlertTimelinePage.COMMAND,		AlertTimelinePage.class);
		
		// Other
		Dispatcher.bindPage(TimeZonePage.COMMAND,			TimeZonePage.class);
		Dispatcher.bindPage(RealNamePage.COMMAND,			RealNamePage.class);
	}
}
