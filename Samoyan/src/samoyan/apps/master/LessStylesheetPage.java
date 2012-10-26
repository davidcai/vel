package samoyan.apps.master;

import java.io.InputStream;

import samoyan.core.Util;
import samoyan.servlet.Controller;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.PageNotFoundException;

public class LessStylesheetPage extends WebPage
{
	public final static String COMMAND = "*.less";

	protected void writeLESSVar(String name, String val)
	{
		write("@");
		write(name);
		write(":");
		write(val);
		write(";\r\n");
	}
	
	@Override
	public final void renderHTML() throws Exception
	{
		writeLESSVar("color-text", getColorText());
		writeLESSVar("color-text-light", getColorTextLight());
		writeLESSVar("color-background", getColorBackground());
		writeLESSVar("color-background-dark", getColorBackgroundDark());
		writeLESSVar("color-background-editbox", getColorBackgroundEditbox());
		writeLESSVar("color-background-editbox-error", getColorBackgroundEditboxError());
		writeLESSVar("color-link", getColorLink());
		writeLESSVar("color-hover", getColorHover());
		writeLESSVar("color-accent", getColorAccent());
		writeLESSVar("color-accent-alert", getColorAccentAlert());
		writeLESSVar("color-error", getColorError());
		writeLESSVar("color-background-warning", getColorBackgroundWarning());

		writeLESSVar("color-negative-text", getColorNegativeText());
		writeLESSVar("color-negative-background", getColorNegativeBackground());
		writeLESSVar("color-negative-link", getColorNegativeLink());
		writeLESSVar("color-negative-hover", getColorNegativeHover());
		writeLESSVar("color-negative-accent", getColorNegativeAccent());
		
		writeLESSVar("font-huge", getFontHuge());
		writeLESSVar("font-large", getFontLarge());
		writeLESSVar("font-normal", getFontNormal());
		writeLESSVar("font-small", getFontSmall());
		
		writeLESSVar("header-height", getHeaderHeight());
		writeLESSVar("footer-height", getFooterHeight());

		write("\r\n");
		renderPrologue();
		write("\r\n\r\n");

		InputStream stm = Controller.getResourceAsStream(getContext().getCommand());
		if (stm==null)
		{
			throw new PageNotFoundException();
		}
		write(Util.inputStreamToString(stm, "UTF-8"));
		
		write("\r\n\r\n");
		renderEpilogue();
	}
	
	@Override
	public final String getMimeType() throws Exception
	{
		return "text/less";
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
	public final boolean isActionable() throws Exception
	{
		return false;
	}
	
	@Override
	public final boolean isCacheable() throws Exception
	{
		return true;
	}

	/**
	 * Can be overridden by subclasses to write at the end of the file.
	 */
	protected void renderEpilogue() throws Exception
	{
	}
	
	/**
	 * Can be overridden by subclasses to write at the beginning of the file.
	 */
	protected void renderPrologue() throws Exception
	{
	}

	protected String getColorText()
	{
		return "#333333";
	}
	protected String getColorTextLight()
	{
		return "#999999";
	}
	protected String getColorBackground()
	{
		return "#ffffff";
	}
	protected String getColorBackgroundDark()
	{
		return "#cccccc";
	}
	protected String getColorBackgroundEditbox()
	{
		return "#eeeeee";
	}
	protected String getColorBackgroundEditboxError()
	{
		return "#FFBA8E";
	}
	protected String getColorLink()
	{
		return "#006D9D";
	}
	protected String getColorHover()
	{
		return "#B74A29";
	}
	protected String getColorAccent()
	{
		return "#5F7D34";
	}
	protected String getColorAccentAlert()
	{
		return "#841F33";
	}
	protected String getColorError()
	{
		return "#dd2200";
	}
	protected String getColorBackgroundWarning()
	{
		return "#ffdd88";
	}
	
	protected String getColorNegativeText()
	{
		return "#ffffff";
	}
	protected String getColorNegativeBackground()
	{
		return "#1e1e1e";
	}
	protected String getColorNegativeLink()
	{
		return "#ffffff";
	}
	protected String getColorNegativeHover()
	{
		return "#B74A29";
	}
	protected String getColorNegativeAccent()
	{
		return "#5F7D34";
	}
	
	protected String getFontHuge()
	{
		if (getContext().getUserAgent().isSmartPhone())
		{
			return "24pt Helvetica,Arial Narrow,Arial,Sans-Serif";
		}
		else
		{
			return "24pt Arial,Sans-Serif";
		}
	}
	protected String getFontLarge()
	{
		if (getContext().getUserAgent().isSmartPhone())
		{
			return "16pt Helvetica,Arial Narrow,Arial,Sans-Serif";
		}
		else
		{
			return "16pt Arial,Sans-Serif";
		}
	}
	protected String getFontNormal()
	{
		if (getContext().getUserAgent().isSmartPhone())
		{
			return "10pt Helvetica,Arial Narrow,Arial,Sans-Serif";
		}
		else
		{
			return "10pt Arial,Sans-Serif";
		}
	}
	protected String getFontSmall()
	{
		if (getContext().getUserAgent().isSmartPhone())
		{
			return "8pt Helvetica,Arial Narrow,Arial,Sans-Serif";
		}
		else
		{
			return "8pt Arial,Sans-Serif";
		}
	}
	
	protected String getFooterHeight()
	{
		return "60px";
	}

	protected String getHeaderHeight()
	{
		return "75px";
	}
}
