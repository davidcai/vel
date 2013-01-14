package samoyan.controls;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import samoyan.servlet.WebPage;

public abstract class TagControl
{
	private WebPage out;
	private Map<String, String> attrs;
	private Map<String, String> styleAttrs;
	private Set<String> cssClasses;

	public TagControl(WebPage outputPage)
	{
		this.out = outputPage;
	}
	
	public TagControl setAttribute(String name, String value)
	{
		if (this.attrs==null)
		{
			this.attrs = new HashMap<String, String>();
		}
		this.attrs.put(name, value);
		return this;
	}
	public String getAttribute(String name)
	{
		return (this.attrs==null? null : this.attrs.get(name));
	}
		
	public TagControl addCssClass(String cssClass)
	{
		if (this.cssClasses==null)
		{
			this.cssClasses = new HashSet<String>();
		}
		this.cssClasses.add(cssClass);
		return this;
	}
	public boolean isCssClass(String cssClass)
	{
		return (this.cssClasses==null? false : this.cssClasses.contains(cssClass));
	}

	public TagControl setStyleAttribute(String name, String value)
	{
		if (this.styleAttrs==null)
		{
			this.styleAttrs = new HashMap<String, String>();
		}
		this.styleAttrs.put(name, value);
		return this;
	}
	public String getStyleAttribute(String name)
	{
		return (this.styleAttrs==null? null : this.styleAttrs.get(name));
	}

	protected void writeTag(String tagName)
	{
		out.write("<");
		out.write(tagName);
		
		// Attributes
		if (this.attrs!=null)
		{
			for (String n : this.attrs.keySet())
			{
				String v = this.attrs.get(n);
				if (n!=null && v!=null)
				{
					out.write(" ");
					out.writeEncode(n);
					if (v.length()>0)
					{
						out.write("=\"");
						out.writeEncode(v);
						out.write("\"");
					}
				}
			}
		}
		
		// CSS classes
		if (this.cssClasses!=null)
		{
			boolean first = true;
			out.write(" class=\"");
			for (String n : this.cssClasses)
			{
				if (n!=null)
				{
					if (!first)
					{				
						out.write(" ");
					}
					first = false;
					
					out.writeEncode(n);
				}
			}
			out.write("\"");
		}

		// CSS style attributes
		if (this.styleAttrs!=null)
		{
			out.write(" style=\"");
			for (String n : this.styleAttrs.keySet())
			{
				String v = this.styleAttrs.get(n);
				if (n!=null && v!=null)
				{
					out.writeEncode(n);
					out.write(":");
					out.writeEncode(v);
					out.write(";");
				}
			}
			out.write("\"");
		}

		out.write(">");
	}
	
	public abstract void render();
}
