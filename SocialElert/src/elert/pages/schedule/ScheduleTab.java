package elert.pages.schedule;

import elert.pages.ElertPage;
import samoyan.controls.NavTreeControl;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;

public final class ScheduleTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);

		navCtrl.addHeader(outputPage.getString("schedule:Nav.Elerts"));
		navCtrl.addPage(UnresolvedOpeningsPage.COMMAND, null);
		navCtrl.addPage(LogNewOpeningPage.COMMAND, null);
		navCtrl.addPage(CalendarPage.COMMAND, null);

		navCtrl.addHeader(outputPage.getString("schedule:Nav.Patients"));
		navCtrl.addPage(RecentSubscriptionsPage.COMMAND, null);
		navCtrl.addPage(SearchPatientsPage.COMMAND, null);

		navCtrl.addHeader(outputPage.getString("schedule:Nav.Reports"));
		navCtrl.addPage(OpeningsReportPage.COMMAND, null);		
		navCtrl.addPage(OutgoingReportPage.COMMAND, null);		
		navCtrl.addPage(IncomingReportPage.COMMAND, null);		

		navCtrl.addHeader(outputPage.getString("schedule:Nav.DataEntry"));
		navCtrl.addPage(HomeServiceAreasPage.COMMAND, null);
		navCtrl.addPage(ProceduresPage.COMMAND_STANDARD, null);
		navCtrl.addPage(PhysiciansPage.COMMAND, null);

		return navCtrl;
	}

	@Override
	public String getCommand()
	{
		return ElertPage.COMMAND_SCHEDULE;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("schedule:Tab.Title");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "elert/tab-schedule.png";
	}
}
