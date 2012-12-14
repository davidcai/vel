package elert.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.database.LinkStore;
import samoyan.database.LinkTableDef;

public class PhysicianProcedureTypeLinkStore extends LinkStore
{
	private static PhysicianProcedureTypeLinkStore instance = new PhysicianProcedureTypeLinkStore();

	protected PhysicianProcedureTypeLinkStore()
	{
	}
	
	public final static PhysicianProcedureTypeLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = createLinkTableDef("PhysicianProcedureTypeLink");

		td.setKey1("PhysicianID", "Users");
		td.setKey2("ProcedureTypeID", "ProcedureTypes");
		
		return td;
	}

	// - - -
	
	/**
	 * Returns the list of procedure types that this physician specializes in.
	 * @param openingID
	 * @return
	 * @throws SQLException
	 */
	public List<UUID> getPhysicianSpecialties(UUID typeID) throws SQLException
	{
		return getByKey2(typeID);
	}
			
	public void clearPhysicianSpecialties(UUID physicianID) throws Exception
	{
		unlinkByKey1(physicianID);
	}

	public void addPhysicianSpecialty(UUID physicianID, UUID typeID) throws SQLException
	{
		link(physicianID, typeID);
	}

	public boolean isPhysicianSpecialized(UUID physicianID, UUID typeID) throws SQLException
	{
		return isLinked(physicianID, typeID);
	}	
}
