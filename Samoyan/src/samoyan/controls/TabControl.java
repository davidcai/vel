package samoyan.controls;

import java.util.ArrayList;
import java.util.List;

import samoyan.servlet.WebPage;

public class TabControl
{
	private WebPage page;

	private class TabSpec
	{
		String key;
		String link;
		String title;
	}
	private List<TabSpec> tabs = new ArrayList<TabSpec>();
	private String currentTab = null;
	
	public TabControl(WebPage outputPage)
	{
		this.page = outputPage;
	}
	
	public TabControl addTab(String key, String title, String url)
	{
		TabSpec tab = new TabSpec();
		tab.key = key;
		tab.title = title;
		tab.link = url;
		tabs.add(tab);
		
		return this;
	}
	
	public TabControl setCurrentTab(String key)
	{
		currentTab = key;
		
		return this;
	}
	
	public void render()
	{
		page.write("<table cellspacing=0 cellpadding=0 class=TabCtrl><tr valign=bottom>");
		
		for (int t=0; t<tabs.size(); t++)
		{
			TabSpec tab = tabs.get(t);
			
			// Print separator
			page.write("<td><div class=TabSep>&nbsp;</div></td>");
			
			// Print tab
			page.write("<td nowrap><div class=Tab");
			boolean current = (currentTab!=null && currentTab.equals(tab.key) || (currentTab==null && t==0));
			if (current)
			{
				page.write("Front");
			}
			else
			{
				page.write("Back");
			}
			page.write(">");
			if (tab.link!=null)
			{
				page.write("<a href=\"");
				page.write(tab.link);
				page.write("\"");
				if (current)
				{
					page.write(" class=QuietLink");
				}
				page.write(">");
			}
			page.writeEncode(tab.title);
			if (tab.link!=null)
			{
				page.write("</a>");
			}
			page.write("</div></td>");
			
			// Print separator
			page.write("<td><div class=TabSep>&nbsp;</div></td>");
		}
		
		// Print separator
		page.write("<td width=\"100%\"><div class=TabSep>&nbsp;</div></td>");

		page.write("</tr></table>");
	}
}
