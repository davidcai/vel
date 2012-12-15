package baby.pages.setup;

import samoyan.apps.guidedsetup.GuidedSetupTab;
import samoyan.servlet.WebPage;

public class SetupTab extends GuidedSetupTab
{
	@Override
	public String getIcon(WebPage outputPage)
	{
		return "baby/tab-setup.png";
	}
}
