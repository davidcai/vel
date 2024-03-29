package baby.pages.content;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.controls.ControlArray;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TextAreaInputControl;
import samoyan.controls.TextInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.app.BabyConsts;
import baby.database.CheckItem;
import baby.database.CheckItemStore;
import baby.database.Checklist;
import baby.database.ChecklistStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public final class EditChecklistPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/checklist";
	public final static String PARAM_ID = "id";
	
	private Checklist checklist;

	@Override
	public void init() throws Exception
	{
		this.checklist = ChecklistStore.getInstance().open(getParameterUUID(PARAM_ID));
		if (this.checklist==null)
		{
			this.checklist = new Checklist();
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		if (this.checklist.isSaved())
		{
			return this.checklist.getTitle();
		}
		else
		{
			return getString("content:EditChecklist.Title");
		}
	}

	@Override
	public void renderHTML() throws Exception
	{	
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("content:EditChecklist.ChecklistTitle"));
		twoCol.writeTextInput("title", this.checklist.getTitle(), 70, Checklist.MAXSIZE_TITLE);
		
		twoCol.writeRow(getString("content:EditChecklist.Description"));
		twoCol.writeTextAreaInput("desc", this.checklist.getDescription(), 70, 2, Checklist.MAXSIZE_DESCRIPTION);
		
		twoCol.writeRow(getString("content:EditChecklist.Section"));
		SelectInputControl select = new SelectInputControl(twoCol, "section");
		select.setInitialValue(this.checklist.getSection());
		select.addOption(BabyConsts.SECTION_TODO, BabyConsts.SECTION_TODO);
		for (String s : BabyConsts.SECTIONS_APPOINTMENT)
		{
			select.addOption(s,s);
		}
		select.render();

		twoCol.writeRow(getString("content:EditChecklist.Timeline"));

		SelectInputControl from = new SelectInputControl(twoCol, "from");
		populateTimelineCombo(from);
		from.setInitialValue(this.checklist.getTimelineFrom());
		from.render();
		
		twoCol.write(" ");
		twoCol.writeEncode(getString("content:EditChecklist.Through"));
		twoCol.write(" ");
				
		SelectInputControl to = new SelectInputControl(twoCol, "to");
		populateTimelineCombo(to);
		to.setInitialValue(this.checklist.getTimelineTo());
		to.render();
		
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("content:EditChecklist.CheckItems"));

		List<UUID> itemIDs = CheckItemStore.getInstance().getByChecklistID(this.checklist.getID());
		new ControlArray<UUID>(twoCol, "items", itemIDs)
		{
			@Override
			public void renderRow(int rowNum, UUID itemID) throws Exception
			{
				CheckItem item = CheckItemStore.getInstance().load(itemID);
				writeHiddenInput("itemid_" + rowNum, item!=null? item.getID() : null);
				new TextAreaInputControl(this, "itemtext_" + rowNum)
					.setRows(2).setCols(70)
					.setPlaceholder(getString("content:EditChecklist.Text"))
					.setMaxLength(CheckItem.MAXSIZE_TEXT)
					.setInitialValue(item!=null? item.getText() : null)
					.render();
				write("<br>");
				new TextInputControl(this, "itemlink_" + rowNum)
					.setSize(70)
					.setPlaceholder(getString("content:EditChecklist.Link"))
					.setMaxLength(CheckItem.MAXSIZE_LINK)
					.setInitialValue(item!=null? item.getLink() : null)
					.render();
			}
		}
		.render();
		
		twoCol.render();

		write("<br>");
		writeSaveButton("save", this.checklist);
		
		writeHiddenInput(PARAM_ID, null);
		
		writeFormClose();
	}

	private void populateTimelineCombo(SelectInputControl select)
	{
		select.addOption(getString("content:EditChecklist.Preconception"), Stage.preconception().toInteger());
		for (int i=1; i<=40; i++)
		{
			select.addOption(getString("content:EditChecklist.Pregnancy", i), Stage.pregnancy(i).toInteger());
		}
		for (int i=1; i<=12; i++)
		{
			select.addOption(getString("content:EditChecklist.Infancy", i), Stage.infancy(i).toInteger());
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		validateParameterString("title", 1, Checklist.MAXSIZE_TITLE);
		validateParameterString("desc", 0, Checklist.MAXSIZE_DESCRIPTION);
		
		// Validate "from" and "to"
		Stage from = Stage.fromInteger(getParameterInteger("from"));
		if (from.isValid()==false)
		{
			throw new WebFormException("from", getString("common:Errors.MissingField"));
		}
		Stage to = Stage.fromInteger(getParameterInteger("to"));
		if (to.isValid()==false)
		{
			throw new WebFormException("to", getString("common:Errors.MissingField"));
		}
		if (from.toInteger() > to.toInteger())
		{
			throw new WebFormException(new String[] {"from", "to"}, getString("content:EditHealthBeg.InvalidTimeline"));
		}

		int actualCount = 0;
		int itemCount = getParameterInteger("items");
		for (int i=0; i<itemCount; i++)
		{
			String txt = getParameterString("itemtext_" + i);
			if (txt!=null)
			{
				actualCount ++;
				if (Util.isEmpty(txt))
				{
					throw new WebFormException("itemtext_" + i, getString("common:Errors.MissingField"));
				}
			}
		}
		
		if (actualCount==0)
		{
			throw new WebFormException("itemtext_" + itemCount, getString("common:Errors.MissingField"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		boolean saved = this.checklist.isSaved();
		
		this.checklist.setTitle(getParameterString("title"));
		this.checklist.setDescription(getParameterString("desc"));
		
		this.checklist.setSection(getParameterString("section"));
		
		this.checklist.setTimelineFrom(getParameterInteger("from"));
		this.checklist.setTimelineTo(getParameterInteger("to"));
		
		this.checklist.setUpdatedDate(new Date());
		
		ChecklistStore.getInstance().save(this.checklist);
		
		// Saved checkitems
		Set<UUID> itemIDs = new HashSet<UUID>();
		itemIDs.addAll(CheckItemStore.getInstance().getByChecklistID(this.checklist.getID()));
		int itemCount = getParameterInteger("items");
		for (int i=0; i<itemCount; i++)
		{
			String txt = getParameterString("itemtext_" + i);
			if (txt==null) continue;
			String lnk = getParameterString("itemlink_" + i);

			CheckItem item;
			UUID id = getParameterUUID("itemid_" + i);
			if (id==null)
			{
				// New item
				item = new CheckItem();
			}
			else
			{
				// Existing item
				item = CheckItemStore.getInstance().open(id);
				itemIDs.remove(id);
			}
			item.setChecklistID(this.checklist.getID());
			item.setText(txt);
			item.setLink(lnk);
			item.setOrderSequence(i);
			CheckItemStore.getInstance().save(item);
		}
		
		// Removed checkitems
		for (UUID id : itemIDs)
		{
			CheckItemStore.getInstance().remove(id);
		}
		
		// For now, redirect to self
		if (saved)
		{
			throw new RedirectException(getContext().getCommand(), new ParameterMap(PARAM_ID, this.checklist.getID().toString()).plus(RequestContext.PARAM_SAVED, ""));
		}
		else
		{
			throw new RedirectException(getContext().getCommand(), null);
		}
	}
}
