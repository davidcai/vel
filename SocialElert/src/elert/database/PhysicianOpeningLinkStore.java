package elert.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.database.LinkStore;
import samoyan.database.LinkTableDef;

public class PhysicianOpeningLinkStore extends LinkStore
{
	private static PhysicianOpeningLinkStore instance = new PhysicianOpeningLinkStore();

	protected PhysicianOpeningLinkStore()
	{
	}
	
	public final static PhysicianOpeningLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = LinkTableDef.newInstance("PhysicianOpeningLink");

		td.setKey1("PhysicianID", "Users");
		td.setKey2("OpeningID", "Openings");
		
		return td;
	}

	// - - -
	
	public List<UUID> getPhysiciansByOpening(UUID openingID) throws SQLException
	{
		return getByKey2(openingID);
	}
			
	public void unlinkAllPhysicians(UUID openingID) throws Exception
	{
		unlinkByKey2(openingID);
	}

	public void linkPhysician(UUID openingID, UUID physicianID) throws SQLException
	{
		link(physicianID, openingID);
	}
	
	public void unlinkPhysician(UUID openingID, UUID physicianID) throws SQLException
	{
		unlink(physicianID, openingID);
	}
}
