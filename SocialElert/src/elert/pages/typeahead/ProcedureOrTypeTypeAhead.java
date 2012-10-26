package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.ProcedureType;
import elert.database.ProcedureTypeStore;
import samoyan.apps.system.TypeAhead;
import samoyan.core.Util;
import samoyan.servlet.WebPage;

/**
 * Allows schedulers to select standard procedures, or procedure types.
 * @author brian
 *
 */
public final class ProcedureOrTypeTypeAhead extends WebPage
{
	public static final String COMMAND = "procedure-or-type.typeahead";
	
	public ProcedureOrTypeTypeAhead()
	{
		setChild(new TypeAhead()
		{
			@Override
			protected void doQuery(String query) throws SQLException, Exception
			{
				List<UUID> ids = ProcedureTypeStore.getInstance().searchByText(query);
				for(UUID id : ids)	
				{
					ProcedureType procType = ProcedureTypeStore.getInstance().load(id);
					if(procType != null)
					{
						int count = ProcedureStore.getInstance().queryStandardByType(id).size();
						
						StringBuilder html = new StringBuilder();
						html.append(Util.htmlEncode(procType.getName()));
						html.append(" (");
						html.append(count);
						html.append(")");
						
						addOption(procType.getID(), procType.getName(), html.toString());
					}
				}

				ids = ProcedureStore.getInstance().searchByName(query, false);
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
		});
	}	
}
