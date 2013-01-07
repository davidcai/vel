package samoyan.controls;

import java.util.Locale;

import samoyan.core.Util;
import samoyan.servlet.UserAgent;
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
	
	/**
	 * Specify an icon to represent the application or webpage on iOS.
	 * @param image114x114 The resource file name of the image. Must be 114x114 pixels.
	 * @param image144x144 The resource file name of the image. Must be 144x144 pixels.
	 * @param precomposed Tell iOS not to add shine to the image.
	 */
	public void appleTouchIcon(boolean precomposed, String image114x114, String image144x144)
	{
		UserAgent ua = outputPage.getContext().getUserAgent();
		if (ua.isAppleTouch() || ua.isAndroid()) // Also Android asks for this icon
		{
			if (ua.getPixelRatio()==2)
			{
				if (image114x114!=null)
				{
					buffer.append("<link rel=\"apple-touch-icon");
					if (precomposed)
					{
						buffer.append("-precomposed");
					}
					buffer.append("\" sizes=\"114x114\" href=\"");
					buffer.append(outputPage.getResourceURL(image114x114));
					buffer.append("\">");
				}
				if (image144x144!=null)
				{
					buffer.append("<link rel=\"apple-touch-icon");
					if (precomposed)
					{
						buffer.append("-precomposed");
					}
					buffer.append("\" sizes=\"144x144\" href=\"");
					buffer.append(outputPage.getResourceURL(image144x144));
					buffer.append("\">");
				}
			}
			else
			{
				if (image114x114!=null)
				{
					buffer.append("<link rel=\"apple-touch-icon");
					if (precomposed)
					{
						buffer.append("-precomposed");
					}
					buffer.append("\" sizes=\"57x57\" href=\"");
					buffer.append(outputPage.getResourceURL(image114x114));
					buffer.append("\">");
				}
				if (image144x144!=null)
				{
					buffer.append("<link rel=\"apple-touch-icon");
					if (precomposed)
					{
						buffer.append("-precomposed");
					}
					buffer.append("\" sizes=\"72x72\" href=\"");
					buffer.append(outputPage.getResourceURL(image144x144));
					buffer.append("\">");
				}
			}
		}
	}

	/**
	 * Specify a startup image that is displayed while the application launches on an iPhone or iPod.<br>
	 * @param image320x460 The resource file name of the image. Must be 320x460 pixels.
	 * @param image640x920 The resource file name of the image. Must be 640x920 pixels.
	 * @param image640x1096 The resource file name of the image. Must be 640x1096 pixels.
	 * @param image768x1004 The resource file name of the image. Must be 768x1004 pixels.
	 * @param image1536x2008 The resource file name of the image. Must be 1536x2008 pixels.
	 * @param image
	 */
	public void appleTouchStartupImage(String image320x460, String image640x920, String image640x1096, String image768x1004, String image1536x2008)
	{
		UserAgent ua = outputPage.getContext().getUserAgent();
		if (ua.isIPhone() || ua.isIPod())
		{
			if (ua.getPixelRatio()==2 && ua.getScreenHeight()==1136/2)
			{
				// iPhone 5 with retina
				if (image640x1096!=null)
				{
					buffer.append("<link rel=\"apple-touch-startup-image\" href=\"");
					buffer.append(outputPage.getResourceURL(image640x1096));
					buffer.append("\">");
				}
				else if (image640x920!=null)
				{
					buffer.append("<link rel=\"apple-touch-startup-image\" href=\"");
					buffer.append(outputPage.getResourceURL(image640x920));
					buffer.append("\">");
				}
				else if (image320x460!=null)
				{
					buffer.append("<link rel=\"apple-touch-startup-image\" href=\"");
					buffer.append(outputPage.getResourceURL(image320x460));
					buffer.append("\">");
				}
			}
			else if (ua.getPixelRatio()==2 && ua.getScreenHeight()==960/2)
			{
				// iPhone 4 with retina
				if (image640x920!=null)
				{
					buffer.append("<link rel=\"apple-touch-startup-image\" href=\"");
					buffer.append(outputPage.getResourceURL(image640x920));
					buffer.append("\">");
				}
				else if (image320x460!=null)
				{
					buffer.append("<link rel=\"apple-touch-startup-image\" href=\"");
					buffer.append(outputPage.getResourceURL(image320x460));
					buffer.append("\">");
				}
			}
			else
			{
				// Defaut to iPhone 3 or earlier
				if (image320x460!=null)
				{
					buffer.append("<link rel=\"apple-touch-startup-image\" href=\"");
					buffer.append(outputPage.getResourceURL(image320x460));
					buffer.append("\">");
				}
			}
		}
		else if (ua.isIPad())
		{
			if (ua.getPixelRatio()==2)
			{
				// iPad with retina
				if (image1536x2008!=null)
				{
					buffer.append("<link rel=\"apple-touch-startup-image\" href=\"");
					buffer.append(outputPage.getResourceURL(image1536x2008));
					buffer.append("\">");
				}
				else if (image768x1004!=null)
				{
					buffer.append("<link rel=\"apple-touch-startup-image\" href=\"");
					buffer.append(outputPage.getResourceURL(image768x1004));
					buffer.append("\">");
				}
			}
			else
			{
				// iPad 2 or iPad mini
				if (image768x1004!=null)
				{
					buffer.append("<link rel=\"apple-touch-startup-image\" href=\"");
					buffer.append(outputPage.getResourceURL(image768x1004));
					buffer.append("\">");
				}
			}
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
		UserAgent ua = outputPage.getContext().getUserAgent();
		if (ua.isMSIE())
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
		UserAgent ua = outputPage.getContext().getUserAgent();
		if (ua.isSmartPhone())
		{
			buffer.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0,user-scalable=no\" />");
		}
	}
	
	/**
	 * When you use this standalone mode, Safari is not used to display the web content-specifically, there is no browser URL
	 * text field at the top of the screen or button bar at the bottom of the screen. Only a status bar appears at the top of the screen.
	 */
	public void appleMobileWebAppCapable()
	{
		UserAgent ua = outputPage.getContext().getUserAgent();
		if (ua.isAppleTouch())
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
		UserAgent ua = outputPage.getContext().getUserAgent();
		if (ua.isAppleTouch())
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
