package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import elert.database.FacilityStore;
import elert.database.Procedure;
import elert.database.ProcedureFacilityLinkStore;
import elert.database.ProcedureStore;
import elert.database.ProcedureType;
import elert.database.ProcedureTypeStore;
import samoyan.apps.system.TypeAhead;
import samoyan.core.Util;
import samoyan.servlet.WebPage;

public class HomeProcedureTypeAhead extends WebPage
{
	public static final String COMMAND = "home-procedure.typeahead";
		
	public HomeProcedureTypeAhead()
	{
		setChild(new TypeAhead()
		{
			@Override
			protected void doQuery(String query) throws SQLException, Exception
			{
				List<UUID> myFacilities = FacilityStore.getInstance().queryByUser(getContext().getUserID());

				List<UUID> procIDs = ProcedureStore.getInstance().searchByName(query, true);
				for(UUID procID : procIDs)	
				{
					for(UUID facilityID : myFacilities)
					{
						if(ProcedureFacilityLinkStore.getInstance().isProcedureAssignedToFacility(procID, facilityID))
						{
							Procedure procedure = ProcedureStore.getInstance().load(procID);
							ProcedureType type = ProcedureTypeStore.getInstance().load(procedure.getTypeID());
							
							StringBuilder html = new StringBuilder();
							html.append(Util.htmlEncode(procedure.getName()));
							html.append(" <span class=Faded>(");
							html.append(Util.htmlEncode(type.getName()));
							html.append(")</span>");
							
							addOption(procedure.getID(), procedure.getName(), html.toString());
							break;
						}
					}						
				}
			}
		});
	}	
}
