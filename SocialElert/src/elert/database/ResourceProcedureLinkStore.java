package elert.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.database.LinkStore;
import samoyan.database.LinkTableDef;

public class ResourceProcedureLinkStore extends LinkStore
{
	private static ResourceProcedureLinkStore instance = new ResourceProcedureLinkStore();

	protected ResourceProcedureLinkStore()
	{
	}
	public final static ResourceProcedureLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = LinkTableDef.newInstance("ResourceProcedureLink");
		
		td.setKey1("ResourceID", "Resources");
		td.setKey2("ProcedureID", "Procedures");
		td.setWeightColumn("Rank");
		
		return td;
	}

	// - - -

	public List<UUID> getResourcesForProcedure(UUID procedureID) throws SQLException
	{
		return getByKey2(procedureID);
	}

	public void unlinkAllResources(UUID procedureID) throws Exception
	{
		unlinkByKey2(procedureID);
	}

	public Integer getResourceRank(UUID procedureID, UUID resourceID) throws SQLException
	{
		return getWeight(resourceID, procedureID);
	}
	
	public void linkResource(UUID procedureID, UUID resourceID, int ranking) throws SQLException
	{
		link(resourceID, procedureID, ranking);
	}
	
	public void unlinkResource(UUID procedureID, UUID resourceID) throws SQLException
	{
		unlink(resourceID, procedureID);
	}

	/**
	 * The total ranking of a procedure is the sum of the ranking of its resources.
	 * @param procedureID
	 * @return
	 * @throws Exception
	 */
	public int getTotalRankForProcedure(UUID procedureID) throws Exception
	{
		int totalRank = 0;
		List<UUID> resourceIDs = getResourcesForProcedure(procedureID);
		for (UUID id : resourceIDs)
		{
			totalRank += getResourceRank(procedureID, id);
		}
		return totalRank;
	
// Code above is more efficient than querying the database due to internal caching by LinkStore
//		Query q = new Query();
//		try
//		{
//			ResultSet rs = q.select("SELECT SUM(Rank) FROM ResourcesLink WHERE ProcedureID=?", new ParameterList(procedureID));
//			if (rs.next())
//			{
//				return rs.getInt(1);
//			}
//			else
//			{
//				return 0;
//			}
//		}
//		finally
//		{
//			q.close();
//		}
	}
}
