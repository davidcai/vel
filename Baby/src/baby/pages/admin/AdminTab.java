package baby.pages.admin;

import samoyan.servlet.WebPage;

public class AdminTab extends samoyan.apps.admin.AdminTab
{
	@Override
	public String getIcon(WebPage outputPage)
	{
		if (outputPage.getContext().getUserAgent().isSmartPhone()==false)
		{
			// Icon only on smartphone
			return null;
		}

		return "baby/tab-admin.png";
	}
}
