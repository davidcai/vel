package elert.pages.govern;

import java.util.List;
import java.util.UUID;

import samoyan.controls.CheckboxInputControl;
import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.exc.PageNotFoundException;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.ServiceArea;
import elert.database.ServiceAreaStore;
import elert.pages.ElertPage;

public class FacilitiesPage extends ElertPage
{
	public static final String COMMAND = ElertPage.COMMAND_GOVERN + "/facilities";

	public static final String PARAM_SERVICE_AREA_ID = "area";
	
	private ServiceArea area;
	
	@Override
	public void init() throws Exception
	{
		this.area = ServiceAreaStore.getInstance().open(getParameterUUID(PARAM_SERVICE_AREA_ID));	
		if (this.area == null)
		{
			throw new PageNotFoundException();
		}
	}

	@Override
	public void commit() throws Exception
	{
		if(isParameter("remove"))
		{
			for (String prmName : getContext().getParameterNamesThatStartWith("chk_"))
			{
				UUID facilityID = UUID.fromString(prmName.substring(4));
				FacilityStore.getInstance().remove(facilityID);
			}
		}
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeEncode(getString("govern:Facilities.Help", this.area.getName()));
		write("<br><br>");

		//create new facility link
		new LinkToolbarControl(this)
			.addLink(	getString("govern:Facilities.CreateNew"),
						getPageURL(FacilityPage.COMMAND, new ParameterMap(FacilityPage.PARAM_SERVICE_AREA_ID, this.area.getID().toString())),
						"icons/basic1/pencil_16.png")
			.render();

		writeFormOpen();
		
		List<UUID> facilities = FacilityStore.getInstance().queryByServiceArea(this.area.getID());
		if(facilities.size() == 0)
		{
			writeEncode(getString("govern:Facilities.NoResults"));
			return;
		}
			
		new DataTableControl<UUID>(this, "facilities", facilities)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column("").width(1);
				column(getString("govern:Facilities.Code"));
				column(getString("govern:Facilities.Name"));
			}

			@Override
			protected void renderRow(UUID facilityID) throws Exception
			{
				Facility facility = FacilityStore.getInstance().load(facilityID);
				
				cell();
				new CheckboxInputControl(this, "chk_" + facility.getID().toString())
					.setDisabled(FacilityStore.getInstance().canRemoveBean(facilityID)==false)
					.render();
//				if (FacilityStore.getInstance().canRemoveBean(facilityID))
//				{
//					writeCheckbox("chk_" + facility.getID().toString(), null, false);
//				}
//				else
//				{
//					write("<input type=checkbox disabled>");
//				}
				
				cell();
				writeLink(facility.getCode(),
						getPageURL(FacilityPage.COMMAND, new ParameterMap(FacilityPage.PARAM_ID, facility.getID().toString())));
				
				cell();
				writeEncode(facility.getName());
			}
		}.render();
			
		write("<br>");		
		writeRemoveButton("remove");
		
		// Report
		writeHiddenInput(PARAM_SERVICE_AREA_ID, null);
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("govern:Facilities.Title", this.area.getName());
	}
}
