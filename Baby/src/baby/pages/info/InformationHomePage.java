package baby.pages.info;

import samoyan.core.Util;
import samoyan.servlet.exc.RedirectException;
import baby.controls.StageTitleControl;
import baby.pages.BabyPage;

public class InformationHomePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION;
	
	@Override
	public void renderHTML() throws Exception
	{
		if (getContext().getUserAgent().isSmartPhone()==false)
		{
			throw new RedirectException(ChecklistPage.COMMAND, null);
		}
		
		StageTitleControl stgTtl = new StageTitleControl();
		stgTtl.setContainer(this);
		stgTtl.renderHTML();
		
		write("<div class=Subtabs>");
		
		write(Util.textToHtml(getString("information:Home.Prompt")));
		write("<br>");
		
		String[] icons = {"baby/subtab-checklists.jpg", "baby/subtab-appointments.jpg", "baby/subtab-calendar.jpg", "baby/subtab-reading.jpg", "baby/subtab-resources.jpg"};
		String[] labels = {"information:Checklist.Title", "information:AppointmentsList.Title", "information:Calendar.Title", "information:Articles.Title", "information:Resources.Title"};
		String[] commands = {ChecklistPage.COMMAND, AppointmentsListPage.COMMAND, CalendarPage.COMMAND, ViewArticleListPage.COMMAND, ViewResourceListPage.COMMAND};
		
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
//			.setTitle(getString("information:Checklist.Title"))
//			.setURL(getPageURL(ChecklistPage.COMMAND));
//		
//		wlg.addLink()
//			.setTitle(getString("information:AppointmentsList.Title"))
//			.setURL(getPageURL(AppointmentsListPage.COMMAND));
//		
//		wlg.addLink()
//			.setTitle(getString("information:Calendar.Title"))
//			.setURL(getPageURL(CalendarPage.COMMAND));
//
//		wlg.addLink()
//			.setTitle(getString("information:Articles.Title"))
//			.setURL(getPageURL(ViewArticleListPage.COMMAND));
//
//		wlg.addLink()
//			.setTitle(getString("information:Resources.Title"))
//			.setURL(getPageURL(ViewResourceListPage.COMMAND));
//
//		wlg.render();

		// Replace H1 with the logo
		if (getContext().getUserAgent().isSmartPhone())
		{
			write("<style>H1{display:none;}</style>");
			write("<script>$('H1').after('<img src=\"");
			writeEncode(getResourceURL("baby/corner-logo-25.png"));
			write("\" height=25>');</script>");
		}
	}
		
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:Home.Title");
	}
}
