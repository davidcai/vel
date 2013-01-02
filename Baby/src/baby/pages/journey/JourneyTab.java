package baby.pages.journey;

import samoyan.controls.NavTreeControl;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;
import baby.pages.BabyPage;

public class JourneyTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		navCtrl.addHeader(outputPage.getString("journey:Nav.Journey"));
		navCtrl.addPage(JournalPage.COMMAND, null);
//		navCtrl.addPage(GalleryPage.COMMAND, null);
//		navCtrl.addPage(ChartsPage.COMMAND, null);
		
		return navCtrl;
	}
	
	@Override
	public String getCommand()
	{
		return BabyPage.COMMAND_JOURNEY;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("journey:Nav.TabTitle");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "baby/tab-journey.png";
	}
}
