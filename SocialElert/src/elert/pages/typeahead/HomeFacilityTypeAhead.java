package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import samoyan.apps.system.TypeAhead;
import samoyan.servlet.WebPage;
import elert.database.Facility;
import elert.database.FacilityStore;

public class HomeFacilityTypeAhead extends WebPage
{
	public static final String COMMAND = "home-facility.typeahead";
		
	public HomeFacilityTypeAhead()
	{
		setChild(new TypeAhead()
		{
			@Override
			protected void doQuery(String query) throws SQLException, Exception
			{		
				List<UUID> myFacilities = FacilityStore.getInstance().queryByUser(getContext().getUserID()); //facilities belonging to the user's service areas
				List<UUID> facilityIDs = new ArrayList<UUID>(FacilityStore.getInstance().searchByText(query));
				facilityIDs.retainAll(myFacilities); //only show facilities in the user's service areas
				
				for(UUID facilityID : facilityIDs)
				{
					Facility facility = FacilityStore.getInstance().load(facilityID);
					addOption(facility.getID(), facility.getName());
				}
			}
		});
	}	
}
