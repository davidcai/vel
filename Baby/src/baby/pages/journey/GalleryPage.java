package baby.pages.journey;

import samoyan.controls.TabControl;
import baby.pages.BabyPage;

public class GalleryPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY + "/gallery";
	
	@Override
	public void renderHTML() throws Exception
	{
		// Horizontal nav bar
		if (getContext().getUserAgent().isSmartPhone())
		{
			new TabControl(this)
				.addTab(JournalPage.COMMAND, getString("journey:Journal.Title"), getPageURL(JournalPage.COMMAND))
				.addTab(GalleryPage.COMMAND, getString("journey:Gallery.Title"), getPageURL(GalleryPage.COMMAND))
				.addTab(ChartsPage.COMMAND, getString("journey:Charts.Title"), getPageURL(ChartsPage.COMMAND))
				.setCurrentTab(getContext().getCommand())
				.setStyleButton()
				.setAlignStretch()
				.render();
		}
		
		writeEncode("Gallery");
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("journey:Gallery.Title");
	}
}
