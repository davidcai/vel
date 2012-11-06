package samoyan.apps.messaging;

import samoyan.servlet.exc.RedirectException;

public class MessagingHomePage extends MessagingPage
{
	public final static String COMMAND = MessagingPage.COMMAND; 
			
	@Override
	public void renderHTML() throws Exception
	{
		throw new RedirectException(InboxPage.COMMAND, null);
	}
}
