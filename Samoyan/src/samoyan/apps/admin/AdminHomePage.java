package samoyan.apps.admin;

import samoyan.servlet.exc.RedirectException;

public class AdminHomePage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND;
	
	@Override
	public void init() throws Exception
	{
		throw new RedirectException(SystemOverviewPage.COMMAND, null);
	}
}
