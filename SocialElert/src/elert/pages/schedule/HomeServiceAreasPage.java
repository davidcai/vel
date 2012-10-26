package elert.pages.schedule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.controls.ControlArray;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import elert.database.ServiceArea;
import elert.database.ServiceAreaUserLinkStore;
import elert.database.ServiceAreaStore;
import elert.pages.ElertPage;
import elert.pages.typeahead.ServiceAreaTypeAhead;

public class HomeServiceAreasPage extends ElertPage
{
	public static final String COMMAND = ElertPage.COMMAND_SCHEDULE + "/home-areas";
	
	private static final String PARAM_HOME_AREA = "home_";
	private static final String PARAM_NEIGHBOUR_AREA = "neighbor_";

	@Override
	public void validate() throws Exception
	{
		//for each area entered check if it exists, otherwise give error message		
		Set<UUID> homeAreas = new HashSet<UUID>();
		Integer homeAreasCount = getParameterInteger("homeareas");
		for(int i = 0; i < homeAreasCount; i++)
		{
			Pair<String, String> field = getParameterTypeAhead(PARAM_HOME_AREA + i);
			if (field!=null && !Util.isEmpty(field.getValue()))
			{
				if (!Util.isUUID(field.getKey()))
				{
					throw new WebFormException(PARAM_HOME_AREA + i, getString("elert:Errors.InvalidServiceArea",  field.getValue()));
				}
				
				UUID areaID = UUID.fromString(field.getKey());
				
				ServiceArea area = ServiceAreaStore.getInstance().load(areaID);
				if(area == null)
					throw new WebFormException(PARAM_HOME_AREA + i, getString("elert:Errors.InvalidServiceArea",  field.getValue()));
				
				homeAreas.add(areaID);
			}
		}
		
		Integer neighbourAreasCount = getParameterInteger("neighbourareas");
		for(int i = 0; i < neighbourAreasCount; i++)
		{
			Pair<String, String> field = getParameterTypeAhead(PARAM_NEIGHBOUR_AREA + i);
			if(field != null && !Util.isEmpty(field.getValue()))
			{
				if (!Util.isUUID(field.getKey()))
				{
					throw new WebFormException(PARAM_HOME_AREA + i, getString("elert:Errors.InvalidServiceArea",  field.getValue()));
				}

				UUID areaID = UUID.fromString(field.getKey());

				if(homeAreas.contains(areaID))
					throw new WebFormException(PARAM_NEIGHBOUR_AREA + i, getString("schedule:HomeServiceAreas.DuplicateAreaDef", field.getValue()));
					
				ServiceArea area = ServiceAreaStore.getInstance().load(areaID);
				if(area == null)
					throw new WebFormException(PARAM_NEIGHBOUR_AREA + i, getString("elert:Errors.InvalidServiceArea", field.getValue()));
			}
		}
	}

	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();

		// Unassign all areas
		ServiceAreaUserLinkStore.getInstance().unassignAreasForUser(ctx.getUserID());
		
		// Assign home areas
		Integer homeAreasCount = getParameterInteger("homeareas");
		for(int i = 0; i < homeAreasCount; i++)
		{
			Pair<String, String> field = getParameterTypeAhead(PARAM_HOME_AREA + i);
			if(field != null && !Util.isEmpty(field.getValue()))
			{
				UUID areaID = UUID.fromString(field.getKey());
				ServiceAreaUserLinkStore.getInstance().assignHomeAreaForUser(ctx.getUserID(), areaID);
			}
		}
		
		// Assign neighboring areas
		Integer neighbourAreasCount = getParameterInteger("neighbourareas");
		for(int i = 0; i < homeAreasCount; i++)
		{
			Pair<String, String> field = getParameterTypeAhead(PARAM_NEIGHBOUR_AREA + i);
			if(field != null && !Util.isEmpty(field.getValue()))
			{
				UUID areaID = UUID.fromString(field.getKey());
				ServiceAreaUserLinkStore.getInstance().assignNeighboringAreaForUser(ctx.getUserID(), areaID);
			}
		}
		
		// Support guided setup
		progressGuidedSetup();
		
		// self redirect to clean the form after save
		throw new RedirectException(ctx.getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		writeFormOpen();

		final TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Home areas
		List<UUID> homeAreaIDs = ServiceAreaUserLinkStore.getInstance().getHomeSerivceAreasForUser(ctx.getUserID());
		
		twoCol.writeTextRow(getString("schedule:HomeServiceAreas.HomeAreaDescription"));
		twoCol.writeSpaceRow();
		twoCol.writeRow(getString("schedule:HomeServiceAreas.HomeAreas"));
		new ControlArray<UUID>(twoCol, "homeareas", homeAreaIDs)
		{
			@Override
			public void renderRow(int rowNum, UUID areaID) throws Exception
			{
				ServiceArea area = ServiceAreaStore.getInstance().load(areaID);
				String areaName = (area != null) ? area.getName() : null;
				writeTypeAheadInput(PARAM_HOME_AREA + rowNum, areaID, areaName, 40, ServiceArea.MAXSIZE_NAME,
						getPageURL(ServiceAreaTypeAhead.COMMAND));
			}
		}.render();
		
		twoCol.writeSpaceRow();

		// Neighboring areas
		List<UUID> neighboringAreaIDs = ServiceAreaUserLinkStore.getInstance().getNeighboringSerivceAreasForUser(ctx.getUserID());

		twoCol.writeTextRow(getString("schedule:HomeServiceAreas.NeighbourAreaDescription"));
		twoCol.writeSpaceRow();
		twoCol.writeRow(getString("schedule:HomeServiceAreas.NeighbourAreas"));
		new ControlArray<UUID>(twoCol, "neighbourareas", neighboringAreaIDs)
		{
			@Override
			public void renderRow(int rowNum, UUID areaID) throws Exception
			{
				ServiceArea area = ServiceAreaStore.getInstance().load(areaID);
				String areaName = (area != null) ? area.getName() : null;
				writeTypeAheadInput(PARAM_NEIGHBOUR_AREA + rowNum, areaID, areaName, 40, ServiceArea.MAXSIZE_NAME,
						getPageURL(ServiceAreaTypeAhead.COMMAND));
			}
		}.render();
		
		twoCol.render();

		write("<br>");
		writeSaveButton(null);
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("schedule:HomeServiceAreas.Title");
	}
}
