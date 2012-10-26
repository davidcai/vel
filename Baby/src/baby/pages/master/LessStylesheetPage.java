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
		return "#929FD0";
	}
	
	@Override
	protected String getColorLink()
	{
		return "#472F92";
	}
	
	@Override
	protected String getColorHover()
	{
//		// return "#922F2F"; // Dark red
//		return "#337336"; // Green
		return "#DE3BBA"; // Baby purple/pink
	}
	
	@Override
	protected String getColorNegativeHover()
	{
		return "#DE3BBA"; // Baby purple/pink
	}
	
	@Override
	protected String getColorBackgroundDark()
	{
		return "#EAEBF6";
	}
	
	@Override
	protected String getColorAccent()
	{
		return "#929FD0";
	}	
}
