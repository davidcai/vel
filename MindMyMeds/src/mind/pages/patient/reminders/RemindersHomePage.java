package mind.pages.patient.reminders;

import samoyan.servlet.exc.RedirectException;

public class RemindersHomePage extends RemindersPage
{
	public final static String COMMAND = RemindersPage.COMMAND;
	
	@Override
	public void init() throws Exception
	{
		throw new RedirectException(PrescriptionListPage.COMMAND, null);
	}
}
