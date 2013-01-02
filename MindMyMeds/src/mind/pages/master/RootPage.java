package mind.pages.master;

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
		
//		if (ctx.getParameter("test")!=null)
//		{
//			Email email = new Email();
////			email.setRecipient("8324746200@txt.att.net", "Brent DeKay");
//			email.setRecipient("brianywillis@gmail.com", "Brian Y Willis");
//			email.setSender("notifications@veloxicom.com", "Veloxicom notifier");
//			email.setReplyTo("notifications+123456@veloxicom.com", "");
//			email.setSubject("Testing embedded CSS");
//			email.setContent("text/html", "<html><head><style type='text/css'>#zRED{color:red;}</style></head><body>This is <span id=zRED>Running</span> another <span style='color:blue;'>test</span>. www.kp.org/testlink . RSVP.</body></html>");
//			EmailServer.send(email);
//		}
			
		String appTitle = Setup.getAppTitle(getLocale());
		String appOwner = Setup.getAppOwner(getLocale());
		write("<div align=center>");
		writeImage("mind/logo.png", appTitle);
		write("</div>");
		
		write(Util.textToHtml(getString("mind:Root.Spiel", appTitle, appOwner)));
		write("<br>");
		if (ctx.getUserID()==null)
		{
			write("<table align=center><tr>");
			if (fed.isOpenRegistration())
			{
				write("<td align=center>");
					writeEncode(getString("mind:Root.RegisterHelp", appTitle));
					write("<br><br>");
					writeFormOpen("GET", JoinPage.COMMAND);
					writeButton(getString("mind:Root.Register"));
					writeFormClose();
				write("</td>");
				write("<td>&nbsp;&nbsp;&nbsp;&nbsp;</td>");
			}
			write("<td align=center>");
				writeEncode(getString("mind:Root.LoginHelp"));
				write("<br><br>");
				writeFormOpen("GET", LoginPage.COMMAND);
				writeButton(getString("mind:Root.Login"));
				writeFormClose();
			write("</tr></table>");
		}
		write("<br><br>");
		write("<div align=right>");
		writeImage("mind/kp-thrive-logo.png", null);
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
