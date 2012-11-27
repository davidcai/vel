package baby.pages.todo;

import java.util.List;
import java.util.UUID;

import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.PageNotFoundException;
import baby.database.CheckItem;
import baby.database.CheckItemStore;
import baby.database.CheckItemUserLinkStore;
import baby.database.Checklist;
import baby.database.ChecklistStore;
import baby.database.ChecklistUserLinkStore;
import baby.pages.BabyPage;

public class ChecklistAjaxPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_TODO + "/checklist.ajax";
	
	@Override
	public boolean isEnvelope() throws Exception
	{
		return false;
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		if (getParameterUUID("sid").equals(ctx.getSessionID())==false)
		{
			throw new PageNotFoundException();
		}
		
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
				
				// Automatically collapse a checklist that is complete
				boolean allChecked = true;
				List<UUID> siblingIDs = CheckItemStore.getInstance().getByChecklistID(ci.getChecklistID());
				for (UUID sibID : siblingIDs)
				{
					if (!CheckItemUserLinkStore.getInstance().isChecked(sibID, ctx.getUserID()))
					{
						allChecked = false;
						break;
					}
				}
				if (allChecked)
				{
					ChecklistUserLinkStore.getInstance().collapse(ci.getChecklistID(), ctx.getUserID());
				}
			}
		}
		else if (cmd.equals("toggle"))
		{
			UUID chkID = UUID.fromString(getParameterString("chk"));
			boolean collapsed = Boolean.valueOf(getParameterString("val"));
			Checklist cl = ChecklistStore.getInstance().load(chkID);
			if (cl!=null)
			{
				if (collapsed)
				{
					ChecklistUserLinkStore.getInstance().collapse(chkID, ctx.getUserID());
				}
				else
				{
					ChecklistUserLinkStore.getInstance().expand(chkID, ctx.getUserID());
				}
			}
		}
	}
}
