package baby.pages.content;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.apps.system.TypeAhead;
import baby.database.UnitType;
import baby.database.UnitTypeStore;

public class UnitTypeTypeAhead extends TypeAhead
{
	public static final String COMMAND = "unitType.typeahead";
	
	@Override
	protected void doQuery(String q) throws SQLException, Exception
	{
		List<UUID> ids = UnitTypeStore.getInstance().getAll();
		for (UUID id : ids)
		{
			UnitType ut = UnitTypeStore.getInstance().load(id);
			
			String imperial = ut.getImperialLabel();
			String metric = ut.getMetricLabel();
			
			if (imperial.toLowerCase(getLocale()).indexOf(q) >= 0 || 
				metric.toLowerCase(getLocale()).indexOf(q) >= 0)
			{
				addOption(ut.getID(), imperial + "/" + metric);
			}
		}
	}
}
