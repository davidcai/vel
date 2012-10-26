package samoyan.apps.guidedsetup;

import samoyan.apps.master.WelcomePage;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;

public class CompletePage extends WebPage
{
	public final static String COMMAND = UrlGenerator.COMMAND_SETUP + "/complete";

	@Override
	public void init() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		if (user.isGuidedSetup()==false)
		{
			throw new PageNotFoundException();
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("guidedsetup:Complete.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		writeFormOpen();
		
		write(Util.textToHtml(getString("guidedsetup:Complete.Intro")));
		write("<br><br>");
		writeButton(getString("guidedsetup:Complete.Finish"));
		
		writeFormClose();
	}

	@Override
	public void commit() throws Exception
	{
		// Disable guided setup for this user
		User user = UserStore.getInstance().open(getContext().getUserID());
		user.setGuidedSetupStep(Integer.MAX_VALUE);
		UserStore.getInstance().save(user);
		
		// Redirect to standard welcome page
		throw new RedirectException(WelcomePage.COMMAND, null);
	}
}
