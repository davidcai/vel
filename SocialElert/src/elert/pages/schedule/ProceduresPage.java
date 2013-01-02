package elert.pages.schedule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.controls.TabControl;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Procedure;
import elert.database.ProcedureFacilityLinkStore;
import elert.database.ProcedureStore;
import elert.database.ProcedureType;
import elert.database.ProcedureTypeStore;
import elert.pages.ElertPage;
import elert.pages.common.CommonProcedurePage;
import elert.pages.typeahead.ProcedureOrTypeTypeAhead;

public class ProceduresPage extends ElertPage
{
	public static final String COMMAND_STANDARD = ElertPage.COMMAND_SCHEDULE + "/standard-procedures";		
	public static final String COMMAND_CUSTOM = ElertPage.COMMAND_SCHEDULE + "/custom-procedures";		

	@Override
	public void validate() throws Exception
	{
		if (isParameter("add"))
		{
			Pair<String, String> procedureKvp = getParameterTypeAhead("procedure");
			if (Util.isEmpty(procedureKvp.getValue()))
			{
				throw new WebFormException("procedure", getString("common:Errors.MissingField"));
			}
			if (Util.isEmpty(procedureKvp.getKey()))
			{
				throw new WebFormException("procedure", getString("common:Errors.InvalidValue"));
			}			
		}
	}

	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();

		//fetch all facilities associated with the current user
		List<UUID> facilityIDs = FacilityStore.getInstance().queryByUser(ctx.getUserID());
		
		// Remove procedure assignments
		if(isParameter("remove"))
		{
			for (String prmName : getContext().getParameterNamesThatStartWith("chk_"))
			{
				UUID procedureID = UUID.fromString(prmName.substring(4));					
				for (UUID facilityID : facilityIDs)
				{
					ProcedureFacilityLinkStore.getInstance().removeProcedureFromFacility(procedureID, facilityID);
				}
			}
		}

		// Load all procedures linked to these facilities
		Set<UUID> proceduresSet = new HashSet<UUID>();
		for (UUID facilityID : facilityIDs)
		{
			proceduresSet.addAll(ProcedureFacilityLinkStore.getInstance().getProceduresByFacility(facilityID));
		}

		
		// Add a procedure or procedure type
		if (isParameter("add"))
		{
			Pair<String, String> procedureKvp = getParameterTypeAhead("procedure");
			UUID id = UUID.fromString(procedureKvp.getKey());
			ProcedureType procType = ProcedureTypeStore.getInstance().load(id);
			Procedure proc = ProcedureStore.getInstance().load(id);
			
			if (procType!=null)
			{
				// Add all procedures from the given type
				for (UUID procedureID : ProcedureStore.getInstance().queryStandardByType(id))
				{
					if (proceduresSet.contains(procedureID)==false)
					{
						// This is a new procedure to be added to the list
						for (UUID facilityID : facilityIDs)
						{
							if (ProcedureFacilityLinkStore.getInstance().isProcedureAssignedToFacility(procedureID, facilityID)==false)
							{
								ProcedureFacilityLinkStore.getInstance().unassignProcedureFromFacility(procedureID, facilityID);
							}
						}
					}
				}
			}
			else if (proc!=null)
			{
				if (proceduresSet.contains(proc.getID())==false)
				{
					// This is a new procedure to be added to the list
					for (UUID facilityID : facilityIDs)
					{
						if (ProcedureFacilityLinkStore.getInstance().isProcedureAssignedToFacility(proc.getID(), facilityID)==false)
						{
							ProcedureFacilityLinkStore.getInstance().unassignProcedureFromFacility(proc.getID(), facilityID);
						}
					}
				}
			}
		}		
	
		// Change procedure assignments
		if(isParameter("save"))
		{
			for (UUID facilityID : facilityIDs)
			{
				for (UUID procedureID : proceduresSet)
				{
					if (isParameter("facility_" + procedureID.toString() + "_" + facilityID.toString()))
					{
						ProcedureFacilityLinkStore.getInstance().assignProcedureToFacility(procedureID, facilityID);
					}
					else
					{
						ProcedureFacilityLinkStore.getInstance().unassignProcedureFromFacility(procedureID, facilityID);
					}
				}
			}
			
			// Support guided setup
			progressGuidedSetup();
		}
		
