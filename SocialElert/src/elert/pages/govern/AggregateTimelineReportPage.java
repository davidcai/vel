package elert.pages.govern;

import elert.pages.ElertPage;

public final class AggregateTimelineReportPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_GOVERN + "/aggregate-timeline";
	
	public AggregateTimelineReportPage()
	{
		setChild(new samoyan.apps.admin.reports.AggregateTimelineReportPage());
	}
}
