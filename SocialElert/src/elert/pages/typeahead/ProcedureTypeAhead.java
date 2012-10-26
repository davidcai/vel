package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.apps.system.TypeAhead;
import samoyan.core.Util;
import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.ProcedureType;
import elert.database.ProcedureTypeStore;

public class ProcedureTypeAhead extends TypeAhead
{
	public static final String COMMAND = "procedure.typeahead";
		
	@Override
	protected void doQuery(String query) throws SQLException, Exception
	{
		List<UUID> ids = ProcedureStore.getInstance().searchByName(query, false);
		for(UUID id : ids)	
		{
			Procedure procedure = ProcedureStore.getInstance().load(id);
			ProcedureType type = ProcedureTypeStore.getInstance().load(procedure.getTypeID());
			
			StringBuilder html = new StringBuilder();
			html.append(Util.htmlEncode(procedure.getName()));
			html.append(" <span class=Faded>(");
			html.append(Util.htmlEncode(type.getName()));
			html.append(")</span>");
			
			addOption(procedure.getID(), procedure.getName(), html.toString());
		}
	}
}
