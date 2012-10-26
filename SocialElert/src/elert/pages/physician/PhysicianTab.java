package elert.pages.physician;

import samoyan.controls.NavTreeControl;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;
import elert.pages.ElertPage;

public class PhysicianTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);

		navCtrl.addHeader(outputPage.getString("physician:Nav.Patients"));
		navCtrl.addPage(UpcomingPatientsPage.COMMAND, null);

		return navCtrl;
	}
	
	@Override
	public String getCommand()
	{
		return ElertPage.COMMAND_PHYSICIAN;
	}
	
	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("physician:Tab.Title");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		// TODO: Add icon for the Physician tab
		return super.getIcon(outputPage);
	}
}
