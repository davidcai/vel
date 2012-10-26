package baby.pages.master;

import samoyan.apps.master.JoinPage;
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
		Server fed = ServerStore.getInstance().loadFederation();
		
		if (ctx.getParameter("test")!=null)
		{
		}
				
		
		String appTitle = Setup.getAppTitle(getLocale());
		String appOwner = Setup.getAppOwner(getLocale());
		write("<div align=center>");
		writeImage("baby/logo.png", appTitle);
		write("</div>");
		
		write(Util.textToHtml(getString("baby:Root.Spiel", appTitle, appOwner)));
		write("<br><br>");
		if (ctx.getUserID()==null)
		{
			write("<table align=center><tr>");
			if (fed.isOpenRegistration())
			{
				write("<td align=center>");
					writeEncode(getString("baby:Root.RegisterHelp", appTitle));
					write("<br><br>");
					writeFormOpen("GET", JoinPage.COMMAND);
					writeButton(getString("baby:Root.Register"));
					writeFormClose();
				write("</td>");
				write("<td>&nbsp;&nbsp;&nbsp;&nbsp;</td>");
			}
			write("<td align=center>");
				writeEncode(getString("baby:Root.LoginHelp"));
				write("<br><br>");
				writeFormOpen("GET", LoginPage.COMMAND);
				writeButton(getString("baby:Root.Login"));
				writeFormClose();
			write("</tr></table>");
		}
		write("<br><br>");
		write("<div align=right>");
		writeImage("baby/kp-thrive-logo.png", null);
		write("</div>");
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
