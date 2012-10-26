package elert.pages.schedule;

import elert.pages.ElertPage;

public final class OpeningsReportPage extends ElertPage
{
	public static final String COMMAND = ElertPage.COMMAND_SCHEDULE + "/openings-report";
	
	public OpeningsReportPage()
	{
		setChild(new elert.pages.common.CommonOpeningsReportPage());
	}
}
