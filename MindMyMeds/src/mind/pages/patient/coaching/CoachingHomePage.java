package mind.pages.patient.coaching;

import samoyan.servlet.exc.RedirectException;

public class CoachingHomePage extends CoachingPage
{
	public final static String COMMAND = CoachingPage.COMMAND;
	
	@Override
	public void init() throws Exception
	{
		throw new RedirectException(BotChatPage.COMMAND, null);
	}
}
