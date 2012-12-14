package baby.database;

import java.sql.SQLException;
import java.util.UUID;

import samoyan.database.LinkStore;
import samoyan.database.LinkTableDef;

public final class CheckItemUserLinkStore extends LinkStore
{
	private static CheckItemUserLinkStore instance = new CheckItemUserLinkStore();

	protected CheckItemUserLinkStore()
	{
	}
	
	public final static CheckItemUserLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = createLinkTableDef("CheckItemUserLink");

		td.setKey1("CheckItemID", "CheckItems");
		td.setKey2("UserID", "Users");
		
		return td;
	}

	// - - -
	
	public void check(UUID checkitemID, UUID userID) throws SQLException
	{
		getInstance().link(checkitemID, userID);
	}
	
	public void uncheck(UUID checkitemID, UUID userID) throws SQLException
	{
		getInstance().unlink(checkitemID, userID);
	}

	public boolean isChecked(UUID checkitemID, UUID userID) throws SQLException
	{
		return getInstance().isLinked(checkitemID, userID);
	}
}
