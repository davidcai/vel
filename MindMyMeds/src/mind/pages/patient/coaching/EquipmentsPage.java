package mind.pages.patient.coaching;

import java.util.List;
import java.util.UUID;

import mind.database.Equipment;
import mind.database.EquipmentStore;
import samoyan.controls.CheckboxInputControl;
import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class EquipmentsPage extends WebPage
{
	public final static String COMMAND = CoachingPage.COMMAND + "/equipments";
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter("query"))
		{
			validateParameterInteger("WeightFrom", 0, Equipment.MAXVALUE_WEIGHT);
			validateParameterInteger("WeightTo", 0, Equipment.MAXVALUE_WEIGHT);
		}
		else if (isParameter("remove"))
		{
			// Must select at least one check box
			if (getContext().getParameterNamesThatStartWith("chk_").size() == 0)
			{
				throw new WebFormException(getString("common:Errors.MissingField"));
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		if (isParameter("query"))
		{
			// Do nothing
		}
		else if (isParameter("remove"))
		{
			for (String prmName : getContext().getParameterNamesThatStartWith("chk_"))
			{
				EquipmentStore.getInstance().remove(UUID.fromString(prmName.substring(4)));				
			}
			
			// self redirect to clean the form after save		
			throw new RedirectException(getContext().getCommand(), null);
		}
	}

	@Override
	public void renderHTML() throws Exception
	{
		// create new procedure link
		new LinkToolbarControl(this).addLink(getString("mind:Equipments.CreateNew"), getPageURL(EquipmentPage.COMMAND),
				"icons/basic1/pencil_16.png").render();

		List<UUID> eqIds = null;
		Integer weightFrom = getParameterInteger("WeightFrom");
		Integer weightTo = getParameterInteger("WeightTo");
		if (weightFrom != null && weightTo != null)
		{
			eqIds = EquipmentStore.getInstance().findWithinWeightRange(weightFrom, weightTo);
		}
		else
		{
			eqIds = EquipmentStore.getInstance().getAllIDs();
		}
		
		// [start] Query form
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("mind:Equipments.WeightFrom"));
		twoCol.writeNumberInput("WeightFrom", 0, 5, 0, Equipment.MAXVALUE_WEIGHT);
		twoCol.writeEncode(" ");
		twoCol.writeEncode(getString("mind:Equipments.WeightTo"));
		twoCol.writeEncode(" ");
		twoCol.writeNumberInput("WeightTo", Equipment.MAXVALUE_WEIGHT, 5, 0, Equipment.MAXVALUE_WEIGHT);
		
		twoCol.render();
		
		write("<br>");
		writeButton("query", getString("mind:Equipments.Query"));
		
		writeFormClose();
		write("<br><br>");
		// [end]
		
		if (eqIds.isEmpty() == false)
		{
			writeFormOpen();
			
			new DataTableControl<UUID>(this, "procs", eqIds) 
			{
				@Override
				protected void defineColumns() throws Exception
				{
					column("").width(1).html(new WebPage() 
					{
						public void renderHTML() throws Exception 
						{
							new CheckboxInputControl(this, "all").affectAll("chk_").render();
						};
					});
					column(getString("mind:Equipments.Name"));
					column(getString("mind:Equipments.Industry"));
					column(getString("mind:Equipments.Weight"));	
				}

				@Override
				protected void renderRow(UUID eqId) throws Exception
				{
					Equipment eq = EquipmentStore.getInstance().load(eqId);			
					
					cell();
					new CheckboxInputControl(this, "chk_" + eqId.toString()).setDisabled(!EquipmentStore.getInstance().canRemove(eqId)).render();
					// writeCheckbox
					
					cell();
					writeLink(eq.getName(), getPageURL(EquipmentPage.COMMAND, new ParameterMap(EquipmentPage.PARAM_ID, eq.getID().toString())));

					cell();
					writeEncode(eq.getIndustry());
					
					cell();
					writeEncodeLong(eq.getWeight());
				}
				
			}.render();
			
			write("<br>");
			writeRemoveButton("remove");
			
			writeFormClose();
			write("<br>");
		}
		else
		{
			writeEncode(getString("mind:Equipments.NoResults"));
		}


		// Equipment equipment = new Equipment();
		// equipment.setName("Audiometer");
		// equipment.setIndustry("Industry A");
		// equipment.setWeight(100);
		// equipment.setDesc("Audiometer description.");
		// EquipmentStore.getInstance().save(equipment);
		//
		// equipment = new Equipment();
		// equipment.setName("Retinoscope");
		// equipment.setIndustry("Industry R");
		// equipment.setWeight(30);
		// equipment.setDesc("Retinoscope description.");
		// EquipmentStore.getInstance().save(equipment);
		//
		// equipment = new Equipment();
		// equipment.setName("Penlight");
		// equipment.setIndustry("Industry P");
		// equipment.setWeight(25);
		// equipment.setDesc("Penlight description.");
		// EquipmentStore.getInstance().save(equipment);
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("mind:Equipments.Title");
	}
}