		// self redirect to clean the form after save
		throw new RedirectException(ctx.getCommand(), null);
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();

		List<UUID> facilityIDs = FacilityStore.getInstance().queryByUser(ctx.getUserID()); //fetch all facilities associated with this user		
		if (facilityIDs.size()==0)
		{
			StringBuilder link = new StringBuilder();
			link.append("<a href=\"");
			link.append(getPageURL(HomeServiceAreasPage.COMMAND));
			link.append("\">");
			link.append(Util.htmlEncode(getString("schedule:Procedures.HomeServiceArea")));
			link.append("</a>");
			String pattern = Util.htmlEncode(getString("schedule:Procedures.NoFacilities", "$link$"));
			write(Util.strReplace(pattern, "$link$", link.toString()));
			return;
		}
		
		// Load all procedures linked to these facilities
		Set<UUID> proceduresSet = new HashSet<UUID>();
		for (UUID facilityID : facilityIDs)
		{
			proceduresSet.addAll(ProcedureFacilityLinkStore.getInstance().getProceduresByFacility(facilityID));
		}
		
		// Break into standard and custom
		List<UUID> standardProcedures = new ArrayList<UUID>();
		List<UUID> customProcedures = new ArrayList<UUID>();
		for (UUID procedureID : proceduresSet)
		{
			Procedure procedure = ProcedureStore.getInstance().load(procedureID);
			if (procedure.isCustom())
			{
				customProcedures.add(procedureID);
			}
			else
			{
				standardProcedures.add(procedureID);
			}
		}
	
		writeEncode(getString("schedule:Procedures.Help"));
		write("<br><br>");

