package baby.controls;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import baby.database.BabyStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.profile.StagePage;

public class StageTitleControl extends WebPage
{
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		Mother mother = MotherStore.getInstance().loadByUserID(ctx.getUserID());
		Stage stage = mother.getPregnancyStage();
		Date now = new Date();

		// Stage status
		String status = null;
		if (stage.isPreconception())
		{
			status = getString("baby:StageTitleCtrl.StatusPreconception");
		}
		else if (stage.isPregnancy())
		{
			Date due = mother.getDueDate();
			long days = (due.getTime() - now.getTime()) / (24L*60L*60L*1000L) + 1;
			if (days<=1L)
			{
				// Overdue
				status = getString("baby:StageTitleCtrl.StatusImminent");
			}
			else
			{
				status = getString("baby:StageTitleCtrl.StatusPregnancy", stage.getPregnancyWeek(), days);
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
				names = getString("baby:StageTitleCtrl.BabyCountName." + (babyIDs.size()<=8 ? babyIDs.size() : "N"));
			}
			
			Date birth = mother.getBirthDate();
			long weeks = (now.getTime() - birth.getTime()) / (7L*24L*60L*60L*1000L) + 1;
			if (weeks<=18)
			{
				status = getString("baby:StageTitleCtrl.StatusInfancyWeeks", names, babyIDs.size(), weeks);
			}
			else
			{
				status = getString("baby:StageTitleCtrl.StatusInfancyMonths", names, babyIDs.size(), stage.getInfancyMonth());
			}
		}
		write("<div align=center>");
		
		write("<h2>");
		writeEncode(status);
		write("</h2>");
		if (stage.isPreconception())
		{
			write("<small>");
			writeLink(getString("baby:StageTitleCtrl.AreYouPregnant"), getPageURL(StagePage.COMMAND, new ParameterMap(RequestContext.PARAM_GO_BACK_ON_SAVE, "")));
			write("</small>");
		}
		else if (stage.isPregnancy() && stage.getPregnancyWeek()>=35)
		{
			write("<small>");
			writeLink(getString("baby:StageTitleCtrl.DidYouGiveBirth"), getPageURL(StagePage.COMMAND, new ParameterMap(RequestContext.PARAM_GO_BACK_ON_SAVE, "")));
			write("</small>");
		}
		
		write("</div>");
	}
}
