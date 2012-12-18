package samoyan.controls;

import java.util.ArrayList;
import java.util.List;

import samoyan.servlet.WebPage;

public class TabControl
{
	private WebPage page;
	private int alignment = -1;
	private int style = 0;
	
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
	
	public TabControl setAlignRight()
	{
		alignment = 1;
		return this;
	}
	public TabControl setAlignLeft()
	{
		alignment = -1;
		return this;
	}
	public TabControl setAlignCenter()
	{
		alignment = 0;
		return this;
	}
	public TabControl setAlignStretch()
	{
		alignment = 100;
		return this;
	}
	
	public TabControl setStyleTab()
	{
		style = 0;
		return this;
	}
	public TabControl setStyleButton()
	{
		style = 1;
		return this;
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
		page.write("<table cellspacing=0 cellpadding=0 class=\"TabCtrl ");
		if (this.style==1)
		{
			page.write("ButtonStyle");
		}
		else
		{
			page.write("TabStyle");
		}
		page.write("\"><tr valign=bottom>");
		
		if (this.alignment!=-1)
		{
			// Print separator
			page.write("<td width=\"");
			if (this.alignment==0)
			{
				page.write("50");
			}
			else if (this.alignment==100)
			{
				page.write("0");
			}
			else
			{
				page.write("100");
			}
			page.write("%\"><div class=TabSep>&nbsp;</div></td>");
		}

		for (int t=0; t<tabs.size(); t++)
		{
			TabSpec tab = tabs.get(t);
			
			// Print separator
			page.write("<td width=\"0%\"><div class=TabSep>&nbsp;</div></td>");
			
			// Print tab
			page.write("<td nowrap");
			if (this.alignment==100)
			{
				page.write(" width=\"");
				page.write(100/tabs.size());
				page.write("%\"");
			}
			page.write("><div class=\"Tab");
			boolean current = (currentTab!=null && currentTab.equals(tab.key) || (currentTab==null && t==0));
			if (current)
			{
				page.write("Front");
			}
			else
			{
				page.write("Back");
			}
			if (t==0)
			{
				page.write(" First");
			}
			else if (t==tabs.size()-1)
			{
				page.write(" Last");
			}
			page.write("\">");
			if (tab.link!=null)
			{
				page.write("<a href=\"");
				page.write(tab.link);
				page.write("\"");
//				if (current)
//				{
//					page.write(" class=QuietLink");
//				}
				page.write(">");
			}
			page.writeEncode(tab.title);
			if (tab.link!=null)
			{
				page.write("</a>");
			}
			page.write("</div></td>");
			
			// Print separator
			page.write("<td width=\"0%\"><div class=TabSep>&nbsp;</div></td>");
		}
		
		if (this.alignment!=1)
		{
			// Print separator
			page.write("<td width=\"");
			if (this.alignment==0)
			{
				page.write("50");
			}
			else if (this.alignment==100)
			{
				page.write("0");
			}
			else
			{
				page.write("100");
			}
			page.write("%\"><div class=TabSep>&nbsp;</div></td>");
		}

		page.write("</tr></table>");
	}
}
