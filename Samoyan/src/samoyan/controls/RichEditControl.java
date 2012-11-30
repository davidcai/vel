package samoyan.controls;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.WebPage;

public class RichEditControl extends WebPage
{
	private String name;
	private String initialValue;
	private int cols = 60;
	private int rows = 5;
	
	public RichEditControl(WebPage outputPage)
	{
		setContainer(outputPage);
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public void setInitialValue(String html)
	{
		this.initialValue = html;
	}

	public void setCols(int c)
	{
		this.cols = c;
	}
	public void setRows(int r)
	{
		this.rows = r;
	}
	
	@Override
	public void renderHTML()
	{
		RequestContext ctx = getContext();
		
		
		int width = this.cols * 6 + 30;
		if (width > ctx.getUserAgent().getScreenWidth()-16)
		{
			width = ctx.getUserAgent().getScreenWidth()-16;
		}
//		int height = this.rows * 18 + 1;
		
		write("<div class=RichEditCtrl>");
		
		boolean supportedDevice = !ctx.getUserAgent().isMobile();
		if (supportedDevice)
		{
			writeToolbar();
			write("<br>");
		}
		
		// Hidden content var
		write("<input type=hidden name=\"");
		write(name);
		write("\" value=\"");
		String val = ctx.getParameter(name);
		if (val==null)
		{
			val = this.initialValue;
		}
		if (val==null) val = " ";
		writeEncode(val);
		write("\">");

		// Rich edit iframe
		write("<iframe id=\"richedit_");
		write(name);
		write("\"");
		if (isFormException(name))
		{
			write(" class=Error");
		}
		write(" src=\"about:blank\" style=\"width:");
		write(width);
		write("px;height:");
		write(1.4F * this.rows); // 1.4 is the line height (defined in the CSS)
		write("em\" frameborder=0></iframe>");
		
//		if (ctx.isUserAgentIE())
//		{
//			write("<br><small>");
//			writeEncode(getString("master.richedit.IEShiftEnter"));
//			write("</small>");
//		}
		
//		if (phone)
//		{
//			write("<br>");
//			writeToolbar();
//		}
//		if (phone)
//		{
//			write("<table><tr><td>");
//			writeImage("icons/basic1/warning_16.png", getString("controls:RichEdit.UnsupportedDevice"));
//			write("</td><td><small>");
//			writeEncode(getString("controls:RichEdit.UnsupportedDevice"));
//			write("</small></td></tr></table>");
//		}
		write("</div>");
		
		if (getEphemeral("richedit")==null)
		{
			writeIncludeJS("richedit/richedit.js");
			
			write("<script type=\"text/javascript\">var RICH_EDIT_LINK_PROMPT=\"");
			writeEncode(getString("controls:RichEdit.LinkPrompt"));
			write("\";");
			
			write("var RICH_EDIT_DOC_START='");
			write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
			write("<html><head>");
			write("<meta name=\"SKYPE_TOOLBAR\" CONTENT=\"SKYPE_TOOLBAR_PARSER_COMPATIBLE\">");
			write("<base href=about:blank>");
			write("<link type=\"text/css\" rel=stylesheet href=\"");
			write(UrlGenerator.getResourceURL(ctx.isSecureSocket(), ctx.getHost(), "samoyan.less"));
			write("\">");
//			writeIncludeCSS("richedit/richedit.css");
			write("<link type=\"text/css\" rel=stylesheet href=\"");
			write(UrlGenerator.getResourceURL(ctx.isSecureSocket(), ctx.getHost(), "richedit/inner.css"));
			write("\">");
			write("</head><body class=RichEditBody>");
			write("';");
			
			write("var RICH_EDIT_DOC_END='");
			write("</body></html>");
			write("';");
			
			write("</script>");

			setEphemeral("richedit", "1");
		}
	}
	
	private void writeToolbar()
	{
		write("<span class=NOBR>");
		writeToolbarButton("bold");
		writeToolbarButton("italic");
		writeToolbarButton("underline");
		writeToolbarButton("strikethrough");
		writeToolbarButton("superscript");
		writeToolbarButton("subscript");
		write("</span>");
		write("<span class=NOBR>");
		writeSeparator();
		writeToolbarButton("justifyleft");
		writeToolbarButton("justifycenter");
		writeToolbarButton("justifyright");
		writeSeparator();
		write("</span>");
		write("<span class=NOBR>");
		writeToolbarButton("insertorderedlist");
		writeToolbarButton("insertunorderedlist");
		writeToolbarButton("outdent");
		writeToolbarButton("indent");
		writeSeparator();
		write("</span>");
		write("<span class=NOBR>");
		writeToolbarButton("createlink");
		writeToolbarButton("inserthorizontalrule");
		writeToolbarButton("removeformat");
		writeSeparator();
		writeToolbarButton("expand");		
//		writeSeparator();
//		writeHelpBalloon(getString("controls:RichEdit.Help"));
		write("</span>");
	}
	
	private void writeToolbarButton(String cmd)
	{
		write("<img src=\"");
		write(getResourceURL("richedit/" + cmd + ".gif"));
		write("\" cmd=\"");
		write(cmd);
		write("\" applyto=\"");
		write(this.name);
		write("\" alt=\"");
		writeEncode(getString("controls:RichEdit." + cmd));
		write("\" title=\"");
		writeEncode(getString("controls:RichEdit." + cmd));
		write("\">");
	}
	private void writeSeparator()
	{
		write("&nbsp;");
	}

	public static String validate(String text)
	{
		if (text==null) return null;
		
		Set<String> simpleTags = new HashSet<String>();
		simpleTags.add("b");			simpleTags.add("/b");
		simpleTags.add("i");			simpleTags.add("/i");
		simpleTags.add("strong");		simpleTags.add("/strong");
		simpleTags.add("em");			simpleTags.add("/em");
		simpleTags.add("u");			simpleTags.add("/u");
		simpleTags.add("strike");		simpleTags.add("/strike");
		simpleTags.add("sup");			simpleTags.add("/sup");
		simpleTags.add("sub");			simpleTags.add("/sub");
		simpleTags.add("hr");
		simpleTags.add("br");
		simpleTags.add("blockquote");	simpleTags.add("/blockquote");
		simpleTags.add("ol");			simpleTags.add("/ol");
		simpleTags.add("ul");			simpleTags.add("/ul");
		simpleTags.add("li");			simpleTags.add("/li");
										simpleTags.add("/div");
										// simpleTags.add("/p"); // P tags are converted to DIVs
										simpleTags.add("/a");
		
		simpleTags.add("table");		simpleTags.add("/table");
		simpleTags.add("tr");			simpleTags.add("/tr");
		simpleTags.add("td");			simpleTags.add("/td");

		Set<String> ignoreDoubleTags = new HashSet<String>();
		ignoreDoubleTags.add("script");
		ignoreDoubleTags.add("style");
		ignoreDoubleTags.add("head");
		ignoreDoubleTags.add("iframe");
		ignoreDoubleTags.add("object");
		ignoreDoubleTags.add("applet");
		ignoreDoubleTags.add("embed");

		List<String> spanStack = new LinkedList<String>();
		boolean openParagraphDiv = false;
		
		int n = text.length();
		StringBuffer result = new StringBuffer(n);
		
		boolean inTag = false;
		String tagName = null;
		String ignoreUntilTag = null;
		for (int i=0; i<n; i++)
		{
			char ch = text.charAt(i);
			if (inTag==false)
			{
				if (ch!='<' && ignoreUntilTag==null)
				{
					if (ch=='\n' || ch=='\r' || ch=='\t')
					{
						result.append(ch);
					}
					else if (ch=='\"' || ch=='\'' || ch=='<' || ch=='>' /*|| ch=='&'*/ || ch>=128 || ch<32)
					{
						result.append("&#");
						result.append((int) ch);
						result.append(";");
					}
					else
					{
						result.append(ch);
					}
				}
				
				if (ch=='<') // Open tag
				{
					inTag = true;
					int p = text.indexOf('>', i);
					String fullTag = text.substring(i+1, p);
					tagName = fullTag;
					p = fullTag.indexOf(" ");
					if (p>0)
					{
						tagName = fullTag.substring(0, p);
					}
//					boolean closeTag = tagName.startsWith("/");
					
					if (tagName.startsWith("!--")) // Comments, can happen for copy-paste from Word
					{
						// Locate end of the tag
						int e = text.indexOf("-->", i);
						if (e>=0)
						{
							i = e+2; // Attention - altering loop variable
						}
						continue;
					}
					else if (ignoreDoubleTags.contains(tagName.toLowerCase(Locale.US)))
					{
						ignoreUntilTag = "/" + tagName;
					}
					else if (ignoreUntilTag!=null && ignoreUntilTag.equalsIgnoreCase(tagName))
					{
						ignoreUntilTag = null;
					}
					else if (simpleTags.contains(tagName.toLowerCase(Locale.US)))
					{
						result.append("<");
						result.append(tagName.toLowerCase(Locale.US));
						result.append(">");
					}
					else if (tagName.equalsIgnoreCase("a"))
					{
						int h = fullTag.indexOf("href=\"");
						if (h>0)
						{
							h += 6;
							int g = fullTag.indexOf("\"", h);
							if (g>0)
							{
								String href = fullTag.substring(h, g).trim();
								String lcHref = href.toLowerCase(Locale.US);
								if (lcHref.startsWith("javascript:"))
								{
									href = "about:blank";
								}
								if (lcHref.startsWith("../"))
								{
									href = "http://" + Setup.getHost() + lcHref.substring(2);
								}
								result.append("<a href=\"");
								result.append(href);
								result.append("\">");
							}
						}
					}
					else if (tagName.equalsIgnoreCase("span"))
					{
						if (fullTag.indexOf("text-decoration: underline;")>0)
						{
							result.append("<u>");
							spanStack.add("</u>");
						}
						else if (fullTag.indexOf("text-decoration: line-through;")>0)
						{
							result.append("<strike>");
							spanStack.add("</strike>");
						}
						else if (fullTag.indexOf("text-decoration: super;")>0 ||
								 fullTag.indexOf("vertical-align: super;")>0)
						{
							result.append("<sup>");
							spanStack.add("</sup>");
						}
						else if (fullTag.indexOf("text-decoration: sub;")>0 ||
								 fullTag.indexOf("vertical-align: sub;")>0)
						{
							result.append("<sub>");
							spanStack.add("</sub>");
						}
						else if (fullTag.indexOf("font-weight: bold;")>0)
						{
							result.append("<b>");
							spanStack.add("</b>");
						}
						else if (fullTag.indexOf("font-style: italic;")>0)
						{
							result.append("<i>");
							spanStack.add("</i>");
						}
						else
						{
							spanStack.add("");
						}
					}
					else if (tagName.equalsIgnoreCase("/span"))
					{
						if (spanStack.size()>0)
						{
							result.append(spanStack.remove(0));
						}
					}
					else if (tagName.equalsIgnoreCase("div") ||
							tagName.equalsIgnoreCase("p")) // P tags are converted to DIVs to eliminate padding above P tags
					{
						// Close previous paragraph which can remain open if there was no /P closing tag
						if (tagName.equalsIgnoreCase("p"))
						{
							if (openParagraphDiv)
							{
								result.append("</div><br>");
//								openParagraphDiv = false;
							}
							openParagraphDiv = true;
						}
						
						if (fullTag.indexOf("text-align: left")>0)
						{
							result.append("<div align=left>");
						}
						else if (fullTag.indexOf("text-align: right")>0)
						{
							result.append("<div align=right>");
						}
						else if (fullTag.indexOf("text-align: center")>0)
						{
							result.append("<div align=center>");
						}
						else if (fullTag.indexOf("left")>0)
						{
							result.append("<div align=left>");
						}
						else if (fullTag.indexOf("right")>0)
						{
							result.append("<div align=right>");
						}
						else if (fullTag.indexOf("center")>0)
						{
							result.append("<div align=center>");
						}
						else
						{
							result.append("<div>");
						}
					}
					else if (tagName.equalsIgnoreCase("/p"))
					{
						if (openParagraphDiv)
						{
							result.append("</div><br>");
						}
						openParagraphDiv = false;
					}
//					else if (tagName.equalsIgnoreCase("p"))
//					{
//						if (fullTag.indexOf("left")>0)
//						{
//							result.append("<p align=left>");
//						}
//						else if (fullTag.indexOf("right")>0)
//						{
//							result.append("<p align=right>");
//						}
//						else if (fullTag.indexOf("center")>0)
//						{
//							result.append("<p align=center>");
//						}
//						else
//						{
//							result.append("<p>");
//						}
//					}
				}
			}
			else if (ch=='>')
			{
				inTag = false;
			}
		}
		
//		Debug.println(text);
//		Debug.println("*****");
//		Debug.println(result.toString());
		
		return result.toString();
	}
}
