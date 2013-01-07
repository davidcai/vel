package mind.pages.patient.reminders;

import samoyan.controls.NavTreeControl;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;

public class RemindersTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		navCtrl.addHeader(outputPage.getString("mind:Nav.Reminders"));
		navCtrl.addPage(PrescriptionListPage.COMMAND, null);
		navCtrl.addPage(EditPrescriptionPage.COMMAND, null);
		navCtrl.addPage(DoseListPage.COMMAND, null);
		
		return navCtrl;
	}

	@Override
	public String getCommand()
	{
		return RemindersPage.COMMAND;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("mind:Tab.Reminders");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "icons/standard/alarm-clock-48.png";
	}
}
