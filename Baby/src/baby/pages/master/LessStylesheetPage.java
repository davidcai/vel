package baby.pages.master;

/**
 * Colors taken from Healthy Beginnings PDF.
 * @author brian
 *
 */
public final class LessStylesheetPage extends samoyan.apps.master.LessStylesheetPage
{
	@Override
	protected void renderPrologue() throws Exception
	{
		writeLESSVar("color-secondary-accent", "#929fd1"); // Healthy Beginnings purple
	}
	
	@Override
	protected String getFontNormal()
	{
		if (getContext().getUserAgent().isSmartPhone())
		{
			return "12pt Helvetica Neue,Helvetica,Arial Narrow,Arial,Sans-Serif";
		}
		else
		{
			return "12pt Arial,Sans-Serif";
		}
	}
	protected String getFontSmall()
	{
		if (getContext().getUserAgent().isSmartPhone())
		{
			return "10pt Helvetica Neue,Helvetica,Arial Narrow,Arial,Sans-Serif";
		}
		else
		{
			return "10pt Arial,Sans-Serif";
		}
	}

	@Override
	protected String getColorNegativeBackground()
	{
//		return "#929FD0"; // Light blue
//		return "#B2D234"; // Lime green
//		return "#006D9D"; // KP Blue (KP approved color)
		return "#52ABD5"; // KP light blue (KP approved color)
	}
	
	@Override
	protected String getColorLink()
	{
//		return "#472F92"; // Blue
//		return "#154365"; // Dark blue
		return "#153D6F"; // Accent sky blue (KP approved color)
	}
	
	@Override
	protected String getColorHover()
	{
//		return "#DE3BBA"; // Baby purple/pink
		return "#F57D20"; // Orange
	}
	
	@Override
	protected String getColorNegativeHover()
	{
//		return "#DE3BBA"; // Baby purple/pink
		return "#F57D20"; // Orange
	}
	
	@Override
	protected String getColorBackgroundDark()
	{
		return "#EAEBF6"; // Light gray
	}
	
	@Override
	protected String getColorAccent()
	{
//		return "#929FD0"; // Light blue
//		return "#B2D234"; // Lime green
		return "#006D9D"; // KP Blue (KP approved color)
	}	
}
