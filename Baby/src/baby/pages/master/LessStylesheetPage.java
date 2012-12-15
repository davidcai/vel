package baby.pages.master;

/**
 * Colors taken from Healthy Beginnings PDF.
 * @author brian
 *
 */
public final class LessStylesheetPage extends samoyan.apps.master.LessStylesheetPage
{
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
