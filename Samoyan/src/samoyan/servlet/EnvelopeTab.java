package samoyan.servlet;

import samoyan.controls.NavTreeControl;
import samoyan.core.Util;

public class EnvelopeTab
{
	/**
	 * To be overridden by subclass to return the navigation tree control to be rendered in the nav area of the page.
	 * @return
	 */
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		return null;
	}
	
	/**
	 * To be overridden by subclass to return the command prefix of this tab, e.g. "admin".
	 * @return
	 */
	public String getCommand()
	{
		return null;
	}
	
	public String getLabel(WebPage outputPage) throws Exception
	{
		return null;
	}
	
	public String getIcon(WebPage outputPage) throws Exception
	{
		return Util.isEmpty(getCommand())? null : "icons/standard/gold-star-48.png";
	}	
}
