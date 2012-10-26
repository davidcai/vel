package mind.pages;

import java.sql.SQLException;
import java.util.List;

import mind.database.EquipmentStore;
import samoyan.apps.system.TypeAhead;

public class IndustryTypeAhead extends TypeAhead
{
	public static final String COMMAND = "industry.typeahead";

	@Override
	protected void doQuery(String query) throws SQLException, Exception
	{
		List<String> industries = EquipmentStore.getInstance().searchIndustryByName(query);
		for (String industry : industries)
		{
			addOption(industry);
		}
	}
}
