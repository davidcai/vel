package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.apps.system.TypeAhead;
import samoyan.servlet.WebPage;
import elert.database.ServiceArea;
import elert.database.ServiceAreaStore;

public class ServiceAreaTypeAhead extends WebPage
{
	public static final String COMMAND = "servicearea.typeahead";
	
	private static class ServiceAreaEx extends TypeAhead
	{
		@Override
		protected void doQuery(String query) throws SQLException, Exception
		{
			List<UUID> areaIDs = ServiceAreaStore.getInstance().searchByName(query);
			for(UUID areaID : areaIDs)	
			{
				ServiceArea area = ServiceAreaStore.getInstance().load(areaID);
				addOption(area.getID(), area.getName());
			}
		}
	}
	
	public ServiceAreaTypeAhead()
	{
		setChild(new ServiceAreaEx());
	}	
}
