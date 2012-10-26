package elert.pages.govern;

import elert.pages.ElertPage;

public final class CumulativeActivityReportPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_GOVERN + "/cumulative-activity";
	
	public CumulativeActivityReportPage()
	{
		setChild(new samoyan.apps.admin.reports.CumulativeActivityReportPage());
	}
}
