package baby.pages.info;

import java.util.List;
import java.util.UUID;

import samoyan.controls.TextInputControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;

import baby.app.BabyConsts;
import baby.controls.ChecklistControl;
import baby.controls.TimelineSliderControl;
import baby.database.CheckItem;
import baby.database.CheckItemStore;
import baby.database.Checklist;
import baby.database.ChecklistStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public final class ChecklistPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/checklist";
	private static final String PARAM_STAGE = "stage";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:Checklist.Title");
	}
		
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		UUID userID = ctx.getUserID();
		boolean phone = ctx.getUserAgent().isSmartPhone();
		
		// Figure out the stage and its range (high, low)
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		int low = 0;
		int high = 0;
		Stage stage = null;
		if (isParameter(PARAM_STAGE))
		{
			String rangeStr = getParameterString(PARAM_STAGE);
			int p = rangeStr.indexOf("-");
			low = Integer.parseInt(rangeStr.substring(0, p));
			high = Integer.parseInt(rangeStr.substring(p+1));
			stage = Stage.fromInteger(low);
		}
		if (stage==null || stage.isValid()==false)
		{
			stage = mother.getPregnancyStage();
			low = TimelineSliderControl.getLowRange(stage.toInteger());
			high = TimelineSliderControl.getHighRange(stage.toInteger());
		}
				
//		writeHorizontalNav(ChecklistPage.COMMAND);

		// Render timeline
		write("<table");
		if (phone)
		{
			write(" width=\"100%\"");
		}
		write("><tr><td align=center>");
		
		new TimelineSliderControl(this, stage, PARAM_STAGE).render();

		write("</td></tr><tr><td align=center>");
		
		// View: in progress or all
		if (isParameter("vu"))
		{
			mother = (Mother) mother.clone(); // Open for writing
			if (getParameterString("vu").equals("all"))
			{
				mother.setChecklistViewAll(true);
			}
			else if (getParameterString("vu").equals("progress"))
			{
				mother.setChecklistViewAll(false);
			}
			MotherStore.getInstance().save(mother);
		}
		boolean showAll = mother.isChecklistViewAll();
//		writeEncode(getString("information:Checklist.View"));
//		write(" ");
		if (showAll)
		{
			writeLink(getString("information:Checklist.ViewInProgress"), getPageURL(ctx.getCommand(), new ParameterMap("vu", "progress")));
			write(" | ");
			write("<b>");
			writeEncode(getString("information:Checklist.ViewAll"));
			write("</b>");
		}
		else
		{
			write("<b>");
			writeEncode(getString("information:Checklist.ViewInProgress"));
			write("</b>");
			write(" | ");
			writeLink(getString("information:Checklist.ViewAll"), getPageURL(ctx.getCommand(), new ParameterMap("vu", "all")));
		}
		write("<hr><br>");		
		write("</td></tr></table>");
		
		
		// Add to personal checklist
		write("<div class=AddToChecklist>");
		writeFormOpen();
		new TextInputControl(this, "add")
			.setPlaceholder(getString("information:Checklist.AddCheckitem"))
			.setSize(ctx.getUserAgent().isSmartPhone()? 30 : 40)
			.setMaxLength(CheckItem.MAXSIZE_TEXT)
			.render();
		write(" ");
		writeButton(getString("controls:Button.Add"));
		writeFormClose();
		write("</div>");
		write("<br>");

		// Personal checklist
		Checklist personalChecklist = ChecklistStore.getInstance().loadPersonalChecklist(userID);
		new ChecklistControl(this, userID, personalChecklist.getID())
			.overrideTitle(getString("information:Checklist.PersonalChecklist"))
			.overrideDescription(getString("information:Checklist.PersonalChecklistDesc"))
			.collapsable(false)
			.collapseLongText(false)
			.showCompleted(showAll)
			.showDueDate(false)
			.render();
			
		// Common checklists
		List<UUID> checklistIDs = ChecklistStore.getInstance().queryBySectionAndTimeline(BabyConsts.SECTION_TODO, Stage.preconception().toInteger(), high);
		for (UUID checklistID : checklistIDs)
		{
			new ChecklistControl(this, userID, checklistID).showCompleted(showAll).render();
		}
	}
		
	@Override
	public void validate() throws Exception
	{
		if (isParameter("add"))
		{
			validateParameterString("add", 1, CheckItem.MAXSIZE_TEXT);
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		if (isParameter("add"))
		{
			Checklist personalChecklist = ChecklistStore.getInstance().loadPersonalChecklist(getContext().getUserID());
			
			int seq = 0;
			List<UUID> checkitemIDs = CheckItemStore.getInstance().getByChecklistID(personalChecklist.getID());
			if (checkitemIDs.size()>0)
			{
				CheckItem lastCheckitem = CheckItemStore.getInstance().load(checkitemIDs.get(checkitemIDs.size()-1));
				seq = lastCheckitem.getOrderSequence() + 1;
			}
			
			CheckItem checkitem = new CheckItem();
			checkitem.setChecklistID(personalChecklist.getID());
			checkitem.setOrderSequence(seq);
			checkitem.setText(getParameterString("add"));
			CheckItemStore.getInstance().save(checkitem);
			
			// Redirect to self
			throw new RedirectException(getContext().getCommand(), null);
		}
	}
}
