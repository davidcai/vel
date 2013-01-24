package samoyan.apps.system;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import samoyan.apps.system.TypeAhead;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;

public class TypeAhead extends WebPage
{
	private class Option
	{
		String key;
		String value;
		String html;
	}
	private List<Option> options;
	
	@Override
	public final String getMimeType() throws Exception
	{
		return "text/plain";
	}

	@Override
	public final boolean isSecureSocket() throws Exception
	{
		return getContext().isSecureSocket();
	}

	@Override
	public final boolean isEnvelope() throws Exception
	{
		return false;
	}
	
	@Override
	public final boolean isLog() throws Exception
	{
		return false;
	}
	
	@Override
	public final void renderHTML() throws Exception
	{
		String q = getParameterString("q");
		
		String preQ = "";
		String searchQ = q.trim();
		
		this.options = new ArrayList<TypeAhead.Option>();
		if (!Util.isEmpty(searchQ))
		{
			this.doQuery(searchQ);
		}
		
		int maxOption = this.getMaxOptions();
		int printed = 0;
		write("{");
		
		write("\"options\":[");
		for (Option o : this.options)
		{
			if (printed>0)
			{
				write(",");
			}
			write("{\"value\":");
			write("\"");
			write(Util.strReplace(preQ + o.value, "\"", "\\\""));
			write("\",\"key\":");
			write("\"");
			write(Util.strReplace(preQ + o.key, "\"", "\\\""));
			write("\",\"html\":\"");
			writeEncodeHighlight(o.html, searchQ);
			write("\"}");
			
			printed ++;
			if (printed>=maxOption)
			{
				break;
			}
		}
		write("]");
		
		write("}");
	}
	
	private void writeEncodeHighlight(String html, String query)
	{
		query = Util.htmlEncode(query); // In case there are & or > in the uery string itself
		
		String lcText = html.toLowerCase(getLocale());
		String lcQ = query.toLowerCase(getLocale());
		
		int p;
		int q = 0;
		while (true)
		{
			p = lcText.indexOf(lcQ, q);
			if (p<0)
			{
				write(html.substring(q));
				break;
			}
			else
			{
				int closeB = html.lastIndexOf(">", p);
				int openB = html.lastIndexOf("<", p);
				if (openB>=0 && openB>closeB)
				{
					// We're inside a tag
					write(html.substring(q, p + query.length()));
				}
				else
				{
					write(html.substring(q, p));
					write("<b>");
					write(html.substring(p, p + query.length()));
					write("</b>");
				}
				q = p + query.length();
			}
		}
	}

	/**
	 * To be called from within {@link #doQuery(String)} to add an option matching the query.
	 * @param key The internal key corresponding to the value. Must not be <code>null</code>.
	 * @param value A text only value to enter into the edit field. Does not need to be encoded.
	 * If <code>null</code>, <code>key</code> will also be used as value.
	 * @param html Raw HTML to show in the type ahead drop down. Must be properly encoded.
	 * If <code>null</code>, the <code>value</code> will be shown in the drop down
	 */
	protected final void addOption(Object key, Object value, String html)
	{
		Option o = new Option();
		o.key = key.toString();
		o.value = value!=null ? value.toString() : o.key;
		o.html = html!=null? html : Util.htmlEncode(o.value);
		this.options.add(o);
	}
		
	/**
	 * Equivalent to <code>addOption(key, null, null)</code>.
	 * @param key
	 */
	protected final void addOption(Object key)
	{
		addOption(key, null, null);
	}
	
	/**
	 * Equivalent to <code>addOption(key, value, null)</code>.
	 * @param key
	 * @param value
	 */
	protected final void addOption(Object key, Object value)
	{
		addOption(key, value, null);
	}
	
	/**
	 * To be overridden by subclass to populate the options that match the given query string.
	 * This method should run the appropriate query and call {@link #addOption(String, String)}.
	 * @param q
	 * @throws Exception 
	 */
	protected void doQuery(String q) throws SQLException, Exception
	{
	}
	
	/**
	 * To be overridden by subclass to return the maximum number of options to show in the drop down.
	 * This should generally be less for a smart phone with limited real estate.
	 */
	protected int getMaxOptions()
	{
		RequestContext ctx = getContext();
		boolean smartPhone = ctx.getUserAgent().isSmartPhone();
		return smartPhone? 4 : 8;
	}
}
