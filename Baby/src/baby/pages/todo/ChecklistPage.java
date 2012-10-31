package baby.pages.todo;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.CheckboxInputControl;
import samoyan.controls.TextInputControl;
import samoyan.core.ParameterMap;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.exc.RedirectException;

import baby.controls.TimelineControl;
import baby.database.CheckItem;
import baby.database.CheckItemStore;
import baby.database.CheckItemUserLinkStore;
import baby.database.Checklist;
import baby.database.ChecklistStore;
import baby.database.ChecklistUserLinkStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public final class ChecklistPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_TODO + "/checklist";
	private static final String PARAM_STAGE = "stage";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("todo:Checklist.Title");
	}
	
	private void performCommand() throws Exception
	{
		RequestContext ctx = getContext();

		String cmd = getParameterString("cmd");
		if (cmd.equals("check"))
		{
			UUID chkID = UUID.fromString(getParameterString("chk").substring(4));
			boolean checked = Boolean.valueOf(getParameterString("val"));
			CheckItem ci = CheckItemStore.getInstance().load(chkID);
			if (ci!=null)
			{
				if (checked)
				{
					CheckItemUserLinkStore.getInstance().check(chkID, ctx.getUserID());
				}
				else
				{
					CheckItemUserLinkStore.getInstance().uncheck(chkID, ctx.getUserID());
				}
			}
		}
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		if (isParameter("cmd"))
		{
			performCommand();
			return;
		}
		
		RequestContext ctx = getContext();
		UUID userID = ctx.getUserID();
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		Stage stage = null;
		if (isParameter(PARAM_STAGE))
		{
			stage = Stage.fromInteger(getParameterInteger(PARAM_STAGE));
		}
		if (stage==null || stage.isValid()==false)
		{
			stage = mother.getPregnancyStage();
		}
		
		// Timeline
		new TimelineControl(this, stage)
			.setStageParamName(PARAM_STAGE)
			.render();
		write("<br>");
				
		// Personal checklist
		{
			Checklist personalChecklist = ChecklistStore.getInstance().loadPersonalChecklist(userID);
			write("<b>");
			writeEncode(getString("todo:Checklist.PersonalChecklist"));
			write("</b>");
			write("<br>");
			writeEncode(getString("todo:Checklist.PersonalChecklistDesc"));
			write("<br>");
			write("<table>");
			List<UUID> checkitemIDs = CheckItemStore.getInstance().getByChecklistID(personalChecklist.getID());
			for (UUID checkitemID : checkitemIDs)
			{
				CheckItem checkitem = CheckItemStore.getInstance().load(checkitemID);
				boolean checked = CheckItemUserLinkStore.getInstance().isChecked(checkitemID, userID);
				write("<tr><td>");
				new CheckboxInputControl(this, "chk_" + checkitemID.toString())
					.setLabel(checkitem.getText())
					.setInitialValue(checked)
					.setAttribute("onclick", "postCheckItem(this);")
					.render();
//				writeCheckbox("chk_" + checkitemID.toString(), checkitem.getText(), checked);
				write("</td></tr>");
			}
			write("<tr><td>");
			writeFormOpen();
			new CheckboxInputControl(this, "emptycb")
				.setDisabled(true)
				.render();
			write(" ");
			new TextInputControl(this, "newitem")
				.setPlaceholder(getString("todo:Checklist.AddCheckitem"))
				.setSize(40)
				.setMaxLength(CheckItem.MAXSIZE_TEXT)
				.render();
			write(" ");
			writeButton("add", getString("controls:Button.Add"));
			writeFormClose();
			write("</td></tr>");
			write("</table><br><br>");
		}
		
		// Checklists
		Date now = Calendar.getInstance(TimeZoneEx.GMT).getTime(); // Today's date in GMT
		int tzOffset = getTimeZone().getRawOffset();
		
		List<UUID> checklistIDs = ChecklistStore.getInstance().queryBySectionAndTimeline(Checklist.SECTION_TODO, stage.toInteger());
		for (UUID checklistID : checklistIDs)
		{
			Checklist checklist = ChecklistStore.getInstance().load(checklistID);
			boolean collapsed = ChecklistUserLinkStore.getInstance().isCollapsed(checklistID, userID);
			
			write("<b>");
			writeEncode(checklist.getTitle());
			write("</b>");
			
			Date due = calcDateOfStage(checklist, mother);
			if (due!=null)
			{
				write(" ");
				if (due.before(now))
				{
					writeEncode(getString("todo:Checklist.Overdue", new Date(due.getTime() - tzOffset)));
				}
				else
				{
					writeEncode(getString("todo:Checklist.Due", new Date(due.getTime() - tzOffset)));
				}
			}
			
			if (!Util.isEmpty(checklist.getDescription()))
			{
				write("<br>");
				writeEncode(checklist.getDescription());
			}
			write("<br>");
			write("<table>");
			List<UUID> checkitemIDs = CheckItemStore.getInstance().getByChecklistID(checklistID);
			for (UUID checkitemID : checkitemIDs)
			{
				CheckItem checkitem = CheckItemStore.getInstance().load(checkitemID);
				boolean checked = CheckItemUserLinkStore.getInstance().isChecked(checkitemID, userID);
				write("<tr><td>");
				new CheckboxInputControl(this, "chk_" + checkitemID.toString())
					.setLabel(checkitem.getText())
					.setInitialValue(checked)
					.setAttribute("onclick", "postCheckItem(this);")
					.render();
//				writeCheckbox("chk_" + checkitemID.toString(), checkitem.getText(), checked);
				write("</td></tr>");
			}
			write("</table><br><br>");
		}
		
		write("<script>\r\n");
		write("function postCheckItem(cb) {");
		write("jQuery.get('");
		writeEncode(UrlGenerator.getPageURL(ctx.isSecureSocket(), ctx.getHost(), ctx.getCommand(), new ParameterMap("cmd", "check")));
		write("&chk=' + cb.getAttribute('name') + '&val=' + cb.checked);");
		write("}");
		write("\r\n</script>");
	}
	
	private Date calcDateOfStage(Checklist cl, Mother mother)
	{
		Stage presentStage = mother.getPregnancyStage();
		Stage dueStage = Stage.fromInteger(cl.getTimelineTo());
		
		if (presentStage.isPreconception())
		{
			// We can't estimate dates
			return null;
		}
		else if (presentStage.isPregnancy())
		{
			Calendar cal = Calendar.getInstance(TimeZoneEx.GMT);
			cal.setTime(mother.getDueDate());

			if (dueStage.isPreconception())
			{
				cal.add(Calendar.DATE, -7 * 40);
			}
			else if (dueStage.isPregnancy())
			{
				cal.add(Calendar.DATE, -7 * 40);
				cal.add(Calendar.DATE, 7 * dueStage.getPregnancyWeek());
			}
			else if (dueStage.isInfancy())
			{
				cal.add(Calendar.MONTH, dueStage.getInfancyMonth());
			}
			return cal.getTime();
		}
		else if (presentStage.isInfancy())
		{
			Calendar cal = Calendar.getInstance(TimeZoneEx.GMT);
			cal.setTime(mother.getBirthDate());
			
			if (dueStage.isPreconception())
			{
				cal.add(Calendar.DATE, -7 * 40);
			}
			else if (dueStage.isPregnancy())
			{
				cal.add(Calendar.DATE, -7 * 40);
				cal.add(Calendar.DATE, 7 * dueStage.getPregnancyWeek());
			}
			else if (dueStage.isInfancy())
			{
				cal.add(Calendar.MONTH, dueStage.getInfancyMonth());
			}
			return cal.getTime();
		}		
		else
		{
			return null;
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter("add"))
		{
			validateParameterString("newitem", 1, CheckItem.MAXSIZE_TEXT);
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		if (isParameter("add"))
		{
			Checklist personalChecklist = ChecklistStore.getInstance().loadPersonalChecklist(getContext().getUserID());
			List<UUID> checkitemIDs = CheckItemStore.getInstance().getByChecklistID(personalChecklist.getID());

			CheckItem checkitem = new CheckItem();
			checkitem.setChecklistID(personalChecklist.getID());
			checkitem.setOrderSequence(checkitemIDs.size());
			checkitem.setText(getParameterString("newitem"));
			CheckItemStore.getInstance().save(checkitem);
			
			// Redirect to self
			throw new RedirectException(getContext().getCommand(), null);
		}
	}
	
	@Override
	public boolean isEnvelope() throws Exception
	{
		return isParameter("cmd")==false;
	}
}
