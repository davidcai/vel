package elert.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.database.LinkStore;
import samoyan.database.LinkTableDef;

public class ServiceAreaUserLinkStore extends LinkStore
{
	private final static int TYPE_HOME = 1;
	private final static int TYPE_NEIGHBORING = 2;
	
	private static ServiceAreaUserLinkStore instance = new ServiceAreaUserLinkStore();

	protected ServiceAreaUserLinkStore()
	{
	}
	public final static ServiceAreaUserLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = LinkTableDef.newInstance("ServiceAreaUserLink");
		
		td.setKey1("ServiceAreaID", "ServiceAreas");
		td.setKey2("UserID", "Users");
		td.setWeightColumn("AreaType");
		
		return td;
	}
	
	// - - -
	
	public List<UUID> getHomeSerivceAreasForUser(UUID userID) throws SQLException
	{
		return getByKey2(userID, TYPE_HOME);
	}

	public List<UUID> getNeighboringSerivceAreasForUser(UUID userID) throws SQLException
	{
		return getByKey2(userID, TYPE_NEIGHBORING);
	}
	
	public void assignHomeAreaForUser(UUID userID, UUID areaID) throws SQLException
	{
		link(areaID, userID, TYPE_HOME);
	}
	
	public void assignNeighboringAreaForUser(UUID userID, UUID areaID) throws SQLException
	{
		link(areaID, userID, TYPE_NEIGHBORING);
	}
	
	public void unassignAreasForUser(UUID userID) throws SQLException
	{
		unlinkByKey2(userID);
	}	
}
