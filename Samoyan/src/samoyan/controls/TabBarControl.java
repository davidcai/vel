package samoyan.controls;

import java.util.ArrayList;
import java.util.List;

import samoyan.apps.master.HelpPage;
import samoyan.apps.master.JoinPage;
import samoyan.apps.master.LoginPage;
import samoyan.apps.master.LogoutPage;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;

public class TabBarControl extends WebPage
{
	private class Icon
	{
		String image;
		String title;
		String command;
	}
	private List<Icon> tabs;
	
	public TabBarControl(WebPage outputPage) throws Exception
	{
		setContainer(outputPage);

		this.tabs = new ArrayList<Icon>();
	}

	@Override
	public void renderHTML() throws Exception
	{
		if (this.tabs.size()==0)
		{
			return;
		}
		
		RequestContext ctx = getContext();
		boolean smartPhone = ctx.getUserAgent().isSmartPhone();
		String command1 = ctx.getCommand(1);

		write("<table class=\"TabBar");
//		if (smartPhone)
//		{
//			write(" Fixed");
//		}
		write("\"><tr>");
		for (Icon t : this.tabs)
		{
			if (t.image==null && Util.isEmpty(t.title))
			{
				// Don't show tabs without icon and text
				continue;
			}
			
			boolean homeTab = Util.isEmpty(t.command);
			
			write("<td class=\"Tab");
			if (/*!homeTab &&*/ command1.equals(t.command))
			{
				write(" Current");
			}
			if (homeTab)
			{
				write(" Home");
			}
			write("\" width=\"");
			if (smartPhone)
			{
				write(100/tabs.size());
			}
			else
			{
				write("1");
			}
			write("%\">");
			
			write("<a href=\"");
			writeEncode(getPageURL(t.command));
			write("\" onclick=\"backClear();\">");
			if (t.image!=null)
			{
				new ImageControl(this)
					.resource(t.image)
					.altText(t.title)
//					.height(smartPhone? 32 : 0)
					.render();
				write("<br>");
			}
//			if (smartPhone || !homeTab || t.image==null)
//			{
				if (!Util.isEmpty(t.title))
				{
					writeEncode(t.title);
				}
//			}
			write("</a>");
			
			write("</td>");
		}
		
		// Help | Register | Login | Logout
		if (!smartPhone)
		{
			write("<td class=Links>");
			
			write("<span id=LinkHelp>");
			writeLink(getString("controls:TabBar.Help"), getPageURL(HelpPage.COMMAND));
			write("</span>");
			if (ctx.getUserID()==null)
			{
				Server fed = ServerStore.getInstance().loadFederation();
				if (fed.isOpenRegistration())
				{
					write("<span id=LinkRegister>");
					write(" | ");
					writeLink(getString("controls:TabBar.Register"), getPageURL(JoinPage.COMMAND));
					write("</span>");
				}
				write("<span id=LinkLogin>");
				write(" | ");
				writeLink(getString("controls:TabBar.Login"), getPageURL(LoginPage.COMMAND));
				write("</span>");
			}
			else
			{
				User user = UserStore.getInstance().load(ctx.getUserID());
				write("<span id=LinkLogout>");
				write(" | ");
				writeLink(getString("controls:TabBar.Logout", user.getLoginName()), getPageURL(LogoutPage.COMMAND));
				write("</span>");
			}

//			// Search box
//			if (this.searchCommand!=null)
//			{
//				writeFormOpen("GET", this.searchCommand);
//				write("<input class=SearchBox type=search name=\"");
//				writeEncode(this.searchParam);
//				write("\" maxlength=128>");
//				writeFormClose();
//			}

			write("</td>");
		}
		
		write("</tr></table>");
	}
	
	public void addTab(String image, String title, String command)
	{
		Icon icon = new Icon();
		icon.image = image;
		icon.title = title;
		icon.command = command;
		this.tabs.add(icon);
	}	
	
//	public void enableSearchBox(String command, String paramName)
//	{
//		this.searchCommand = command;
//		this.searchParam = paramName;
//	}
}
