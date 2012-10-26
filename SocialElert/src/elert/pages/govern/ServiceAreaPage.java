package elert.pages.govern;

import java.util.List;
import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import elert.database.Facility;
import elert.database.Region;
import elert.database.RegionStore;
import elert.database.ServiceArea;
import elert.database.ServiceAreaStore;
import elert.pages.ElertPage;

public class ServiceAreaPage extends ElertPage
{
	public static final String COMMAND = ElertPage.COMMAND_GOVERN + "/service-area";
	public static final String PARAM_ID = "id";	
	
	private ServiceArea area = null;
		
	@Override
	public void init() throws Exception
	{
		this.area = ServiceAreaStore.getInstance().open(getParameterUUID(PARAM_ID));	
		if (this.area == null)
		{
			this.area = new ServiceArea();
		}
	}

	
	@Override
	public void validate() throws Exception
	{		
		String name = validateParameterString("Name", 1, Facility.MAXSIZE_NAME);
		
		ServiceArea byName = ServiceAreaStore.getInstance().loadByName(name);
		if (byName!=null && byName.getID().equals(this.area.getID())==false)
		{
			throw new WebFormException("name", getString("govern:ServiceArea.AlreadyExists"));
		}
		
		if (this.area.isSaved()==false)
		{
			if (!isParameter("radio"))
			{
				throw new WebFormException("radio", getString("common:Errors.MissingField"));
			}
			
			UUID regionID = getParameterUUID("radio");
			if (regionID==null)
			{
				validateParameterString("newregion", 1, Region.MAXSIZE_NAME);
			}
			else if (RegionStore.getInstance().load(regionID)==null)
			{
				throw new WebFormException("radio", getString("common:Errors.InvalidValue"));
			}
		}
	}


	@Override
	public void commit() throws Exception
	{
		boolean isSaved = this.area.isSaved();
		
		area.setName(getParameterString("Name"));
		
		if (isSaved==false)
		{
			UUID regionID = getParameterUUID("radio");
			if (regionID==null)
			{
				// Create new region on the fly
				Region region = new Region();
				region.setName(getParameterString("newregion"));			
				RegionStore.getInstance().save(region);	
				
				regionID = region.getID();
			}
			area.setRegionID(regionID);
		}
		
		ServiceAreaStore.getInstance().save(area);		
		
		if (isSaved==true)
		{
			throw new RedirectException(ServiceAreasPage.COMMAND, null);
		}
		else
		{
			throw new RedirectException(FacilitiesPage.COMMAND, new ParameterMap(FacilitiesPage.PARAM_SERVICE_AREA_ID, area.getID().toString()));
		}
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		if (this.area.isSaved()==false)
		{
			twoCol.writeTextRow(getString("govern:ServiceArea.Help"));
		}
		else
		{
			twoCol.writeTextRow(getString("govern:ServiceArea.HelpSaved"));
		}
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("govern:ServiceArea.Name"));
		twoCol.writeTextInput("Name",  area.getName(), 40, ServiceArea.MAXSIZE_NAME);		
		
		twoCol.writeSpaceRow();
				
		twoCol.writeRow(getString("govern:ServiceArea.Region"));
		if (this.area.isSaved()==false)
		{
			List<UUID> regionIDs = RegionStore.getInstance().getAllIDs();
			regionIDs.add(null);
			twoCol.write("<table>");
			for (int i=0; i<regionIDs.size(); i++)
			{
				UUID regionID = regionIDs.get(i);
				Region region = RegionStore.getInstance().load(regionID);
				
				twoCol.write("<tr valign=middle><td>");
				twoCol.writeRadioButton("radio", null, regionID==null? "" : regionID.toString(), null);
				twoCol.write("</td><td>");
				if (regionID==null)
				{
					twoCol.writeTextInput("newregion", null, 40, Region.MAXSIZE_NAME);
				}
				else
				{
					twoCol.writeEncode(region.getName());
				}
				twoCol.write("</td></tr>");
			}
			twoCol.write("</table>");
		}
		else
		{
			// Cannot edit the region of an already saved service area
			Region region = RegionStore.getInstance().load(this.area.getRegionID());
			twoCol.writeEncode(region.getName());
		}
		
		twoCol.render();
		
		write("<br>");
		writeSaveButton(area);

		// Postback param ID
		writeHiddenInput(PARAM_ID, null);

		writeFormClose();
		
		// Autoselect
		write("<script>$('INPUT[name=newregion]').change(function(ev){$('INPUT[name=radio]').last().attr('checked',1);});</script>");
	}

	@Override
	public String getTitle() throws Exception
	{
		return area.isSaved() ? area.getName() : getString("govern:ServiceArea.Title");
	}
}
