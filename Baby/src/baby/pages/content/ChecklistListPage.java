package baby.pages.content;

import java.util.List;
import java.util.UUID;

import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.exc.RedirectException;

import baby.database.Checklist;
import baby.database.ChecklistStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public final class ChecklistListPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/checklists";

	@Override
	public String getTitle() throws Exception
	{
		return getString("content:ChecklistList.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		List<UUID> checklistIDs = ChecklistStore.getInstance().getAllStandard();
		
		new LinkToolbarControl(this)
			.addLink(getString("content:ChecklistList.NewChecklist"), getPageURL(EditChecklistPage.COMMAND), "icons/standard/pencil-16.png")
			.addLink(getString("content:ChecklistList.ImportChecklists"), getPageURL(ImportChecklistPage.COMMAND), "icons/standard/cardboard-box-16.png")
			.render();
		
		if (checklistIDs.size()==0)
		{
			writeEncode(getString("content:ChecklistList.NoChecklistsDefined"));
			return;
		}
		
		writeFormOpen();
		
		new DataTableControl<UUID>(this, "checklists", checklistIDs)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column("").width(1); // checkbox
				column(getString("content:ChecklistList.ChecklistTitle"));
				column(getString("content:ChecklistList.Section"));
				column(getString("content:ChecklistList.From"));
				column(getString("content:ChecklistList.To"));
			}

			@Override
			protected void renderRow(UUID checklistID) throws Exception
			{
				Checklist cl = ChecklistStore.getInstance().load(checklistID);
				
				cell();
				writeCheckbox("chk_" + cl.getID().toString(), null, false);

				cell();
				writeLink(cl.getTitle(), getPageURL(EditChecklistPage.COMMAND, new ParameterMap(EditChecklistPage.PARAM_ID, cl.getID().toString())));
				
				cell();
				if (!Util.isEmpty(cl.getSection()))
				{
					writeEncode(cl.getSection());
				}
				
				cell();
				Stage stage = Stage.fromInteger(cl.getTimelineFrom());
				if (stage.isPreconception())
				{
					writeEncode(getString("content:ChecklistList.Preconception"));
				}
				else if (stage.isPregnancy())
				{
					writeEncode(getString("content:ChecklistList.Pregnancy", stage.getPregnancyWeek()));
				}
				else if (stage.isInfancy())
				{
					writeEncode(getString("content:ChecklistList.Infancy", stage.getInfancyMonth()));
				}

				cell();
				stage = Stage.fromInteger(cl.getTimelineTo());
				if (stage.isPreconception())
				{
					writeEncode(getString("content:ChecklistList.Preconception"));
				}
				else if (stage.isPregnancy())
				{
					writeEncode(getString("content:ChecklistList.Pregnancy", stage.getPregnancyWeek()));
				}
				else if (stage.isInfancy())
				{
					writeEncode(getString("content:ChecklistList.Infancy", stage.getInfancyMonth()));
				}
			}
		}
		.render();
		
		write("<br>");
		writeRemoveButton();
		
		writeFormClose();
	}
	
	@Override
	public void commit() throws Exception
	{
		for (String p : getContext().getParameterNamesThatStartWith("chk_"))
		{
			ChecklistStore.getInstance().remove(UUID.fromString(p.substring(4)));
		}
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), null);
	}
}
