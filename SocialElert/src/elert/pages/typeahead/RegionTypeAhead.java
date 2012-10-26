package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.apps.system.TypeAhead;
import elert.database.Region;
import elert.database.RegionStore;

public class RegionTypeAhead extends TypeAhead
{
	public static final String COMMAND = "region.typeahead";
		
	@Override
	protected void doQuery(String query) throws SQLException, Exception
	{
		List<UUID> regionNames = RegionStore.getInstance().searchByName(query);
		for(UUID regionID : regionNames)
		{
			Region region = RegionStore.getInstance().load(regionID);
			addOption(region.getID(), region.getName());
		}
	}
}
