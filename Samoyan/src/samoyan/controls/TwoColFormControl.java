package samoyan.controls;

import java.io.ByteArrayOutputStream;

import samoyan.core.Util;
import samoyan.servlet.WebPage;

public class TwoColFormControl extends WebPage
{
	private String delayed = null;
	private boolean phone = false;
	private WebPage container = null;
	private ByteArrayOutputStream canvas = new ByteArrayOutputStream(512);
	
	public TwoColFormControl(WebPage outputPage)
	{
		setContainer(outputPage);
		
		this.phone = getContext().getUserAgent().isSmartPhone();
	}
	
	private void setDelayed(String content)
	{
		this.delayed = content;
	}
	private void writeDelayed()
	{
		if (this.delayed!=null)
		{
			write(this.delayed);
		}
		this.delayed = null;
	}

	@Override
	public ByteArrayOutputStream getCanvas()
	{
		return this.canvas;
	}

	// - - - - -
	
	@Override
	public void renderHTML() throws Exception
	{
		writeDelayed();
		
		WebPage outputPage = getContainer();
		outputPage.write("<table class=TwoColForm>");
		outputPage.writeBinary(this.canvas.toByteArray());
		outputPage.write("</table>");
	}
			
	public void writeTextRow(String text)
	{
		writeDelayed();
		
		write("<tr>");
		if (phone)
		{
			write("<td>");
		}
		else
		{
			write("<td colspan=2>");
		}
		writeEncode(text);
		write("</td></tr>");
	}
	
	public void writeSubtitleRow(String subtitle)
	{
		writeDelayed();
		
		write("<tr>");
		if (phone)
		{
			write("<td>");
		}
		else
		{
			write("<td colspan=2>");
		}
		write("<h2>");
		writeEncode(subtitle);
		write("</h2></td></tr>");
	}
	
	public void writeSpaceRow()
	{
		writeDelayed();
		
		write("<tr>");
		if (phone)
		{
			write("<td>");
		}
		else
		{
			write("<td colspan=2>");
		}
		write("&nbsp;</td></tr>");
	}
	
	public void writeRow(String title)
	{
		writeRow(title, null);
	}
	
	public void writeRow(String title, String help)
	{
		writeDelayed();
		
		write("<tr><th>");
		if (!Util.isEmpty(help) && !this.phone)
		{
			writeTooltip(title, help);
		}
		else
		{
			writeEncode(title);
		}
		write("</th>");
		if (this.phone)
		{
			write("</tr><tr>");
		}
		write("<td>");

		
		StringBuffer delayed = new StringBuffer();
		if (!Util.isEmpty(help) && this.phone)
		{
			delayed.append("<div class=Faded><small>");
			delayed.append(Util.htmlEncode(help));
			delayed.append("<small></div>");
		}
		delayed.append("</td></tr>");
		
		setDelayed(delayed.toString());
	}

	public void writeRow(WebPage page)
	{
		writeDelayed();
		
		write("<tr><th>");
		try
		{
			page.render();
		}
		catch (Exception e)
		{
		}
		write("</th>");
		if (this.phone)
		{
			write("</tr><tr>");
		}
		write("<td>");
		
		setDelayed("</td></tr>");
	}
}
