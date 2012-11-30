package samoyan.apps.master;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import samoyan.core.Cache;
import samoyan.core.DateFormatEx;
import samoyan.core.Util;
import samoyan.servlet.Controller;
import samoyan.servlet.Setup;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.PageNotFoundException;

public class StaticPage extends WebPage
{
	private String head = "";
	private String body = "";
	
	@Override
	public void init() throws Exception
	{		
		// Load the page
		String html = loadFile(getFileName());
		if (Util.isEmpty(html))
		{
			throw new PageNotFoundException();
		}
		
		// Get HEAD and BODY
		this.head = getBetween(html, "<head>", "</head>");
		this.body = getBetween(html, "<body>", "</body>");
		
//		// Get keywords
//		String keywords = getBetween(this.head, "<keywords>", "</keywords>");
//		if (!Util.isEmpty(keywords))
//		{
//			keywords = processNoWrite(keywords).trim();
//			super.addKeywords(keywords);
//		}
	}

	private String loadFile(String fileName) throws Exception
	{
		String cached = (String) Cache.get("staticpage:" + fileName);
		if (cached!=null)
		{
			return (String) cached;
		}
		
		Locale locale = getLocale();
		String language = locale.getLanguage();
		String country = locale.getCountry();
		String variant = locale.getVariant();
		
		List<String> suffixes = new ArrayList<String>();
		if (variant!=null && country!=null && language!=null)
		{
			suffixes.add("_" + language + "_" + country + "_" + variant);
		}
		if (country!=null && language!=null)
		{
			suffixes.add("_" + language + "_" + country);
		}
		if (language!=null)
		{
			suffixes.add("_" + language);
		}
		suffixes.add("");

		InputStream in = null;
		for (int i=0; i<suffixes.size() && in==null; i++)
		{
			in = Controller.getResourceAsStream("WEB-INF/html/" + fileName + suffixes.get(i) + ".html");
		}
		if (in==null)
		{
			return "";
		}
		String html = Util.inputStreamToString(in, "UTF-8");
		
		// Remove redundant new lines
		html = Util.strReplace(html, "\r\n\r\n", "\r\n");
		html = Util.strReplace(html, "\r\n\r\n", "\r\n");
		html = Util.strReplace(html, ">\r\n<", "><");
		html = Util.strReplace(html, "\r\n", " ");
		
		if (Setup.isDebug()==false)
		{
			Cache.insert("staticpage:" + fileName, html);
		}
		return html;
	}
	
	private String getBetween(String src, String start, String end)
	{
		int p = src.indexOf(start);
		if (p>=0)
		{
			p += start.length();
			int q = src.indexOf(end, p);
			if (q>=p)
			{
				return src.substring(p, q);
			}
		}
		return "";
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		process(this.body);
	}

//	@Override
//	public String getDescription() throws Exception
//	{
//		String desc = getBetween(this.head, "<description>", "</description>");
//		desc = processNoWrite(desc).trim();
//		return desc;
//	}

	@Override
	public String getTitle() throws Exception
	{
		String title = getBetween(this.head, "<title>", "</title>");
		title = processNoWrite(title).trim();
		return title;
	}
		
	protected String processNoWrite(String html) throws Exception
	{
		ByteArrayOutputStream canvas = new ByteArrayOutputStream();
		ByteArrayOutputStream origCanvas = super.getCanvas();
		super.setCanvas(canvas);
		try
		{
			process(html);
		}
		finally
		{
			super.setCanvas(origCanvas);
		}
		return new String(canvas.toByteArray(), "UTF-8");
	}

