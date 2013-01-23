package baby.pages.journey;

import baby.controls.StageTitleControl;
import baby.pages.BabyPage;
import samoyan.core.Util;
import samoyan.servlet.exc.RedirectException;

public class JourneyHomePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY;
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("journey:Home.Title");
	}
	
	@Override
	public void render() throws Exception
	{
		if (getContext().getUserAgent().isSmartPhone()==false)
		{
			throw new RedirectException(JournalPage.COMMAND_LIST, null);
		}

		StageTitleControl stgTtl = new StageTitleControl();
		stgTtl.setContainer(this);
		stgTtl.renderHTML();

		write("<div class=Subtabs>");
		
		write(Util.textToHtml(getString("journey:Home.Prompt")));
		write("<br>");
		
		String[] icons = {"baby/subtab-journal.jpg", "baby/subtab-album.jpg", "baby/subtab-charts.jpg"};
		String[] labels = {"journey:Journal.Title", "journey:Gallery.Title", "journey:Charts.Title"};
		String[] commands = {JournalPage.COMMAND_LIST, GalleryPage.COMMAND, ChartsPage.COMMAND};
		
		for (int i=0; i<icons.length; i++)
		{
			write("<div>");
			writeImage(icons[i], getString(labels[i]), getPageURL(commands[i]));
			write("<br>");
			writeEncode(getString(labels[i]));
			write("</div>");
		}		
		write("<br>");
		write("</div>");

//		WideLinkGroupControl wlg = new WideLinkGroupControl(this);
//		
//		wlg.addLink()
//			.setTitle(getString("journey:Journal.Title"))
//			.setURL(getPageURL(JournalPage.COMMAND_LIST));
//		
//		wlg.addLink()
//			.setTitle(getString("journey:Gallery.Title"))
//			.setURL(getPageURL(GalleryPage.COMMAND));
//		
//		wlg.addLink()
//			.setTitle(getString("journey:Charts.Title"))
//			.setURL(getPageURL(ChartsPage.COMMAND));
//		
//		wlg.render();
	}
}
