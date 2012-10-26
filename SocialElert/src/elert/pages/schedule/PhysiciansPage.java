package elert.pages.schedule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.controls.DataTableControl;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.database.UserStore;
import samoyan.database.UserUserGroupLinkStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import elert.app.ElertConsts;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.PhysicianFacilityLinkStore;
import elert.pages.ElertPage;
import elert.pages.typeahead.PhysicianTypeAhead;

public class PhysiciansPage extends ElertPage
{
	public static final String COMMAND = ElertPage.COMMAND_SCHEDULE + "/physicians";

	@Override
	public void validate() throws Exception
	{
		if(isParameter("add"))
		{
			Pair<String, String> physicianKvp = getParameterTypeAhead("physician");
			if(Util.isEmpty(physicianKvp.getValue()))
				throw new RedirectException(PhysicianProfilePage.COMMAND, null);
				
			if(Util.isEmpty(physicianKvp.getKey()))
				throw new RedirectException(PhysicianProfilePage.COMMAND, new ParameterMap(PhysicianProfilePage.PARAM_NAME, physicianKvp.getValue()));
			

			// Verify user exists
			User physician = UserStore.getInstance().load(UUID.fromString(physicianKvp.getKey()));
			if(physician == null || physician.isTerminated())
			{
				throw new WebFormException("physician",
						getString("schedule:Physicians.NoSuchPhysicianError", physicianKvp.getValue()));
			}

			// Verify user is in physicians group
			UserGroup grp = UserGroupStore.getInstance().loadByName(ElertConsts.GROUP_PHYSICIANS);
			if (!UserUserGroupLinkStore.getInstance().isUserInGroup(physician.getID(), grp.getID()))
			{
				throw new WebFormException("physician", getString("schedule:Physicians.NoSuchPhysicianError",  physicianKvp.getValue()));
			}
		}
	}

	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();

		//fetch all facilities associated with the current user
		final List<UUID> facilityIDs = FacilityStore.getInstance().queryByUser(ctx.getUserID());

		// Remove a physician
		if(isParameter("remove"))
		{
			for (String prmName : getContext().getParameterNamesThatStartWith("chk_"))
			{
				UUID physicianID = UUID.fromString(prmName.substring(4));
				for(UUID facilityID : facilityIDs)
				{
					PhysicianFacilityLinkStore.getInstance().removePhysicianFromFacility(physicianID, facilityID);
				}
			}
		}

		// Load all physicians linked to these facilities
		Set<UUID> physicianSet = new HashSet<UUID>();
		for(UUID facilityID : facilityIDs)
		{
			physicianSet.addAll(PhysicianFacilityLinkStore.getInstance().getPhysiciansByFacility(facilityID));
		}

		// Add a physician
		if(isParameter("add"))
		{
			Pair<String, String> physicianKvp = getParameterTypeAhead("physician");
			UUID physicianID = UUID.fromString(physicianKvp.getKey());
			if(physicianSet.contains(physicianID) == false)
			{
				//this is new physician to be added to the list
				for(UUID facilityID : facilityIDs)
				{
					if (PhysicianFacilityLinkStore.getInstance().isPhysicianAssignedToFacility(physicianID, facilityID)==false)
					{
						PhysicianFacilityLinkStore.getInstance().unassignPhysicianFromFacility(physicianID, facilityID);
					}
				}
			}
		}

		// Change physician assignments
		if(isParameter("save"))
		{
			for(UUID facilityID : facilityIDs)
			{
				for(UUID physicianID : physicianSet)
				{
					if(isParameter("facility_" + physicianID.toString() + "_" + facilityID.toString()))
					{
						PhysicianFacilityLinkStore.getInstance().assignPhysicianToFacility(physicianID, facilityID);
					}
					else
					{
						PhysicianFacilityLinkStore.getInstance().unassignPhysicianFromFacility(physicianID, facilityID);
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
		final boolean phone = ctx.getUserAgent().isSmartPhone();

		//fetch all facilities associated with the current user
		final List<UUID> facilityIDs = FacilityStore.getInstance().queryByUser(ctx.getUserID());
		if (facilityIDs.size()==0)
		{
			StringBuilder link = new StringBuilder();
			link.append("<a href=\"");
			link.append(getPageURL(HomeServiceAreasPage.COMMAND));
			link.append("\">");
			link.append(Util.htmlEncode(getString("schedule:Physicians.HomeServiceArea")));
			link.append("</a>");
			String pattern = Util.htmlEncode(getString("schedule:Physicians.NoFacilities", "$link$"));
			write(Util.strReplace(pattern, "$link$", link.toString()));
			return;
		}
		
		writeEncode(getString("schedule:Physicians.Description"));
		write("<br><br>");

		writeFormOpen();
		writeTypeAheadInput("physician", null, null, 40, User.MAXSIZE_NAME, getPageURL(PhysicianTypeAhead.COMMAND));
		write(" ");
		writeButton("add", getString("controls:Button.Add"));
		write("<br><br>");
		writeFormClose();

		// Load all physicians linked to these facilities
		Set<UUID> physicianSet = new HashSet<UUID>();
		for(UUID facilityID : facilityIDs)
		{
			physicianSet.addAll(PhysicianFacilityLinkStore.getInstance().getPhysiciansByFacility(facilityID));
		}
		if(physicianSet == null || physicianSet.size() == 0)
		{
			writeEncode(getString("schedule:Physicians.EmptyPhysiciansList"));
			return;
		}

		writeFormOpen();

		List<UUID> physicianIDs = new ArrayList<UUID>(physicianSet);
		new DataTableControl<UUID>(this, "physicians", physicianIDs)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column("").width(1);
				column(getString("schedule:Physicians.NameHeader"));
				//add facilities codes
				for(UUID facilityID : facilityIDs)
				{
					final Facility facility = FacilityStore.getInstance().load(facilityID);

//					StringBuilder html = new StringBuilder();
//					html.append("<span title=\"");
//					html.append(Util.htmlEncode(facility.getName()));
//					html.append("\">");
//					html.append(facility.getCode());
//					html.append("</span>");

//					writeTooltip(facility.getCode(), facility.getName());
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
			protected void renderRow(UUID physicianID) throws Exception
			{
				User physician = UserStore.getInstance().load(physicianID);

				cell();
				writeCheckbox("chk_" + physician.getID().toString(), null, false);

				cell();				
				writeLink(physician.getName(),
						getPageURL(PhysicianProfilePage.COMMAND, new ParameterMap(PhysicianProfilePage.PARAM_ID, physician.getID().toString())));
				
				//add facilities checkboxes
				for(UUID facilityID : facilityIDs)
				{
					cell();				
					boolean active = PhysicianFacilityLinkStore.getInstance().isPhysicianAssignedToFacility(physicianID, facilityID);
					writeCheckbox("facility_" + physician.getID() + "_" + facilityID, null, active);
				}
			}

		}.render();

		write("<br>");
		writeSaveButton("save", null);
		write(" ");
		writeRemoveButton("remove");

		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("schedule:Physicians.Title");
	}
}
