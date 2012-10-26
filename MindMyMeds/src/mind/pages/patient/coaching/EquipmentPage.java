package mind.pages.patient.coaching;

import mind.database.Equipment;
import mind.database.EquipmentStore;
import mind.pages.IndustryTypeAhead;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class EquipmentPage extends WebPage
{
	public static final String COMMAND = CoachingPage.COMMAND + "/equipment";
	public static final String PARAM_ID = "id";

	private Equipment eq = null;

	@Override
	public void init() throws Exception
	{
		this.eq = EquipmentStore.getInstance().open(getParameterUUID("id"));
		if (this.eq == null)
		{
			this.eq = new Equipment();
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		validateParameterString("name", 1, Equipment.MAXSIZE_NAME);
		validateParameterString("industry", 3, Equipment.MAXSIZE_INDUSTRY);
		validateParameterInteger("weight", 0, Equipment.MAXVALUE_WEIGHT);
		
		if (Util.isEmptyHTML(getParameterRichEdit("desc")))
		{
			throw new WebFormException("desc", "empty");
		}
	}

	@Override
	public void commit() throws Exception
	{
		boolean isSaved = this.eq.isSaved();

		this.eq.setName(getParameterString("name"));
		this.eq.setIndustry(getParameterString("industry"));
		this.eq.setWeight(getParameterInteger("weight"));
		this.eq.setDesc(getParameterRichEdit("desc"));

		EquipmentStore.getInstance().save(this.eq);

		if (isSaved == false)
		{
			// redirect to equipments page
			throw new RedirectException(EquipmentsPage.COMMAND, null);
		}
		else
		{
			// redirect to self in order to clear form submission
			throw new RedirectException(getContext().getCommand(), new ParameterMap(EquipmentPage.PARAM_ID, this.eq
					.getID().toString()).plus(RequestContext.PARAM_SAVED, ""));
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("mind:Equipment.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("mind:Equipment.Name"));
		twoCol.writeTextInput("name", this.eq.getName(), 20, Equipment.MAXSIZE_NAME);
		twoCol.writeRow(getString("mind:Equipment.Industry"));
		twoCol.writeTypeAheadInput("industry", null, null, 20, Equipment.MAXSIZE_INDUSTRY, getPageURL(IndustryTypeAhead.COMMAND));
		twoCol.writeRow(getString("mind:Equipment.Weight"));
		twoCol.writeNumberInput("weight", 100, 5, 0, Equipment.MAXVALUE_WEIGHT);
		twoCol.writeRow(getString("mind:Equipment.Desc"));
		twoCol.writeRichEditField("desc", this.eq.getDesc(), 80, 5);
		
		twoCol.render();
		
		write("<br>");
		writeSaveButton(this.eq);
		
		// postback
		writeHiddenInput("id", null);
		
		writeFormClose();
	}
}