package samoyan.controls;

import java.util.Locale;

import samoyan.core.Util;
import samoyan.servlet.WebPage;

public class MetaTagControl
{
	private StringBuffer buffer;
	private WebPage outputPage;
	
	public MetaTagControl(WebPage outputPage)
	{
		this.buffer = new StringBuffer();
		this.outputPage = outputPage;
	}

	public void contentType()
	{
		buffer.append("<meta http-equiv=\"Content-type\" content=\"text/html; charset=UTF-8\">");
	}
	
	public void copyright(String owner)
	{
		buffer.append("<meta name=\"copyright\" content=\"");
		buffer.append(Util.htmlEncode(owner));
		buffer.append("\">");
	}
	
	public void robots()
	{
		int flags = 0;
		try
		{
			flags = outputPage.getXRobotFlags();
		}
		catch (Exception e)
		{
			// Not mission critical
		}
		
		if (flags!=0)
		{
			buffer.append("<meta name=\"robots\" content=\"");
			boolean comma=false;
			if ((flags & WebPage.NO_INDEX) !=0)
			{
				buffer.append("noindex");
				comma = true;
			}
			if ((flags & WebPage.NO_FOLLOW) !=0)
			{
				if (comma) buffer.append(",");
				buffer.append("nofollow");
				comma = true;
			}
			if ((flags & WebPage.NO_ARCHIVE) !=0)
			{
				if (comma) buffer.append(",");
				buffer.append("noarchive");
				comma = true;
			}
			if ((flags & WebPage.NO_SNIPPET) !=0)
			{
				if (comma) buffer.append(",");
				buffer.append("nosnippet");
				comma = true;
			}
			buffer.append("\">");
		}
	}
	
	public void favicon(String image)
	{
		buffer.append("<link rel=\"");
		if (outputPage.getContext().getUserAgent().isMSIE())
		{
			buffer.append("shortcut ");
		}
		buffer.append("icon\" href=\"");
		buffer.append(outputPage.getResourceURL(image));
		buffer.append("\">");
	}
	
	public void thumbnail(String image)
	{
		buffer.append("<link rel=\"image_src\" href=\"");
		buffer.append(outputPage.getResourceURL(image));
		buffer.append("\"/>");
	}
	
	public void description(String desc)
	{
		buffer.append("<meta name=\"description\" content=\"");
		buffer.append(Util.htmlEncode(Util.getTextAbstract(desc, 160)));
		buffer.append("\">");
	}
	
	public void keywords(String keywords)
	{
		buffer.append("<meta name=\"keywords\" content=\"");
		buffer.append(Util.htmlEncode(keywords));
		buffer.append("\">");
	}

	public void appleTouchIcon(String image)
	{
		if (outputPage.getContext().getUserAgent().isAppleTouch())
		{
			buffer.append("<link rel=\"apple-touch-icon\" href=\"");
			buffer.append(outputPage.getResourceURL(image));
			buffer.append("\">");
		}
	}
	
	public void appleTouchStartupImage(String image)
	{
		if (outputPage.getContext().getUserAgent().isAppleTouch())
		{
			buffer.append("<link rel=\"apple-touch-startup-image\" href=\"");
			buffer.append(outputPage.getResourceURL(image));
			buffer.append("\">");
		}
	}
	
	public void language(Locale loc)
	{
		buffer.append("<meta http-equiv=\"Content-Language\" content=\"");
		buffer.append(Util.strReplace(loc.toString(), "_", "-"));
		buffer.append("\">");
	}
	
	public void disableIEImageToolbar()
	{
		if (outputPage.getContext().getUserAgent().isMSIE())
		{
			buffer.append("<meta http-equiv=\"imagetoolbar\" content=\"no\">");
		}
	}
	
	public void disableMicrosoftSmartTags()
	{
		if (outputPage.getContext().getUserAgent().isMSIE())
		{
			buffer.append("<meta name=MSSmartTagsPreventParsing content=true>");
		}
	}
	
	public void disableSkypeToolbar()
	{
		buffer.append("<meta name=\"SKYPE_TOOLBAR\" content=\"SKYPE_TOOLBAR_PARSER_COMPATIBLE\">");
	}
	
	public void viewportNoScale()
	{
		if (outputPage.getContext().getUserAgent().isSmartPhone())
		{
			buffer.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0,user-scalable=no\" />");
		}
	}
	
	public void appleMobileWebAppCapable()
	{
		if (outputPage.getContext().getUserAgent().isAppleTouch())
		{
			buffer.append("<meta name=\"apple-mobile-web-app-capable\" content=\"yes\">");
		}
	}
	
	/**
	 * Minimize the status bar that is displayed at the top of the screen on iOS.
	 * @see http://developer.apple.com/library/IOs/#documentation/AppleApplications/Reference/SafariWebContent/ConfiguringWebApplications/ConfiguringWebApplications.html
	 */
	public void appleMobileWebAppStatusBarStyleBlack()
	{
		if (outputPage.getContext().getUserAgent().isAppleTouch())
		{
			buffer.append("<meta name=\"apple-mobile-web-app-status-bar-style\" content=\"black\">");
		}
	}

	// For more meta-tags, see http://www.quotes.uk.com/web-design/meta-tags.php 
	
	public void render()
	{
		outputPage.write(buffer);
	}
}
