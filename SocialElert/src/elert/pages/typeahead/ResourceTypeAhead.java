package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.apps.system.TypeAhead;
import samoyan.servlet.WebPage;
import elert.database.Resource;
import elert.database.ResourceStore;

public class ResourceTypeAhead extends WebPage
{
	public static final String COMMAND = "resource.typeahead";
	
	public ResourceTypeAhead()
	{
		setChild(new TypeAhead()
		{
			@Override
			protected void doQuery(String query) throws SQLException, Exception
			{
				List<UUID> resourceIDs = ResourceStore.getInstance().searchByName(query);
				for(UUID resourceID : resourceIDs)
				{
					Resource res = ResourceStore.getInstance().load(resourceID);
					addOption(res.getID(), res.getName());
				}
			}
		});
	}
}
