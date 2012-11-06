package samoyan.apps.messaging;

import samoyan.servlet.Dispatcher;

public class MessagingApp
{
	public static void init()
	{
		Dispatcher.bindPage(MessagingHomePage.COMMAND,	MessagingHomePage.class);
		Dispatcher.bindPage(ComposePage.COMMAND,		ComposePage.class);
		Dispatcher.bindPage(InboxPage.COMMAND,			InboxPage.class);
		Dispatcher.bindPage(OutboxPage.COMMAND,			OutboxPage.class);
		Dispatcher.bindPage(ReadMessagePage.COMMAND,	ReadMessagePage.class);
		Dispatcher.bindPage(UserTypeAhead.COMMAND,		UserTypeAhead.class);
		
		Dispatcher.bindPage(YouGotMailNotif.COMMAND,	YouGotMailNotif.class);
	}
}