	private void process(String html) throws Exception
	{
		if (html==null || html.length()==0) return;
		
		int p = 0;
		int q = 0;
		while (true)
		{
			p = html.indexOf("<", q);
			if (p<0)
			{
				write(html.substring(q));
				break;
			}
			else
			{
				write(html.substring(q, p));
			}
			
			String tagName;
			int closeBracket = html.indexOf(">", p+1);
			if (closeBracket<0)
			{
				throw new Exception("No closing bracket");
			}
			int space = html.indexOf(" ", p+1);
			if (space<0 || space>closeBracket)
			{
				tagName = html.substring(p+1, closeBracket);
			}
			else
			{
				tagName = html.substring(p+1, space);
			}
			
			if (tagName.equals("string"))
			{
				int closeTag = findClosingTag(html, "string", p); // html.indexOf("</string>", closeBracket);
				if (closeTag<0)
				{
					throw new Exception("Missing </string>");
				}
				Map<String, String> attrs = getAttributes(html.substring(p, closeBracket));
				String idAttr = attrs.get("id");
				if (Util.isEmpty(idAttr))
				{
					throw new Exception("Missing string id attribute");
				}

				List<String> params = new ArrayList<String>();
				String inside = html.substring(closeBracket+1, closeTag);
				int pPrm = 0;
				int qPrm = 0;
				while (true)
				{
					pPrm = inside.indexOf("<param>", qPrm);
					if (pPrm<0) break;
					qPrm = findClosingTag(inside, "param", pPrm);
					if (qPrm<0)
					{
						throw new Exception("Missing </param>");
					}
					pPrm += "<param>".length();
					String val = inside.substring(pPrm, qPrm);
					val = processNoWrite(val);
					
					params.add(val);
				}

				String val = getString(idAttr, params.toArray());
				if (val==null)
				{
					throw new Exception("Null string for " + idAttr);
				}
				writeEncode(val);
				q = closeTag + "</string>".length();
			}
			else if (tagName.equals("date"))
			{
				int closeTag = html.indexOf("</date>", closeBracket);
				if (closeTag<0)
				{
					throw new Exception("Missing </date>");
				}
				String spec = html.substring(closeBracket+1, closeTag);
				DateFormat formatter = DateFormatEx.getSimpleInstance("yyyy'-'MM'-'dd", getLocale(), getTimeZone());
				Date date = formatter.parse(spec);
				writeEncodeDay(date);
				q = closeTag + "</date>".length();
			}
			else if (tagName.equals("integer"))
			{
				int closeTag = html.indexOf("</integer>", closeBracket);
				if (closeTag<0)
				{
					throw new Exception("Missing </integer>");
				}
				String spec = html.substring(closeBracket+1, closeTag);
				long l = Long.parseLong(spec);
				writeEncodeLong(l);
				q = closeTag + "</integer>".length();
			}
//			else if (tagName.equals("money"))
//			{
//				int closeTag = html.indexOf("</money>", closeBracket);
//				if (closeTag<0)
//				{
//					throw new Exception("Missing </money>");
//					break;
//				}
//				String spec = html.substring(closeBracket+1, closeTag);
//				Money m = new Money(spec);
//				writeEncodeMoney(m);
//				q = closeTag + "</money>".length();
//			}
			else if (tagName.equals("if"))
			{
				int closeTag = findClosingTag(html, tagName, p);
				if (closeTag<0)
				{
					throw new Exception("Missing </"+tagName+">");
				}

				Map<String, String> attrs = getAttributes(html.substring(p, closeBracket));
				String flagAttr = attrs.get("flag");
				if (!Util.isEmpty(flagAttr) && doSwitch(flagAttr))
				{
					String internal = html.substring(closeBracket+1, closeTag);
					process(internal); // Recursive call
				}
				
				q = closeTag + ("</"+tagName+">").length();
			}
			else if (tagName.equals("a"))
			{
				boolean printLink = true;
				int closeTag = html.indexOf("</a>", closeBracket);
				if (closeTag<0)
				{
					throw new Exception("Missing </a>");
				}
				
//				Map<String, String> attrs = getAttributes(html.substring(p, closeBracket));
//				String href = attrs.get("href");
//				if (!Util.isEmpty(href) && href.startsWith("/"))
//				{
//					// Validate relative URLs
//					printLink = false;
//
//					int qMark = href.indexOf("?");
//					if (qMark>=0)
//					{
//						href = href.substring(0, qMark);
//					}
//					
//					String cmd = null;
//					String subCmd = null;
//					int aa = href.indexOf("/", 1);
//					if (aa<0)
//					{
//						cmd = href.substring(1);
//					}
//					else
//					{
//						cmd = href.substring(1, aa);
//						int bb = href.indexOf("/", aa+1);
//						if (bb<0)
//						{
//							subCmd = href.substring(aa+1);
//						}
//						else
//						{
//							subCmd = href.substring(aa+1, bb);
//						}
//					}
//					
//					try
//					{
//						Dispatcher.lo
//						RequestContext c = getContext().derive(cmd, subCmd, null);
//						HtmlPage pg = MainDispatcher.createPage(c);
//						if (pg!=null) printLink = true;
//					}
//					catch (Exception e)
//					{
//					}
//					
//					if (printLink==false)
//					{
//						throw new Exception("Broken link in " + getFileName() + ".html href=" + attrs.get("href") + " cmd=" + cmd + " subcmd=" + subCmd);
//					}
//				}
				
				if (printLink)
				{
					write(html.substring(p, closeBracket+1));
				}
				process(html.substring(closeBracket+1, closeTag));
				if (printLink)
				{
					write("</a>");
				}
				q = closeTag + "</a>".length();
			}
			else if (tagName.equals("img"))
			{
				boolean printImage = true;
				
//				Map<String, String> attrs = getAttributes(html.substring(p, closeBracket));
//				String src = attrs.get("src");
//				if (!Util.isEmpty(src) && src.startsWith("/" + UrlGenerator.PATH_RESOURCE + "/"))
//				{
//					// Validate relative URLs
//					File resFile = getResourceFile(src.substring(("/" + UrlGenerator.PATH_RESOURCE + "/").length()));
//					printImage = resFile.exists();
//					if (printImage==false)
//					{
//						int dot = src.lastIndexOf(".");
//						if (dot>0)
//						{
//							resFile = getResourceFile(src.substring("/res/".length(), dot) + "(" + Setup.getServerSwitchContext() + ")" + src.substring(dot));
//							printImage = resFile.exists();
//						}
//					}
//					if (printImage==false)
//					{
//						throw new Exception("Missing image " + src + " in " + getFileName() + ".html");
//					}
//				}
				
				if (printImage)
				{
					write(html.substring(p, closeBracket+1));
				}
				q = closeBracket+1;				
			}
			else if (tagName.startsWith("!--"))
			{
				q = html.indexOf("-->", p+1);
				if (q<0)
				{
					q = html.length();
				}
				else
				{
					q += "-->".length();
				}
			}
			else if (tagName.equals("import"))
			{
				Map<String, String> attrs = getAttributes(html.substring(p, closeBracket));
				String fileAttr = attrs.get("file");
				if (Util.isEmpty(fileAttr))
				{
					throw new Exception("Missing import file attribute");
				}

				String importedHtml = loadFile(fileAttr);
				if (!Util.isEmpty(importedHtml))
				{
					process(importedHtml);
				}
				
				q = closeBracket+1;
			}
			else if (tagName.equals("invoke"))
			{
				int closeTag = findClosingTag(html, "invoke", p);
				if (closeTag<0)
				{
					throw new Exception("Missing </invoke>");
				}

				Map<String, String> attrs = getAttributes(html.substring(p, closeBracket));
				String methodAttr = attrs.get("method");
				if (Util.isEmpty(methodAttr))
				{
					throw new Exception("Missing invoke method attribute");
				}

				Map<String, String> params = new HashMap<String, String>();
				
				String inside = html.substring(closeBracket+1, closeTag);
				int pPrm = 0;
				int qPrm = 0;
				while (true)
				{
					pPrm = inside.indexOf("<param name=", qPrm);
					if (pPrm<0) break;
					pPrm += "<param name=".length();
					qPrm = inside.indexOf(">", pPrm);
					if (qPrm<0)
					{
						throw new Exception("No closing bracket");
					}
					String pName = inside.substring(pPrm, qPrm);
					
					pPrm = qPrm+1;
					qPrm = inside.indexOf("</param>", pPrm);
					if (qPrm<0)
					{
						throw new Exception("Missing </param>");
					}
					String pVal = inside.substring(pPrm, qPrm);
					pVal = processNoWrite(pVal);
					
					params.put(pName, pVal);
				}
				
				doInvoke(methodAttr, params);
				
				q = closeTag + "</invoke>".length();
			}
			else
			{
				write("<");
				q = p + 1;
			}
		}
	}
	
