package elert.pages.govern;

import elert.pages.ElertPage;

public final class LastActivityReportPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_GOVERN + "/last-activity";
	
	public LastActivityReportPage()
	{
		setChild(new samoyan.apps.admin.reports.LastActivityReportPage());
	}
}
