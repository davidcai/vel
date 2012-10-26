package samoyan.controls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import samoyan.core.Cache;
import samoyan.core.Util;
import samoyan.servlet.Dispatcher;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;

public class NavTreeControl
{
	private WebPage outputPage = null;

	public class NavTreeEntry
	{
		String title = null;
		String url = null;
		boolean header = false;
		
		public String getTitle()
		{
			return title;
		}
		public void setTitle(String title)
		{
			this.title = title;
		}
		
		public String getURL()
		{
			return url;
		}
		public void setURL(String url)
		{
			this.url = url;
		}
	}
	private List<NavTreeEntry> links;

	public NavTreeControl(WebPage outputPage)
	{
		this.outputPage = outputPage;
		
		this.links = new ArrayList<NavTreeEntry>();
	}
	
	public NavTreeEntry addHeader(String title)
	{
		NavTreeEntry header = new NavTreeEntry();
		header.title = title;
		header.header = true;
		this.links.add(header);
		return header;
	}

	public NavTreeEntry addLink(String title, String url)
	{
		NavTreeEntry link = new NavTreeEntry();
		link.title = title;
		link.url = url;
		link.header = false;
		this.links.add(link);
		return link;
	}
	
	/**
	 * Adds a link to the page indicated by the command and params.
	 * This method may need to instantiate the page in order to get its title.
	 * @param command
	 * @param params
	 * @throws Exception
	 */
	public NavTreeEntry addPage(String command, Map<String, String> params) throws Exception
	{
		// Check cache for the page's locale
		String url = outputPage.getPageURL(command, params);
		String cacheKey = "navtree.pgttl:" + outputPage.getLocale().toString() + "." + url;
		String title = (String) Cache.get(cacheKey);
		if (title==null)
		{
			RequestContext ctx = (RequestContext) outputPage.getContext().clone();
			ctx.setMethod("GET");
			ctx.setCommand(command);
			ctx.getParameters().clear();
			if (params!=null)
			{
				ctx.getParameters().putAll(params);
			}
			ctx.getPostedFiles().clear();
			
			WebPage page = Dispatcher.lookup(ctx);
			if (page==null)
			{
				throw new NullPointerException();
			}
			
			title = Dispatcher.getTitle(page, ctx);
			
			// Cache for future use
			Cache.insert(cacheKey, title);
		}
		return addLink(title, url);
	}
	
	public void render()
	{
		RequestContext ctx = outputPage.getContext();
		
		if (this.links.size()==0)
		{
			return;
		}

		boolean openLI = false;
		outputPage.write("<div class=NavTree>");
		outputPage.write("<ul>");
		for (NavTreeEntry entry : this.links)
		{
			String title = entry.title;
			if (Util.isEmpty(title))
			{
				title = outputPage.getString("controls:NavTree.Untitled");
			}
			
			if (entry.header==false)
			{
				// Link
				outputPage.write("<li>");
				if (entry.url!=null)
				{
					outputPage.writeLink(title, entry.url);
				}
				else
				{
					outputPage.writeEncode(title);
				}
				outputPage.write("</li>");
			}
			else
			{
				// Header
				if (openLI)
				{
					outputPage.write("</ul></li>");
				}
				outputPage.write("<li>");
				outputPage.writeEncode(title);
				outputPage.write("<ul>");
				
				openLI = true;
			}
		}
		if (openLI)
		{
			outputPage.write("</ul></li>");
		}
		outputPage.write("</ul>");
		outputPage.write("</div>");
	}
}
