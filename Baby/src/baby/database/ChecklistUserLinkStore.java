package baby.database;

import java.sql.SQLException;
import java.util.UUID;

import samoyan.database.LinkStore;
import samoyan.database.LinkTableDef;

public final class ChecklistUserLinkStore extends LinkStore
{
	private static ChecklistUserLinkStore instance = new ChecklistUserLinkStore();

	protected ChecklistUserLinkStore()
	{
	}
	
	public final static ChecklistUserLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = createLinkTableDef("ChecklistUserLink");

		td.setKey1("ChecklistID", "Checklists");
		td.setKey2("UserID", "Users");
		
		return td;
	}

	// - - -
	
	public void collapse(UUID checklistID, UUID userID) throws SQLException
	{
		getInstance().link(checklistID, userID);
	}
	
	public void expand(UUID checklistID, UUID userID) throws SQLException
	{
		getInstance().unlink(checklistID, userID);
	}

	public boolean isCollapsed(UUID checklistID, UUID userID) throws SQLException
	{
		return getInstance().isLinked(checklistID, userID);
	}
}
