package baby.pages.master;

import samoyan.apps.master.JoinPage;
import samoyan.controls.BigCalendarControl;
import samoyan.controls.LoginControl;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.WebPage;

public class RootPage extends WebPage
{
	public final static String COMMAND = "";
	
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		
		if (ctx.getUserID()==null)
		{
			renderLoginScreen();
		}
		else
		{
			renderPortal();
		}
	}

	private void renderPortal() throws Exception
	{
		// !$! TBI
		new BigCalendarControl(this).render();
	}
	
	private void renderLoginScreen() throws Exception
	{
		RequestContext ctx = getContext();
		boolean phone = ctx.getUserAgent().isSmartPhone();
		String appTitle = Setup.getAppTitle(getLocale());
		String appOwner = Setup.getAppOwner(getLocale());
		Server fed = ServerStore.getInstance().loadFederation();
		
		if (!phone)
		{
			write("<table><tr><td width=\"67%\" id=spiel>");
			
			write("<h2>");
			writeEncode(getString("baby:Root.Welcome", appTitle, appOwner));
			write("</h2>");
			write(Util.textToHtml(getString("baby:Root.Spiel", appTitle, appOwner)));
			if (fed.isOpenRegistration())
			{
				write("<br><br>");
				writeFormOpen("GET", JoinPage.COMMAND);
				writeButton(getString("baby:Root.Register"));
				writeFormClose();
			}
			
			write("</td><td width=\"33%\">");
			
			write("<div id=loginframe>");
			new LoginControl(this).showPrompt(false).render();
			write("</div>");
			
			write("</td></tr></table>");
			write("<br>");
			
			write("<table id=benefits><tr>");
			for (int i=1; i<=3; i++)
			{
				write("<td width=\"33%\">");
				write("<big>");
				writeEncode(getString("baby:Root.BenefitTitle." + i));
				write("</big><br>");
				writeEncode(getString("baby:Root.Benefit." + i, appTitle, appOwner));
				write("</td>");
			}
			write("</tr></table>");
			
			// Background image on #middle
			write("<style>#middle {background-image: url(\"");
			write(getResourceURL("baby/babies-background.jpg"));
			write("\"); background-position: top center; background-repeat: no-repeat;</style>");
		}
		else
		{
			write("<h2>");
			writeEncode(getString("baby:Root.Welcome", appTitle, appOwner));
			write("</h2>");
			write(Util.textToHtml(getString("baby:Root.Spiel", appTitle, appOwner)));

			write("<div id=loginframe>");
			new LoginControl(this).render();
			write("</div>");
			
			for (int i=1; i<=3; i++)
			{
				write("<big>");
				writeEncode(getString("baby:Root.BenefitTitle." + i));
				write("</big><br>");
				writeEncode(getString("baby:Root.Benefit." + i, appTitle, appOwner));
				write("<br><br>");
			}
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return Setup.getAppTitle(getLocale());
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return getContext().isSecureSocket();
	}
}