	private Map<String, String> getAttributes(String tag)
	{
		Map<String, String> result = new HashMap<String, String>();
		StringTokenizer tokens = new StringTokenizer(tag, " ");
		while (tokens.hasMoreTokens())
		{
			String token = tokens.nextToken();
			int p = token.indexOf("=");
			if (p>=0)
			{
				String name = token.substring(0, p);
				String val = token.substring(p+1);
				if (val.startsWith("\"") || val.startsWith("\'"))
				{
					val = val.substring(1);
				}
				if (val.endsWith("\"") || val.endsWith("\'"))
				{
					val = val.substring(0, val.length()-1);
				}
				result.put(name, val);
			}
		}
		return result;
	}
	
	private int findClosingTag(String html, String tagName, int tagPos)
	{
		int open = tagPos;
		int close = html.indexOf("</" + tagName, tagPos);
		if (close<0) return close;

		int openCount = 1;
		int closeCount = 1;
		
		while (true)
		{
			open = html.indexOf("<" + tagName, open+1+tagName.length());
			if (open>0 && open<close)
			{
				openCount++;
			}
			
			if (openCount==closeCount)
			{
				return close;
			}
			
			close = html.indexOf("</" + tagName, close+2+tagName.length());
			if (close>0)
			{
				closeCount ++;
			}
			else
			{
				return -1;
			}
		}
	}
	
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	
	protected void doInvoke(String method, Map<String, String> params) throws Exception
	{
//		if (Debug.ON==false) return;
//		
//		writeEncode(method);
//		write("(");
//		int i = 0;
//		for (String n : params.keySet())
//		{
//			if (i>0) write(", ");
//			
//			String v = params.get(n);
//			writeEncode(n);
//			write("=");
//			writeEncode(v);
//			
//			i++;
//		}
//		write(")");
		
		if (method.equalsIgnoreCase("appOwner"))
		{
			writeEncode(Setup.getAppOwner(getLocale()));
		}
		else if (method.equalsIgnoreCase("appTitle"))
		{
			writeEncode(Setup.getAppTitle(getLocale()));
		}
		else if (method.equalsIgnoreCase("appAddress"))
		{
			write(Util.textToHtml(Setup.getAppAddress(getLocale())));
		}
	}

	protected boolean doSwitch(String flag)
	{
		return false;
	}

	protected String getFileName()
	{
		return getContext().getCommand();
	}
}
