package baby.pages.content;

import samoyan.controls.NavTreeControl;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;
import baby.pages.BabyPage;

public class ContentTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		navCtrl.addHeader(outputPage.getString("content:Nav.Content"));
		navCtrl.addPage(ArticleListPage.COMMAND, null);
		navCtrl.addPage(MeasureListPage.COMMAND, null);
		navCtrl.addPage(ChecklistListPage.COMMAND, null);
		navCtrl.addPage(DeleteAllPage.COMMAND, null);
		
		return navCtrl;
	}
	
	@Override
	public String getCommand()
	{
		return BabyPage.COMMAND_CONTENT;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("content:Nav.TabTitle");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "baby/tab-content.png";
	}
}
