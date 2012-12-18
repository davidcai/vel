package baby.pages.profile;

import samoyan.apps.profile.ChangeLoginNamePage;
import samoyan.apps.profile.ChangePasswordPage;
import samoyan.apps.profile.CloseAccountPage;
import samoyan.apps.profile.EmailPage;
import samoyan.apps.profile.MobilePage;
import samoyan.apps.profile.PhonePage;
import samoyan.apps.profile.ProfileTab;
import samoyan.apps.profile.RealNamePage;
import samoyan.apps.profile.TimeZonePage;
import samoyan.controls.NavTreeControl;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.Channel;
import samoyan.servlet.WebPage;

public class BabyProfileTab extends ProfileTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();

		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		navCtrl.addHeader(outputPage.getString("babyprofile:Nav.Pregnancy"));
		navCtrl.addPage(StagePage.COMMAND, null);
		navCtrl.addPage(BabiesPage.COMMAND, null);
		navCtrl.addPage(MedicalCenterPage.COMMAND, null);
		
		navCtrl.addHeader(outputPage.getString("babyprofile:Nav.Account"));
		navCtrl.addPage(RealNamePage.COMMAND, null);
		navCtrl.addPage(ChangeLoginNamePage.COMMAND, null);
		navCtrl.addPage(ChangePasswordPage.COMMAND, null);
		navCtrl.addPage(CloseAccountPage.COMMAND, null);
		
		navCtrl.addHeader(outputPage.getString("babyprofile:Nav.ContactInfo"));
		navCtrl.addPage(EmailPage.COMMAND, null);
		if (fed.isChannelEnabled(Channel.SMS))
		{
			navCtrl.addPage(MobilePage.COMMAND, null);
		}		
		if (fed.isChannelEnabled(Channel.VOICE))
		{
			navCtrl.addPage(PhonePage.COMMAND, null);
		}		
		
		navCtrl.addHeader(outputPage.getString("babyprofile:Nav.Preferences"));
		navCtrl.addPage(UnitsPage.COMMAND, null);
		navCtrl.addPage(TimeZonePage.COMMAND, null);

		return navCtrl;
	}
	
	@Override
	public String getIcon(WebPage outputPage)
	{
		return "baby/tab-profile.png";
	}
}
