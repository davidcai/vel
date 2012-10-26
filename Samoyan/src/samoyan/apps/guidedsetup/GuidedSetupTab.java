package samoyan.apps.guidedsetup;

import java.util.List;

import samoyan.controls.NavTreeControl;
import samoyan.controls.NavTreeControl.NavTreeEntry;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.RequestContext;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.WebPage;

public class GuidedSetupTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		RequestContext ctx = outputPage.getContext();
		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		User user = UserStore.getInstance().load(ctx.getUserID());
		if (user!=null)
		{
			List<String> pages = user.getGuidedSetupPages();
			if (pages!=null)
			{
				navCtrl.addHeader(outputPage.getString("guidedsetup:Nav.GuidedSetup"));
				
				int currentStep = user.getGuidedSetupStep();
				for (int p=0; p<pages.size(); p++)
				{
					NavTreeEntry nte = navCtrl.addPage(UrlGenerator.COMMAND_SETUP + "/" + pages.get(p), null);
					if (p>currentStep)
					{
						// Disable the links of future steps
						nte.setURL(null);
					}
				}
			}
		}
		
		return navCtrl;
	}

	@Override
	public String getCommand()
	{
		return UrlGenerator.COMMAND_SETUP;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("guidedsetup:Nav.TabTitle");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "tab-setup.png";
	}
}
