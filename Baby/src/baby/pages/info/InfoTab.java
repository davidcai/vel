package baby.pages.info;

import samoyan.controls.NavTreeControl;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;
import baby.pages.BabyPage;

public class InfoTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);

		navCtrl.addHeader(outputPage.getString("information:Nav.ToDo"));
		navCtrl.addPage(ChecklistPage.COMMAND, null);
		navCtrl.addPage(AppointmentsListPage.COMMAND, null);
		navCtrl.addPage(CalendarPage.COMMAND, null);
		
		navCtrl.addHeader(outputPage.getString("information:Nav.Articles"));
		navCtrl.addPage(ViewArticleListPage.COMMAND, null);
		navCtrl.addPage(ViewResourceListPage.COMMAND, null);
		
		return navCtrl;
	}
	
	@Override
	public String getCommand()
	{
		return BabyPage.COMMAND_INFORMATION;
	}

	@Override
	public String getLabel(WebPage outputPage) throws Exception
	{
		return outputPage.getString("information:Nav.TabTitle");
	}

	@Override
	public String getIcon(WebPage outputPage) throws Exception
	{
		if (outputPage.getContext().getUserAgent().isSmartPhone()==false)
		{
			// Icon only on smartphone
			return null;
		}
		
		return "baby/tab-pregnancy.png";
	}
}
