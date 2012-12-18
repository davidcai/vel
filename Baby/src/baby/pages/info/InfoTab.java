package baby.pages.info;

import baby.pages.BabyPage;
import samoyan.controls.NavTreeControl;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;

public class InfoTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);

		navCtrl.addHeader(outputPage.getString("information:Nav.ToDo"));
		navCtrl.addPage(ChecklistPage.COMMAND, null);
		navCtrl.addPage(CalendarPage.COMMAND, null);
		
		navCtrl.addHeader(outputPage.getString("information:Nav.Articles"));
		navCtrl.addPage(ViewArticleListPage.COMMAND, null);
		navCtrl.addPage(ViewResourceListPage.COMMAND, null);
		navCtrl.addPage(SearchPage.COMMAND, null);
		
		return navCtrl;
	}
	
	@Override
	public String getCommand()
	{
		return BabyPage.COMMAND_INFORMATION;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("information:Nav.TabTitle");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "baby/tab-info.png";
	}
}
