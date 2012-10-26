package elert.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.database.LinkStore;
import samoyan.database.LinkTableDef;

public class ProcedureOpeningLinkStore extends LinkStore
{
	private static ProcedureOpeningLinkStore instance = new ProcedureOpeningLinkStore();

	protected ProcedureOpeningLinkStore()
	{
	}
	
	public final static ProcedureOpeningLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = LinkTableDef.newInstance("ProcedureOpeningLink");

		td.setKey1("ProcedureID", "Procedures").disallowRemoveIfHasLinks();
		td.setKey2("OpeningID", "Openings");
		
		return td;
	}

	// - - -
	
	public List<UUID> getProceduresByOpening(UUID openingID) throws SQLException
	{
		return getInstance().getByKey2(openingID);
	}
			
	public void unlinkAllProcedures(UUID openingID) throws Exception
	{
		getInstance().unlinkByKey2(openingID);
	}

	public void linkProcedure(UUID openingID, UUID procedureID) throws SQLException
	{
		getInstance().link(procedureID, openingID);
	}
	
	public void unlinkProcedure(UUID openingID, UUID procedureID) throws SQLException
	{
		getInstance().unlink(procedureID, openingID);
	}
}