		if (ctx.getCommand(1).equals(UrlGenerator.COMMAND_SETUP)==false)
		{
			new TabControl(this)
				.addTab(COMMAND_STANDARD, getString("schedule:Procedures.Standard"), getPageURL(COMMAND_STANDARD))
				.addTab(COMMAND_CUSTOM, getString("schedule:Procedures.Custom"), getPageURL(COMMAND_CUSTOM))
				.setCurrentTab(ctx.getCommand())
				.render();
			
			if (ctx.getCommand().equals(COMMAND_STANDARD))
			{
				renderStandardProcs(standardProcedures, facilityIDs);
			}
			else if (ctx.getCommand().equals(COMMAND_CUSTOM))
			{
				renderCustomProcs(customProcedures, facilityIDs);
			}
		}
		else
		{
			renderStandardProcs(standardProcedures, facilityIDs);
		}
	}
	
	private void renderStandardProcs(List<UUID> standardProcedures, final List<UUID> facilityIDs) throws Exception
	{
		RequestContext ctx = getContext();
		final boolean phone = ctx.getUserAgent().isSmartPhone();
		final boolean guidedSetup = ctx.getCommand(1).equals(UrlGenerator.COMMAND_SETUP); 
		
		writeFormOpen();
		
//		if (!guidedSetup)
		{
			writeEncode(getString("schedule:Procedures.HelpStandard", Setup.getAppOwner(getLocale())));
			write("<br><br>");
		}
		
		writeTypeAheadInput("procedure", null, null, 40, Procedure.MAXSIZE_NAME, getPageURL(ProcedureOrTypeTypeAhead.COMMAND));
		write(" ");
		writeButton("add", getString("controls:Button.Add"));
		write("<br><br>");		
		
		if(standardProcedures.size() == 0)
		{
			writeEncode(getString("schedule:Procedures.EmptyStandardProcedures"));
		}
		else
		{
			new DataTableControl<UUID>(this, "standardprocs", standardProcedures)
			{
				@Override
				protected void defineColumns() throws Exception
				{
//					if (!guidedSetup)
					{
						column("").width(1);
					}
					column(getString("schedule:Procedures.Procedure"));
					column(getString("schedule:Procedures.Type"));
	
					//add facilities codes
					for(UUID facilityID : facilityIDs)
					{
						final Facility facility = FacilityStore.getInstance().load(facilityID);
						
//						StringBuilder html = new StringBuilder();
//						html.append("<span title=\"");
//						html.append(Util.htmlEncode(facility.getName()));
//						html.append("\">");
//						html.append(facility.getCode());
//						html.append("</span>");

//						writeTooltip(facility.getCode(), facility.getName());
						column(facility.getCode()).align("center").alignHeader("center").html(new WebPage()
						{
							@Override
							public void renderHTML() throws Exception
							{
								if (!phone)
								{
									writeTooltipRightAligned(facility.getCode(), Util.htmlEncode(facility.getName()));
								}
								else
								{
									writeTooltip(facility.getCode(), Util.htmlEncode(facility.getName()));
								}
							}
						});
					}
				}
	
				@Override
				protected void renderRow(UUID procedureID) throws Exception
				{
					Procedure procedure = ProcedureStore.getInstance().load(procedureID);
					
//					if (!guidedSetup)
					{
						cell();
						writeCheckbox("chk_" + procedure.getID().toString(), null, false);
					}
					
					cell();
					writeEncode(procedure.getName());
					// !$! Should be a link to view only screen of the standard procedure
	
					cell();
					ProcedureType type = ProcedureTypeStore.getInstance().load(procedure.getTypeID());
					writeEncode(type.getName());
	
					//add facilities checkboxes
					for(UUID facilityID : facilityIDs)
					{
						cell();
						boolean active = ProcedureFacilityLinkStore.getInstance().isProcedureAssignedToFacility(procedure.getID(), facilityID);
						writeCheckbox("facility_" + procedure.getID() + "_" + facilityID, null, active);
					}
				}
	
			}.render();
		}
				
		write("<br>");
		if (standardProcedures.size()>0 )
		{
			writeSaveButton("save", null);
			if (!guidedSetup)
			{
				write(" ");
				writeRemoveButton("remove");
			}
		}
		
		writeFormClose();
	}
	
	private void renderCustomProcs(List<UUID> customProcedures, final List<UUID> facilityIDs) throws Exception
	{
		writeFormOpen();

		writeEncode(getString("schedule:Procedures.HelpCustom"));
		write("<br><br>");

		//create new procedure link
		new LinkToolbarControl(this)
			.addLink(getString("schedule:Procedures.CreateNew"), getPageURL(ProcedurePage.COMMAND), "icons/standard/pencil-16.png")
			.render();

		if(customProcedures.size() == 0)
		{
			writeEncode(getString("schedule:Procedures.EmptyCustomProcedures"));
		}
		else
		{
			new DataTableControl<UUID>(this, "customprocs", customProcedures)
			{
				@Override
				protected void defineColumns() throws Exception
				{
					column("").width(1);
					column(getString("schedule:Procedures.Procedure"));
					column(getString("schedule:Procedures.Type"));
					//add facilities codes
					for(UUID facilityID : facilityIDs)
					{
						Facility facility = FacilityStore.getInstance().load(facilityID);
//						writeTooltip(facility.getCode(), facility.getName());
						column(facility.getCode()).align("center").alignHeader("center");
					}
				}
	
				@Override
				protected void renderRow(UUID procedureID) throws Exception
				{
					Procedure procedure = ProcedureStore.getInstance().load(procedureID);
	
					cell();
					writeCheckbox("chk_" + procedure.getID().toString(), null, false);
	
					cell();
					writeLink(procedure.getName(),
								getPageURL(ProcedurePage.COMMAND, new ParameterMap(CommonProcedurePage.PARAM_ID, procedure.getID().toString())));
	
					cell();
					ProcedureType type = ProcedureTypeStore.getInstance().load(procedure.getTypeID());
					writeEncode(type.getName());
	
					//add facilities checkboxes
					for(UUID facilityID : facilityIDs)
					{
						cell();
						boolean active = ProcedureFacilityLinkStore.getInstance().isProcedureAssignedToFacility(procedure.getID(), facilityID);
						writeCheckbox("facility_" + procedure.getID() + "_" + facilityID, null, active);
					}
				}
	
			}.render();
		}
		write("<br>");
		
		if (customProcedures.size()>0)
		{
			writeSaveButton("save", null);
			write(" ");
			writeRemoveButton("remove");
		}
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("schedule:Procedures.Title");
	}
}
