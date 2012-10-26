package samoyan.controls;

import java.util.ArrayList;
import java.util.List;


import samoyan.apps.master.HelpPage;
import samoyan.apps.master.JoinPage;
import samoyan.apps.master.LoginPage;
import samoyan.apps.master.LogoutPage;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.WebPage;
import samoyan.servlet.RequestContext;

public class TopBarControl extends WebPage
{
	private class Icon
	{
		String image;
		String title;
		String command;
	}
	
	private Icon logo;
	private List<Icon> tabs;
	private String searchCommand;
	private String searchParam;
	
	WebPage outputPage;

	public TopBarControl(WebPage outputPage) throws Exception
	{
		setContainer(outputPage);

		this.outputPage = outputPage;
		this.logo = null;
		this.tabs = new ArrayList<Icon>();
		this.searchCommand = null;
		this.searchParam = null;
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();

		write("<table class=TopBar><tr>");

		// Write logo
		write("<td class=Logo>");
		if (this.logo!=null)
		{
			write("<a href=\"");
			writeEncode(getPageURL(this.logo.command));
			write("\">");
			if (this.logo.image!=null)
			{
				writeImage(this.logo.image, this.logo.title);
			}
			else
			{
				writeEncode(this.logo.title);
			}
			write("</a>");
		}
		write("</td>");
	
		// Tabs
		String command1 = ctx.getCommand(1);
		write("<td class=Tabs>");
		if (this.tabs.size()>0)
		{
			write("<table><tr>");
			for (Icon icon : this.tabs)
			{
				write("<td");
				if (command1.equals(icon.command))
				{
					write(" class=Current");
				}
				write(">");
				write("<a href=\"");
				writeEncode(getPageURL(icon.command));
				write("\">");
				if (icon.image!=null)
				{
					writeImage(icon.image, icon.title);
					write("<br>");
				}
				writeEncode(icon.title);
				write("</a>");
				write("</td>");
			}
			write("</tr></table>");
		}
		write("</td>");
		
		// Login/logout links
		write("<td class=Links>");
		
		write("<span id=LinkHelp>");
		writeLink(getString("controls:TopBar.Help"), getPageURL(HelpPage.COMMAND));
		write("</span>");
		if (ctx.getUserID()==null)
		{
			Server fed = ServerStore.getInstance().loadFederation();
			if (fed.isOpenRegistration())
			{
				write("<span id=LinkRegister>");
				write(" | ");
				writeLink(getString("controls:TopBar.Register"), getPageURL(JoinPage.COMMAND));
				write("</span>");
			}
			write("<span id=LinkLogin>");
			write(" | ");
			writeLink(getString("controls:TopBar.Login"), getPageURL(LoginPage.COMMAND));
			write("</span>");
		}
		else
		{
			User user = UserStore.getInstance().load(ctx.getUserID());
			write("<span id=LinkLogout>");
			write(" | ");
			writeLink(getString("controls:TopBar.Logout", user.getLoginName()), getPageURL(LogoutPage.COMMAND));
			write("</span>");
		}
		
		// Search box
		if (this.searchCommand!=null)
		{
			writeFormOpen("GET", this.searchCommand);
			write("<input class=SearchBox type=search name=\"");
			writeEncode(this.searchParam);
			write("\" maxlength=128>");
			writeFormClose();
		}
		
		
		write("</td>");
		write("</tr></table>");
	}
	
	public void setLogo(String image, String title, String command)
	{
		this.logo = new Icon();
		this.logo.image = image;
		this.logo.title = title;
		this.logo.command = command;
	}
	
	public void addTab(String image, String title, String command)
	{
		Icon icon = new Icon();
		icon.image = image;
		icon.title = title;
		icon.command = command;
		this.tabs.add(icon);
	}
	
	public void enableSearchBox(String command, String paramName)
	{
		this.searchCommand = command;
		this.searchParam = paramName;
	}
}
