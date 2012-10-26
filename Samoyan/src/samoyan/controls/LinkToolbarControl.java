package samoyan.controls;

import java.util.ArrayList;
import java.util.List;

import samoyan.core.Util;
import samoyan.servlet.WebPage;

public class LinkToolbarControl extends WebPage
{
	private class Link
	{
		String url;
		String icon;
		String caption;
	}
	private List<Link> links = new ArrayList<Link>();
	
	public LinkToolbarControl(WebPage outputPage)
	{
		setContainer(outputPage);
	}
	
	/**
	 * 
	 * @param icon The path to the resource to use as icon for the link, e.g. "icons/pencil_16.png". May be <code>null</code>.
	 * @param caption The caption of the toolbar entry. May not be empty or <code>null</code>.
	 * @param url The URL of the link. May be <code>null</code>, in which case the label will be shown without a link.
	 * @return 
	 */
	public LinkToolbarControl addLink(String caption, String url, String icon)
	{
		if (Util.isEmpty(caption)) return this;
		
		Link link = new Link();
		link.icon = icon;
		link.caption = caption;
		link.url = url;
		this.links.add(link);
		
		return this;
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		if (this.links.size()==0) return;

		write("<div class=LinkToolbar>");
		write("<table><tr valign=middle>");
		for (Link link : this.links)
		{
			if (!Util.isEmpty(link.icon))
			{
				write("<td>");
				writeImage(link.icon, link.caption);
				write("</td>");
			}

			write("<td>");
			if (!Util.isEmpty(link.url))
			{
				writeLink(link.caption, link.url);
			}
			else
			{
				write("<span>");
				writeEncode(link.caption);
				write("</span>");
			}
			write("</td>");
		}
		write("</tr></table>");
		write("</div>");
	}
}
