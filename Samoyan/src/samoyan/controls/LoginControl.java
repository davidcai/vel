package samoyan.controls;

import samoyan.apps.master.JoinPage;
import samoyan.apps.master.LoginPage;
import samoyan.apps.master.PasswordResetPage;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.servlet.WebPage;

public class LoginControl
{
	private WebPage out;
	private boolean prompt = true;
	private boolean remember = true;
	
	public LoginControl(WebPage outputPage)
	{
		this.out = outputPage;
	}
	
	public LoginControl showPrompt(boolean show)
	{
		this.prompt = show;
		return this;
	}
	
	public LoginControl showRememberMe(boolean show)
	{
		this.remember = show;
		return this;
	}

	public void render() throws Exception
	{
		out.writeFormOpen("POST", LoginPage.COMMAND);
				
		// Prompt
		if (this.prompt)
		{
			out.write("<div align=center>");
			
			out.writeEncode(out.getString("controls:Login.EnterCredentials"));
			
			// Register for access
			Server fed = ServerStore.getInstance().loadFederation();
			if (fed.isOpenRegistration())
			{
				out.write("<br><small>");
		
				StringBuffer regLink = new StringBuffer();
				regLink.append("<a href=\"");
				regLink.append(out.getPageURL(JoinPage.COMMAND));
				regLink.append("\">");
				regLink.append(Util.htmlEncode(out.getString("controls:Login.Register")));
				regLink.append("</a>");
				
				String pattern = Util.htmlEncode(out.getString("controls:Login.NewMember", "$link$"));
				pattern = Util.strReplace(pattern, "$link$", regLink.toString());
				out.write(pattern);
		
				out.write("</small>");
			}
			
			out.write("</div><br>");
		}
		
		out.write("<table align=center><tr valign=middle><td align=left>"); // Inner
		out.writeEncode(out.getString("controls:Login.LoginName"));
		out.write("</td><td align=right>");
		out.writeTextInput(LoginPage.PARAM_LOGINNAME, null, 20, User.MAXSIZE_LOGINNAME);
		out.write("</td></tr><tr valign=middle><td align=left>");
		out.writeEncode(out.getString("controls:Login.Password"));
		out.write("</td><td align=right>");
		out.writePasswordInput(LoginPage.PARAM_PASSWORD, null, 20, User.MAXSIZE_PASSWORD);
		out.write("</td></tr>");
		
		out.write("<tr><td colspan=2>"); // Inner
		
		out.write("<table width=\"100%\"><tr valign=middle><td align=left>"); // Button table
		
			// Forgot your password?
			out.write("<small><a href=\"");
			out.write(out.getPageURL(PasswordResetPage.COMMAND));
			out.write("\">");
			out.writeEncode(out.getString("controls:Login.ForgotPassword"));
			out.write("</a>&nbsp;</small>");

		out.write("</td><td align=right nowrap>"); // Button table
		
			out.writeButton(out.getString("controls:Login.Login"));
			
		out.write("</td></tr></table>"); // Button table
		
			out.write("</td></tr><tr><td colspan=2 align=center>"); // Inner
			
			if (this.remember)
			{
				out.write("<small><br>");
				out.writeCheckbox(LoginPage.PARAM_KEEP, null, false);
				out.write(" ");
				out.writeTooltip(out.getString("controls:Login.KeepLogin"), out.getString("controls:Login.KeepHelp"));
				out.write("</small>");
			}
		
		out.write("</td></tr></table>"); // Inner
						
		out.writeFormClose();
	}
}
