package elert.pages.govern;

import java.util.List;
import java.util.UUID;

import samoyan.controls.CheckboxInputControl;
import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.RedirectException;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Region;
import elert.database.RegionStore;
import elert.database.ServiceArea;
import elert.database.ServiceAreaStore;
import elert.pages.ElertPage;

public class ServiceAreasPage extends ElertPage
{
	public static final String COMMAND = ElertPage.COMMAND_GOVERN + "/service-areas";

	private static final String REMOVE_PARAM = "remove";

	@Override
	public void commit() throws Exception
	{
		for (String prmName : getContext().getParameterNamesThatStartWith("chk_"))
		{
			UUID areaID = UUID.fromString(prmName.substring(4));
			ServiceAreaStore.getInstance().remove(areaID);
		}
		
		throw new RedirectException(getContext().getCommand(), null);
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeEncode(getString("govern:ServiceAreas.Help", Setup.getAppOwner(getLocale()), Setup.getAppTitle(getLocale())));
		write("<br><br>");
		
		//create new service area link
		new LinkToolbarControl(this)
			.addLink(getString("govern:ServiceAreas.CreateNew"), getPageURL(ServiceAreaPage.COMMAND), "icons/basic1/pencil_16.png")
			.render();

		// Load regions
		List<UUID> regionIDs = RegionStore.getInstance().getAllIDs();
		if (regionIDs.size()==0)
		{
			writeEncode(getString("govern:ServiceAreas.NoResults"));
			return;
		}

		writeFormOpen();

		// For each region, show service areas
		for (UUID regionID : regionIDs)
		{
			Region region = RegionStore.getInstance().load(regionID);
			write("<h2>");
			writeEncode(region.getName());
			write("</h2>");
			
			List<UUID> areaIDs = ServiceAreaStore.getInstance().getByRegion(regionID);
			if (areaIDs.size()==0)
			{
				writeEncode(getString("govern:ServiceAreas.NoResults"));
				write("<br><br>");
				continue;
			}

			new DataTableControl<UUID>(this, "areas_" + regionID.toString(), areaIDs)
			{
				@Override
				protected void defineColumns() throws Exception
				{
					column("").width(1);
					
					column(getString("govern:ServiceAreas.Name")).width(39);
					column(getString("govern:ServiceAreas.Facilities")).width(60);				
				}

				@Override
				protected void renderRow(UUID areaID) throws Exception
				{
					ServiceArea area = ServiceAreaStore.getInstance().load(areaID);

					cell();
					new CheckboxInputControl(this, "chk_" + area.getID().toString())
						.setDisabled(ServiceAreaStore.getInstance().canRemove(areaID)==false)
						.render();
//					if (ServiceAreaStore.getInstance().canRemoveBean(areaID))
//					{
//						writeCheckbox("chk_" + area.getID().toString(), false);
//					}
//					else
//					{
//						write("<input type=checkbox disabled>");
//					}
					
					cell();
					writeLink(area.getName(),
							getPageURL(ServiceAreaPage.COMMAND, new ParameterMap(ServiceAreaPage.PARAM_ID, area.getID().toString())));
					
					cell();
					String url = getPageURL(FacilitiesPage.COMMAND, new ParameterMap(FacilitiesPage.PARAM_SERVICE_AREA_ID, area.getID().toString()));
					List<UUID> facilityIDs = FacilityStore.getInstance().queryByServiceArea(area.getID());
					if (facilityIDs.size()==0)
					{
						writeLink(getString("govern:ServiceAreas.NoFacilities"), url);
					}
					else
					{
						write("<a href=\"");
						writeEncode(url);
						write("\">");
						for (int f=0; f<facilityIDs.size(); f++)
						{
							Facility facility = FacilityStore.getInstance().load(facilityIDs.get(f));
							if (f>0)
							{
								write(", ");
							}
							writeEncode(facility.getCode());
						}
						write("</a>");
					}
				}
			}.render();
			write("<br>");
		}
				
		writeRemoveButton(REMOVE_PARAM);
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("govern:ServiceAreas.Title");
	}
}
