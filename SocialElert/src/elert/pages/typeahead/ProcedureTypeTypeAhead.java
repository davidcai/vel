package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.apps.system.TypeAhead;
import elert.database.ProcedureType;
import elert.database.ProcedureTypeStore;

public class ProcedureTypeTypeAhead extends TypeAhead
{
public static final String COMMAND = "proceduretype.typeahead";
	
	@Override
	protected void doQuery(String query) throws SQLException, Exception
	{
		List<UUID> ids = ProcedureTypeStore.getInstance().searchByText(query);
		for(UUID id : ids)	
		{
			ProcedureType procType = ProcedureTypeStore.getInstance().load(id);
			if(procType != null)
				addOption(procType.getID(), procType.getName());		
		}
	}
}
