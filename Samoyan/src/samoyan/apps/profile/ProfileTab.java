package samoyan.apps.profile;

import samoyan.controls.NavTreeControl;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;

public class ProfileTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		navCtrl.addHeader(outputPage.getString("profile:Nav.Settings"));
		navCtrl.addPage(PersonalInfoPage.COMMAND, null);
		navCtrl.addPage(ContactInfoPage.COMMAND, null);
		navCtrl.addPage(AlertTimelinePage.COMMAND, null);
		
		navCtrl.addHeader(outputPage.getString("profile:Nav.Login"));
		navCtrl.addPage(ChangeLoginNamePage.COMMAND, null);
		navCtrl.addPage(ChangePasswordPage.COMMAND, null);
		navCtrl.addPage(CloseAccountPage.COMMAND, null);
		
		return navCtrl;
	}

	@Override
	public String getCommand()
	{
		return ProfilePage.COMMAND;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("profile:Nav.TabTitle");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "icons/standard/id-card-48.png";
	}
}
