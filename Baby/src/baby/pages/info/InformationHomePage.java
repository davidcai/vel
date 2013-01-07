package baby.pages.info;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.WideLinkGroupControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import baby.database.BabyStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.BabyPage;
import baby.pages.profile.StagePage;

public class InformationHomePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION;
	
	@Override
	public void renderHTML() throws Exception
	{
		writeStageInfo();
		
		WideLinkGroupControl wlg = new WideLinkGroupControl(this);
		
		wlg.addLink()
			.setTitle(getString("information:Checklist.Title"))
			.setURL(getPageURL(ChecklistPage.COMMAND));
		
		wlg.addLink()
			.setTitle(getString("information:AppointmentsList.Title"))
			.setURL(getPageURL(AppointmentsListPage.COMMAND));
		
		wlg.addLink()
			.setTitle(getString("information:Calendar.Title"))
			.setURL(getPageURL(CalendarPage.COMMAND));

		wlg.addLink()
			.setTitle(getString("information:Articles.Title"))
			.setURL(getPageURL(ViewArticleListPage.COMMAND));

		wlg.addLink()
			.setTitle(getString("information:Resources.Title"))
			.setURL(getPageURL(ViewResourceListPage.COMMAND));

		// !$! Temp code, should not be here.
		wlg.addLink()
			.setTitle(getString("information:Search.Title"))
			.setURL(getPageURL(SearchPage.COMMAND));

		wlg.render();

		// Replace H1 with the logo
		if (getContext().getUserAgent().isSmartPhone())
		{
			write("<style>H1{display:none;}</style>");
			write("<script>$('H1').after('<img src=\"");
			writeEncode(getResourceURL("baby/corner-logo-25.png"));
			write("\" height=25>');</script>");
		}
	}
	
	private void writeStageInfo() throws Exception
	{
		RequestContext ctx = getContext();
		Mother mother = MotherStore.getInstance().loadByUserID(ctx.getUserID());
		Stage stage = mother.getPregnancyStage();
		Date now = new Date();

		// Stage status
		String status = null;
		if (stage.isPreconception())
		{
			status = getString("information:Home.StatusPreconception");
		}
		else if (stage.isPregnancy())
		{
			Date due = mother.getDueDate();
			long days = (due.getTime() - now.getTime()) / (24L*60L*60L*1000L) + 1;
			if (days<=1L)
			{
				// Overdue
				status = getString("information:Home.StatusImminent");
			}
			else
			{
				status = getString("information:Home.StatusPregnancy", stage.getPregnancyWeek(), days);
			}
		}
		else if (stage.isInfancy())
		{
			String names = null;
			List<UUID> babyIDs = BabyStore.getInstance().getAtLeastOneBaby(ctx.getUserID());
			if (babyIDs.size()==1)
			{
				names = BabyStore.getInstance().load(babyIDs.get(0)).getName();
			}
			if (Util.isEmpty(names))
			{
				names = getString("information:Home.BabyCountName." + (babyIDs.size()<=8 ? babyIDs.size() : "N"));
			}
			
			Date birth = mother.getBirthDate();
			long weeks = (now.getTime() - birth.getTime()) / (7L*24L*60L*60L*1000L) + 1;
			if (weeks<=18)
			{
				status = getString("information:Home.StatusInfancyWeeks", names, babyIDs.size(), weeks);
			}
			else
			{
				status = getString("information:Home.StatusInfancyMonths", names, babyIDs.size(), stage.getInfancyMonth());
			}
		}
		write("<div align=center>");
		
		write("<h2>");
		writeEncode(status);
		write("</h2>");
		if (stage.isPreconception())
		{
			write("<small>");
			writeLink(getString("information:Home.AreYouPregnant"), getPageURL(StagePage.COMMAND, new ParameterMap(RequestContext.PARAM_GO_BACK_ON_SAVE, "")));
			write("</small><br><br>");
		}
		else if (stage.isPregnancy() && stage.getPregnancyWeek()>=35)
		{
			write("<small>");
			writeLink(getString("information:Home.DidYouGiveBirth"), getPageURL(StagePage.COMMAND, new ParameterMap(RequestContext.PARAM_GO_BACK_ON_SAVE, "")));
			write("</small><br><br>");
		}
		
		write("</div>");
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:Home.Title");
	}
}
