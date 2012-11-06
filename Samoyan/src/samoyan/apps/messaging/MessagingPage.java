package samoyan.apps.messaging;

import samoyan.servlet.WebPage;

public abstract class MessagingPage extends WebPage
{
	public final static String COMMAND = "messaging";

	@Override
	public boolean isAuthorized() throws Exception
	{
		return getContext().getUserID()!=null;
	}
}
