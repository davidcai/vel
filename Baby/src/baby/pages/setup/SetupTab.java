package baby.pages.setup;

import samoyan.apps.guidedsetup.GuidedSetupTab;
import samoyan.servlet.WebPage;

public class SetupTab extends GuidedSetupTab
{
	@Override
	public String getIcon(WebPage outputPage)
	{
		if (outputPage.getContext().getUserAgent().isSmartPhone())
		{
			// Same as master tab
			return "baby/corner-logo-60.png";
		}
		else
		{
			// Do not show icon in tab bar
			return null;
		}
	}
	
	@Override
	public String getLabel(WebPage outputPage)
	{
		return null;
	}
}
