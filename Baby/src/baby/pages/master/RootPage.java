package baby.pages.master;

import baby.pages.info.InformationHomePage;
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
		if (user!=null && phone)
		{
			// On smart phone, we redirect to the InformationHomePage if the user is logged in
			// because we don't have the master tab.
			throw new RedirectException(InformationHomePage.COMMAND, null);
		}
		
		if (!phone)
		{
			write("<table id=colored><tr><td>");
			
			write("<div id=spiel>");
			write("<h2>");
			writeEncode(getString("baby:Root.Welcome", appTitle, appOwner));
			write("</h2>");
			write(Util.textToHtml(getString("baby:Root.Spiel", appTitle, appOwner)));
			write("</div><br>");
			
			// Login
			if (user==null)
			{
				write("<div id=loginframe>");
				new LoginControl(this).showPrompt(false).render();
				write("</div><br>");
			}
			
			// Signup
			if (user==null && fed.isOpenRegistration())
			{
				writeEncode(getString("baby:Root.RegisterHelp"));
				write(" ");
				writeLink(getString("baby:Root.Register"), getPageURL(JoinPage.COMMAND));
			}
			
			write("</td><td>");
			
			writeImage("baby/decor.jpg", Setup.getAppTitle(getLocale()));
			
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
		}
		else
		{
			write("<div id=colored>");
			
				write("<div id=spiel>");
				write("<h2>");
				writeEncode(getString("baby:Root.Welcome", appTitle, appOwner));
				write("</h2>");
				write(Util.textToHtml(getString("baby:Root.Spiel", appTitle, appOwner)));
				write("</div>");
			
			write("</div>");

			// Login
			if (user==null)
			{
				write("<div id=loginframe>");
				new LoginControl(this).showPrompt(false).render();
				write("</div>");
			}
			
			// Signup
			if (user==null && fed.isOpenRegistration())
			{
				write("<div id=registerframe>");
				writeEncode(getString("baby:Root.RegisterHelp"));
				write(" <b>");
				writeLink(getString("baby:Root.Register"), getPageURL(JoinPage.COMMAND));
				write("</b>");
				write("</div>");
			}
			
			new ImageControl(this).resource("baby/decor.jpg").setAttribute("width", "100%").setAttribute("height", "").render();
			write("<br>");
			
			write("<div id=benefits>");
			for (int i=1; i<=3; i++)
			{
				write("<big>");
				writeEncode(getString("baby:Root.BenefitTitle." + i));
				write("</big><br>");
				writeEncode(getString("baby:Root.Benefit." + i, appTitle, appOwner));
				write("<br><br>");
			}
			write("</div>");
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
		// iOS has problems posting from HTTP to HTTPS when running as app,
		// so we use HTTPS when the login form is rendered.
		return getContext().getUserAgent().isIOS() && getContext().getUserID()==null;
	}
}
