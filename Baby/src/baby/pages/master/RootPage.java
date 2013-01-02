package baby.pages.master;

import samoyan.apps.master.JoinPage;
import samoyan.controls.ImageControl;
import samoyan.controls.LoginControl;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;

public class RootPage extends WebPage
{
	public final static String COMMAND = "";
	
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		boolean phone = ctx.getUserAgent().isSmartPhone();
		String appTitle = Setup.getAppTitle(getLocale());
		String appOwner = Setup.getAppOwner(getLocale());
		Server fed = ServerStore.getInstance().loadFederation();
				
		if (user!=null && user.isGuidedSetup())
		{
			// Redirect to GuidedSetup
			throw new RedirectException(UrlGenerator.COMMAND_SETUP, null);
		}
		
		if (!phone)
		{
			write("<table><tr><td width=\"67%\" id=spiel>");
			
			write("<h2>");
			writeEncode(getString("baby:Root.Welcome", appTitle, appOwner));
			write("</h2>");
			write(Util.textToHtml(getString("baby:Root.Spiel", appTitle, appOwner)));
			if (user==null && fed.isOpenRegistration())
			{
				write("<br><br>");
				writeFormOpen("GET", JoinPage.COMMAND);
				writeButton(getString("baby:Root.Register"));
				writeFormClose();
			}
			
			write("</td>");
			
			if (user==null)
			{
				write("<td width=\"33%\">");
				write("<div id=loginframe>");
				new LoginControl(this).showPrompt(false).render();
				write("</div>");
				write("</td>");
			}
			
			write("</tr></table>");
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
		}
		else
		{
			write("<h2>");
			writeEncode(getString("baby:Root.Welcome", appTitle, appOwner));
			write("</h2>");
			write(Util.textToHtml(getString("baby:Root.Spiel", appTitle, appOwner)));

			if (user==null)
			{
				write("<div id=loginframe>");
				new LoginControl(this).render();
				write("</div>");
			}
			else
			{
				write("<div align=center id=loginframe>");
				new ImageControl(this).resource("baby/logo.png").render();
				write("</div>");
			}
			
			for (int i=1; i<=3; i++)
			{
				write("<big>");
				writeEncode(getString("baby:Root.BenefitTitle." + i));
				write("</big><br>");
				writeEncode(getString("baby:Root.Benefit." + i, appTitle, appOwner));
				write("<br><br>");
			}
		}
		
		// Custom CSS
		write("<style>");
			if (!phone)
			{
				// Background image on #middle
				write("#middle{background-image:url(\"");
				writeEncode(getResourceURL("baby/babies-background.jpg"));
				write("\");background-position:top center;background-repeat:no-repeat;}");
			}
			if (ctx.getUserID()==null)
			{
				// Hide navbar when not logged in
				write("#navbar{display:none;}");
			}
		write("</style>");
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return Setup.getAppTitle(getLocale());
	}

//	@Override
//	public boolean isSecureSocket() throws Exception
//	{
//		return getContext().isSecureSocket();
//	}
}
