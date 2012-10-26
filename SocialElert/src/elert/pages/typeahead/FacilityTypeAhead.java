package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import elert.database.Facility;
import elert.database.FacilityStore;
import samoyan.apps.system.TypeAhead;
import samoyan.servlet.WebPage;

public final class FacilityTypeAhead extends WebPage
{
	public static final String COMMAND = "facility.typeahead";
		
	public FacilityTypeAhead()
	{
		setChild(new TypeAhead()
		{
			@Override
			protected void doQuery(String query) throws SQLException, Exception
			{		
				List<UUID> facilityIDs = FacilityStore.getInstance().searchByText(query);
				for(UUID facilityID : facilityIDs)
				{
					Facility facility = FacilityStore.getInstance().load(facilityID);
					addOption(facility.getID(), facility.getName());
				}
			}
		});
	}	
}
