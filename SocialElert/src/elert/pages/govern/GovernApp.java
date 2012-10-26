package elert.pages.govern;

import samoyan.servlet.Dispatcher;

public class GovernApp
{
	public static void init()
	{
		Dispatcher.bindPage(GovernHomePage.COMMAND,					GovernHomePage.class);
		Dispatcher.bindPage(FacilityPage.COMMAND,					FacilityPage.class);
		Dispatcher.bindPage(FacilitiesPage.COMMAND,					FacilitiesPage.class);
		Dispatcher.bindPage(ProceduresPage.COMMAND,					ProceduresPage.class);
		Dispatcher.bindPage(ProcedurePage.COMMAND,					ProcedurePage.class);
		Dispatcher.bindPage(ServiceAreasPage.COMMAND,				ServiceAreasPage.class);
		Dispatcher.bindPage(ServiceAreaPage.COMMAND,				ServiceAreaPage.class);

		Dispatcher.bindPage(JoinReportPage.COMMAND,					JoinReportPage.class);
		Dispatcher.bindPage(LastActivityReportPage.COMMAND,			LastActivityReportPage.class);
		Dispatcher.bindPage(CumulativeActivityReportPage.COMMAND,	CumulativeActivityReportPage.class);
		Dispatcher.bindPage(AggregateTimelineReportPage.COMMAND,	AggregateTimelineReportPage.class);
		
		Dispatcher.bindPage(OpeningsReportPage.COMMAND,				OpeningsReportPage.class);
		Dispatcher.bindPage(OutgoingReportPage.COMMAND,				OutgoingReportPage.class);
		Dispatcher.bindPage(IncomingReportPage.COMMAND,				IncomingReportPage.class);
	}
}
