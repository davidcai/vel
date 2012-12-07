package baby.controls;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.CheckboxInputControl;
import samoyan.controls.ImageControl;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.WebPage;
import baby.database.CheckItem;
import baby.database.CheckItemStore;
import baby.database.CheckItemUserLinkStore;
import baby.database.Checklist;
import baby.database.ChecklistStore;
import baby.database.ChecklistUserLinkStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.todo.ChecklistAjaxPage;

public class ChecklistControl
{
	private WebPage out;
	private UUID checklistID;
	private UUID userID;
	private String title = null;
	private String desc = null;
	private boolean collapsable = true;
	private boolean showChecked = true;
	
	public ChecklistControl(WebPage outputPage, UUID userID, UUID checklistID)
	{
		this.out = outputPage;
		this.checklistID = checklistID;
		this.userID = userID;
	}
	
	public ChecklistControl overrideTitle(String text)
	{
		this.title = text;
		return this;
	}
	
	public ChecklistControl overrideDescription(String text)
	{
		this.desc = text;
		return this;
	}
		
	public ChecklistControl setCollapsable(boolean b)
	{
		this.collapsable = b;
		return this;
	}

	public ChecklistControl showChecked(boolean b)
	{
		this.showChecked = b;
		return this;
	}

	public void render() throws Exception
	{
		RequestContext ctx = this.out.getContext();
		
		Checklist checklist = ChecklistStore.getInstance().load(checklistID);
		boolean collapsed = ChecklistUserLinkStore.getInstance().isCollapsed(checklistID, userID);
		List<UUID> checkitemIDs = CheckItemStore.getInstance().getByChecklistID(checklistID);
					
		Mother mother = MotherStore.getInstance().loadByUserID(out.getContext().getUserID());
		Date now = Calendar.getInstance(TimeZoneEx.GMT).getTime(); // Today's date in GMT

		boolean incomplete = false;
		for (UUID checkitemID : checkitemIDs)
		{
			if (CheckItemUserLinkStore.getInstance().isChecked(checkitemID, userID)==false)
			{
				incomplete = true;
				break;
			}
		}
		
		out.write("<div class=Checklist>");
		
		out.write("<table><tr><td>");
		
			// Expand/collapse icon
			if (this.collapsable)
			{
				new ImageControl(out)
					.resource("blank.png")
					.setAttribute("target", checklistID.toString())
					.setAttribute("onclick", "postChecklist(this);")
					.addCssClass(collapsed? "Collapsed" : "Expanded")
					.render();
			}
			else
			{
				new ImageControl(out)
					.resource("baby/collapse.png")
					.render();
			}
			
		out.write("</td><td>");
		
			// Title
			out.write("<b>");
			if (this.title!=null)
			{
				out.writeEncode(this.title);
			}
			else
			{
				out.writeEncode(checklist.getTitle());
			}
			out.write("</b>");
			
			Date due = mother.calcDateOfStage(checklist.getTimelineTo(), out.getTimeZone());
			if (due!=null)
			{
				out.write(" ");
				out.writeEncode(out.getString("baby:ChecklistCtrl.Due", due));
				if (due.before(now) && incomplete)
				{
					// out.write "Overdue" label, but only if the checklist is not complete
					out.write(" ");
					out.write("<span class=ChecklistOverdue>");
					out.writeEncode(out.getString("baby:ChecklistCtrl.Overdue"));
					out.write("</span>");
				}
			}
			if (incomplete==false && checkitemIDs.size()>0)
			{
				out.write(" ");
				out.write("<span class=ChecklistComplete>");
				out.writeEncode(out.getString("baby:ChecklistCtrl.Complete"));
				out.write("</span>");
			}
		
			// Description
			if (this.desc!=null)
			{
				out.write("<br>");
				out.writeEncode(this.desc);
			}
			else if (!Util.isEmpty(checklist.getDescription()))
			{
				out.write("<br>");
				out.writeEncode(checklist.getDescription());
			}
		out.write("</td></tr></table>");

		out.write("<table id='");
		out.writeEncode(checklistID.toString());
		out.write("'");
		if (collapsed)
		{
			out.write(" style='display:none;'");
		}
		out.write(">");
		for (UUID checkitemID : checkitemIDs)
		{
			CheckItem checkitem = CheckItemStore.getInstance().load(checkitemID);
			boolean checked = CheckItemUserLinkStore.getInstance().isChecked(checkitemID, userID);
			if (checked && !this.showChecked)
			{
				continue;
			}
			
			out.write("<tr><td>");
			new CheckboxInputControl(this.out, "chk_" + checkitemID.toString())
				.setLabel(checkitem.getText())
				.setInitialValue(checked)
				.setAttribute("onclick", "postCheckItem(this);")
				.render();
//			out.writeCheckbox("chk_" + checkitemID.toString(), checkitem.getText(), checked);
			out.write("</td></tr>");
		}
				
		out.write("</table>");
		
		out.write("</div>");
		
		
		if (out.getEphemeral("baby:ChecklistControl")==null)
		{
			this.out.setEphemeral("baby:ChecklistControl", "1");

			out.write("<script>");
			out.write("function postCheckItem(cb){");
				out.write("jQuery.get('");
				out.writeEncode(UrlGenerator.getPageURL(ctx.isSecureSocket(), ctx.getHost(), ChecklistAjaxPage.COMMAND, null));
				out.write("?sid=");
				out.writeEncode(ctx.getSessionID());
				out.write("&cmd=check&chk=' + cb.getAttribute('name') + '&val=' + cb.checked);");
			out.write("}");
			out.write("function postChecklist(img){");
				out.write("var $img=$(img);");
				out.write("$img.toggleClass('Collapsed');");
				out.write("$img.toggleClass('Expanded');");
				out.write("$('#'+$img.attr('target')).toggle();");
				out.write("jQuery.get('");
				out.writeEncode(UrlGenerator.getPageURL(ctx.isSecureSocket(), ctx.getHost(), ChecklistAjaxPage.COMMAND, null));
				out.write("?sid=");
				out.writeEncode(ctx.getSessionID());
				out.write("&cmd=toggle&chk=' + $img.attr('target') + '&val=' + $img.hasClass('Collapsed'));");
			out.write("}");
			out.write("</script>");
		}
	}
}
