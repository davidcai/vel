package elert.pages.govern;

import elert.pages.ElertPage;
import samoyan.controls.NavTreeControl;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;

public class GovernTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		navCtrl.addHeader(outputPage.getString("govern:Nav.Reports"));
			navCtrl.addPage(OpeningsReportPage.COMMAND, null);		
			navCtrl.addPage(OutgoingReportPage.COMMAND, null);
			navCtrl.addPage(IncomingReportPage.COMMAND, null);
		
			navCtrl.addPage(JoinReportPage.COMMAND, null);		
			navCtrl.addPage(LastActivityReportPage.COMMAND, null);		
			navCtrl.addPage(CumulativeActivityReportPage.COMMAND, null);		
			navCtrl.addPage(AggregateTimelineReportPage.COMMAND, null);		

		navCtrl.addHeader(outputPage.getString("govern:Nav.DataEntry"));
			navCtrl.addPage(ServiceAreasPage.COMMAND, null);
			navCtrl.addPage(ProceduresPage.COMMAND, null);		
		
		return navCtrl;
	}

	@Override
	public String getCommand()
	{
		return ElertPage.COMMAND_GOVERN;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("govern:Tab.Title");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "elert/tab-govern.png";
	}
}
