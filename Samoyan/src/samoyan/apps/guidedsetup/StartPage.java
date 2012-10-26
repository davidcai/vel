package samoyan.apps.guidedsetup;

import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Setup;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;

public class StartPage extends WebPage
{
	public final static String COMMAND = UrlGenerator.COMMAND_SETUP;
	
	@Override
	public void init() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		if (user==null || user.isGuidedSetup()==false)
		{
			throw new PageNotFoundException();
		}
		
		int step = user.getGuidedSetupStep();
		if (step>=0)
		{
			throw new RedirectException(UrlGenerator.COMMAND_SETUP + "/" + user.getGuidedSetupPages().get(step), null);
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("guidedsetup:Start.Title", Setup.getAppTitle(getLocale()));
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		writeFormOpen();
		
		write(Util.textToHtml(getString("guidedsetup:Start.Intro", Setup.getAppTitle(getLocale()))));
		write("<br><br>");
		writeButton(getString("guidedsetup:Start.GetStarted"));
		
		writeFormClose();
	}

	@Override
	public void commit() throws Exception
	{
		progressGuidedSetup();
	}
}
