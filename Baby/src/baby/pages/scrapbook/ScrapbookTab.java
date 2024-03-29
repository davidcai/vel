package baby.pages.scrapbook;

import baby.pages.BabyPage;
import samoyan.controls.NavTreeControl;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;

public class ScrapbookTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		navCtrl.addHeader(outputPage.getString("scrapbook:Nav.Scrapbook"));
		navCtrl.addPage(JournalPage.COMMAND, null);
		navCtrl.addPage(GalleryPage.COMMAND, null);
		navCtrl.addPage(ChartsPage.COMMAND, null);
		
		return navCtrl;
	}
	
	@Override
	public String getCommand()
	{
		return BabyPage.COMMAND_SCRAPBOOK;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("scrapbook:Nav.TabTitle");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		if (outputPage.getContext().getUserAgent().isSmartPhone()==false)
		{
			// Icon only on smartphone
			return null;
		}

		return "baby/tab-scrapbook.png";
	}
}
