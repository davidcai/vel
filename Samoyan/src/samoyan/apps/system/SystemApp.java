package samoyan.apps.system;

import samoyan.servlet.Dispatcher;

public final class SystemApp
{
	public static void init()
	{
		Dispatcher.bindPage(CaptchaImagePage.COMMAND,			CaptchaImagePage.class);
		Dispatcher.bindPage(EmailBeaconPngPage.COMMAND,			EmailBeaconPngPage.class);
		Dispatcher.bindPage(SmsReceiptPage.COMMAND,				SmsReceiptPage.class);
		Dispatcher.bindPage(IncomingSMSPage.COMMAND,			IncomingSMSPage.class);
		Dispatcher.bindPage(CountriesTypeAhead.COMMAND,			CountriesTypeAhead.class);
		Dispatcher.bindPage(NotificationPage.COMMAND,			NotificationPage.class);
		Dispatcher.bindPage(GoBackPage.COMMAND,					GoBackPage.class);
		Dispatcher.bindPage(UnresponsiveVoiceCallPage.COMMAND,	UnresponsiveVoiceCallPage.class);
		
		// Typeahead
		Dispatcher.bindPage(TimeZoneTypeAhead.COMMAND,			TimeZoneTypeAhead.class);
		Dispatcher.bindPage(LocaleTypeAhead.COMMAND,			LocaleTypeAhead.class);
		Dispatcher.bindPage(CountryTypeAhead.COMMAND,			CountryTypeAhead.class);
	}
}
