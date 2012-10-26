package elert.pages.govern;

import elert.pages.ElertPage;

public final class JoinReportPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_GOVERN + "/join-report";
	
	public JoinReportPage()
	{
		setChild(new samoyan.apps.admin.reports.JoinReportPage());
	}
}
