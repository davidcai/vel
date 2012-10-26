package elert.pages.master;

import samoyan.apps.master.JoinPage;
import samoyan.apps.master.PasswordResetPage;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
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
		boolean phone = ctx.getUserAgent().isSmartPhone();
				
if (isParameter("test"))
{
//	UserEx ux = UserExStore.getInstance().openByUserID(ctx.getUserID());
//	if (ux!=null)
//	{
//		JaiImage jai = new JaiImage(new File("C:\\dev\\" + ctx.getParameter("test") + ".jpg"));
//		ux.setImage(new ImageEx(jai));
//		UserExStore.getInstance().save(ux);
//		
//		ux = UserExStore.getInstance().loadByUserID(ctx.getUserID());
//		ImageEx img = ux.getImage();
//		jai = new JaiImage(img.getBytes());
//		jai.toFile(new File("C:\\dev\\" + ctx.getParameter("test") + "_OUT.jpg"), 75);
//	}
	
//	User user = UserStore.getInstance().open(ctx.getUserID());
//	if (user!=null && !user.isGuidedSetup())
//	{
//		List<String> pages = new ArrayList<String>();
//		pages.add(PersonalInfoPage.COMMAND);
//		pages.add(MobilePage.COMMAND);
//		pages.add(PhonePage.COMMAND);
//		pages.add(ConsentFormPage.COMMAND);
//		user.setGuidedSetupPages(pages);
//		user.setGuidedSetupStep(-1);
//		UserStore.getInstance().save(user);
//	}
	
//	EmailMessage msg = new EmailMessage();
////	msg.setRecipient("7605591681@vzwpix.com", "Melissa");
//	msg.setRecipient("brian.willis@veloxicom.com", "BW");
//	msg.setContent("text/plain", "Brian here again with another test. Pls respond with anything.");
//	msg.setReplyTo("my.elert+1@veloxicom.com", "My eLert");
//	msg.setSender("notifications+3@veloxicom.com", "My eLert");
//	EmailServer.send(msg);
}		
		
		write("<table class=ShadedArea><tr>");
		
		// Elevator pitch
		write("<td class=Pitch");
		if (!phone)
		{
			write(" width=\"67%\"");
		}
		write(">");
		renderElevatorPitch();
		write("</td>");
		
		if (phone)
		{
			write("</tr><tr>");
		}
		
		// Login box
		write("<td align=center");
		if (!phone)
		{
			write(" width=\"33%\"");
		}
		write(">");
		if (ctx.getUserID()==null)
		{
			renderLoginBox();
		}
		else
		{
			writeImage("elert/logo.png", Setup.getAppTitle(getLocale()));
		}
		write("</td>");
		
		write("</tr></table>");
		
		// Triage
		if (!phone)
		{
			write("<table><tr valign=top>");
		}
		for (int i=1; i<=3; i++)
		{
			if (!phone)
			{
				write("<td width=\"33%\">");
			}
			write("<h3>");
			writeEncode(getString("elert:Root.TriageTitle_" + i, Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale())));
			write("</h3>");
			writeEncode(getString("elert:Root.TriageText_" + i, Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale())));
			if (!phone)
			{
				write("</td>");
			}
			else
			{
				write("<br>");
			}
		}
		if (!phone)
		{
			write("</tr></table>");
		}
	}

	private void renderElevatorPitch() throws Exception
	{
		RequestContext ctx = getContext();

		write("<h1>");
		writeEncode(getString("elert:Root.ElevatorPitch", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale())));
		write("</h1><big>");
		writeEncode(getString("elert:Root.ElevatorPitchSub", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale())));
		write("</big><br><br>");
		
		if (ctx.getUserID()==null)
		{
			Server fed = ServerStore.getInstance().loadFederation();
			if (fed.isOpenRegistration()==true)
			{
				writeFormOpen(true, "GET", JoinPage.COMMAND);
				writeButton(getString("elert:Root.SignUpBtn"));
				writeFormClose();
			}
		}
	}

	private void renderLoginBox()
	{
		write("<br>");
		
		writeFormOpen(true, "POST", LoginPage.COMMAND);
		write("<table class=LoginBox>");
		
		write("<tr><td colspan=2><b>");
		writeEncode(getString("elert:Root.MembersLogin"));
		write("</b><br><br></td></tr>");

		write("<tr><td>");
		writeEncode(getString("elert:Root.LoginName"));
		write("</td><td align=right>");
		writeTextInput(LoginPage.PARAM_LOGINNAME, null, 16, User.MAXSIZE_LOGINNAME);
		write("</td></tr>");

		write("<tr><td>");
		writeEncode(getString("elert:Root.Password"));
		write("</td><td align=right>");
		writePasswordInput(LoginPage.PARAM_PASSWORD, null, 16, User.MAXSIZE_PASSWORD);
		write("</td></tr>");

		write("<tr valign=middle><td><nobr><small>");
		writeLink(getString("elert:Root.ForgotPassword"), getPageURL(PasswordResetPage.COMMAND));
		write("</small></nobr></td><td align=right>");
		writeButton(getString("elert:Root.LoginBtn"));
		write("</td></tr>");

		write("<tr><td colspan=2>");
		writeCheckbox(LoginPage.PARAM_KEEP, getString("elert:Root.RememberMe"), false);
		write("</td></tr>");

		write("</table>");
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		String title = Setup.getAppTitle(getLocale());
		if (getContext().getUserAgent().isSmartPhone()==false)
		{
			title += " - " + Setup.getAppOwner(getLocale());
		}
		return title;
	}	
}
