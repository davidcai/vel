package baby.pages.profile;

import samoyan.apps.profile.ProfileTab;
import samoyan.controls.NavTreeControl;
import samoyan.servlet.WebPage;

public class BabyProfileTab extends ProfileTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		navCtrl.addHeader(outputPage.getString("babyprofile:Nav.Settings"));
		navCtrl.addPage(ConsolidatedProfilePage.COMMAND, null);
		navCtrl.addPage(StagePage.COMMAND, null);
		navCtrl.addPage(BabiesPage.COMMAND, null);
						
		return navCtrl;
	}
}
