package baby.pages.info;

import java.util.List;
import java.util.UUID;

import samoyan.controls.TextInputControl;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;

import baby.app.BabyConsts;
import baby.controls.ChecklistControl;
import baby.controls.TimelineControl;
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
			low = TimelineControl.getLowRange(stage.toInteger());
			high = TimelineControl.getHighRange(stage.toInteger());
		}
		
		TimelineControl tlCtrl = new TimelineControl(this, stage, PARAM_STAGE);
		
//		writeHorizontalNav(ChecklistPage.COMMAND);

		// Render timeline
		write("<table><tr valign=middle><td>");
		writeEncode(getString("information:Checklist.YourChecklists"));
		write("</td><td>");
		tlCtrl.render();
		write("</td></tr></table><br>");
				
		// Personal checklist
		Checklist personalChecklist = ChecklistStore.getInstance().loadPersonalChecklist(userID);
		new ChecklistControl(this, userID, personalChecklist.getID())
			.overrideTitle(getString("information:Checklist.PersonalChecklist"))
			.overrideDescription(getString("information:Checklist.PersonalChecklistDesc"))
			.setCollapsable(false)
			.showChecked(false)
			.showDueDate(false)
			.render();
		write("<br>");
		
		// Add
		writeFormOpen();
		new TextInputControl(this, "add")
			.setPlaceholder(getString("information:Checklist.AddCheckitem"))
			.setSize(ctx.getUserAgent().isSmartPhone()? 30 : 40)
			.setMaxLength(CheckItem.MAXSIZE_TEXT)
			.render();
		write(" ");
		writeButton(getString("controls:Button.Add"));
		writeFormClose();
		write("<br>");
		
		// Common checklists
		List<UUID> checklistIDs = ChecklistStore.getInstance().queryBySectionAndTimeline(BabyConsts.SECTION_TODO, Stage.preconception().toInteger(), high);
		for (UUID checklistID : checklistIDs)
		{
			new ChecklistControl(this, userID, checklistID).render();
			write("<br>");
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
