package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.apps.system.TypeAhead;
import samoyan.database.User;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.database.UserStore;
import samoyan.servlet.WebPage;
import elert.app.ElertConsts;
import elert.database.FacilityStore;
import elert.database.PhysicianFacilityLinkStore;

public class HomePhysicianTypeAhead extends WebPage
{
	public static final String COMMAND = "home-physician.typeahead";
		
	public HomePhysicianTypeAhead()
	{
		setChild(new TypeAhead()
		{
			@Override
			protected void doQuery(String query) throws SQLException, Exception
			{
				UserGroup grp = UserGroupStore.getInstance().loadByName(ElertConsts.GROUP_PHYSICIANS);				
				List<UUID> physicianIDs = UserStore.getInstance().searchByNameInGroup(query, grp.getID());
				
				List<UUID> myFacilities = FacilityStore.getInstance().queryByUser(getContext().getUserID());
				for(UUID physicianID : physicianIDs)
				{
					for(UUID facilityID : myFacilities)
					{
						if(PhysicianFacilityLinkStore.getInstance().isPhysicianAssignedToFacility(physicianID, facilityID))
						{
							User physician = UserStore.getInstance().load(physicianID);					
							addOption(physician.getID(), physician.getName());
							break;
						}
					}						
				}
			}
		});
	}	
}
