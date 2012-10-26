package elert.pages.govern;

import java.util.Locale;

import samoyan.controls.PhoneInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Country;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.ServiceArea;
import elert.database.ServiceAreaStore;
import elert.pages.ElertPage;

public class FacilityPage extends ElertPage
{
	public static final String COMMAND = ElertPage.COMMAND_GOVERN + "/facility";
	public static final String PARAM_ID = "id";	
	public static final String PARAM_SERVICE_AREA_ID = "area";	
	
	private Facility facility = null;
	private ServiceArea area = null;
		
	@Override
	public void init() throws Exception
	{
		this.facility = FacilityStore.getInstance().open(getParameterUUID(PARAM_ID));
		if (this.facility == null)
		{
			this.facility = new Facility();
			
			this.area = ServiceAreaStore.getInstance().load(getParameterUUID(PARAM_SERVICE_AREA_ID));
			if (this.area==null)
			{
				throw new PageNotFoundException();
			}
		}
		else
		{
			this.area = ServiceAreaStore.getInstance().load(this.facility.getServiceAreaID());
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		String code = validateParameterString("Code", 1, Facility.MAXSIZE_CODE);
		Facility storedFacility = FacilityStore.getInstance().loadByCode(code);
		if(storedFacility != null)
		{
			if(!facility.isSaved() || !storedFacility.getID().equals(facility.getID()))
				throw new WebFormException("Code", getString("govern:Facility.AlreadyExists", code));
		}
		
		validateParameterString("Name", 1, Facility.MAXSIZE_NAME);		
//		validateParameterString("City", 1, Facility.MAXSIZE_CITY);
//		validateParameterString("State", 2, Facility.MAXSIZE_STATE);
		
		if (!Util.isEmpty(getParameterString("Phone")))
		{
			validateParameterPhone("Phone");
		}
	}


	@Override
	public void commit() throws Exception
	{		
		facility.setCode(getParameterString("Code"));
		facility.setName(getParameterString("Name"));		
		facility.setServiceAreaID(this.area.getID());
		facility.setAddress(getParameterString("Address"));
		facility.setCity(getParameterString("City"));
		facility.setState(getParameterString("State").toUpperCase(Locale.US));
		facility.setZip(getParameterString("Zip"));
		facility.setPhone(getParameterPhone("Phone"));
		
		FacilityStore.getInstance().save(facility);		
		
		throw new RedirectException(FacilitiesPage.COMMAND, new ParameterMap(FacilitiesPage.PARAM_SERVICE_AREA_ID, this.area.getID().toString()));
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("govern:Facility.Code"));
		twoCol.writeTextInput("Code",  facility.getCode() , 4, Facility.MAXSIZE_CODE);
		
		twoCol.writeRow(getString("govern:Facility.Name"));
		twoCol.writeTextInput("Name",  facility.getName(), 40, Facility.MAXSIZE_NAME);		
				
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("govern:Facility.Address"));
		twoCol.writeTextInput("Address",  facility.getAddress(), 40, Facility.MAXSIZE_ADDRESS);
		
		twoCol.writeRow(getString("govern:Facility.City"));
		twoCol.writeTextInput("City",  facility.getCity(), 40, Facility.MAXSIZE_CITY);
		
		twoCol.writeRow(getString("govern:Facility.State"));
		twoCol.writeTextInput("State",  facility.getState(), Facility.MAXSIZE_STATE, Facility.MAXSIZE_STATE);
		
		twoCol.writeRow(getString("govern:Facility.Zip"));
		twoCol.writeTextInput("Zip",  facility.getZip(), Facility.MAXSIZE_ZIP, Facility.MAXSIZE_ZIP);
		
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("govern:Facility.Phone"));
		new PhoneInputControl(twoCol, "Phone")
			.limitCountry(Country.UNITED_STATES)
			.setInitialValue(facility.getPhone())
			.render();
//		twoCol.writePhoneInput("Phone",  facility.getPhone());
		
		twoCol.render();
		
		write("<br>");
		writeSaveButton(this.facility);
		
		// Postback facility ID
		writeHiddenInput(PARAM_ID, null);
		writeHiddenInput(PARAM_SERVICE_AREA_ID, null);
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return facility.isSaved() ? facility.getName() : getString("govern:Facility.Title", this.area.getName());
	}
}
