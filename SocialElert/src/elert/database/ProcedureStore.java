package elert.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
import samoyan.database.TableDef;

public class ProcedureStore extends DataBeanStore<Procedure>
{
	private static ProcedureStore instance = new ProcedureStore();

	protected ProcedureStore()
	{
	}
	public final static ProcedureStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Procedure> getBeanClass()
	{
		return Procedure.class;
	}	

	@Override
	public TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Procedures", this);

		td.defineCol("Name", String.class).size(0, Procedure.MAXSIZE_NAME);	
		td.defineCol("TypeID", UUID.class).refersTo("ProcedureTypes");
		td.defineCol("Duration", Integer.class);
		td.defineCol("Lead", Integer.class);	
		td.defineCol("Custom", Boolean.class);
		td.defineCol("CommonName", String.class).size(0, Procedure.MAXSIZE_COMMON_NAME);	
		td.defineCol("ShortDesc", String.class).size(0, Procedure.MAXSIZE_SHORT_DESCRIPTION);	
		
		td.defineProp("Definition", String.class);	
		td.defineProp("Instructions", String.class);	
		td.defineProp("Notes", String.class);	
		td.defineProp("Video", String.class);	
		
		return td;
	}

	// - - -
		
//	@Override
//	public void remove(UUID procedureID) throws Exception
//	{	
//		Procedure procedure = load(procedureID);
//		if (procedure!=null)
//		{
//			super.remove(procedureID);
//		
//			removeOrphanedProcedureType(procedure.getTypeID());
//			
//			// !$! Also, remove orphaned Resources
//		}
//	}
//		
//	private void removeOrphanedProcedureType(UUID procedureType) throws Exception
//	{
//		Query q = new Query();
//		try
//		{
//			ResultSet rs = q.select("SELECT 1 FROM Procedures WHERE TypeID=?",  new ParameterList(procedureType));
//			if (!rs.next())
//			{
//				ProcedureTypeStore.getInstance().remove(procedureType);
//			}
//		}
//		finally
//		{
//			q.close();
//		}		
//	}

	public List<UUID> getAllIDs() throws Exception
	{
		return getAllBeanIDs("Name", true);
	}
		
	public Procedure loadByName(String name) throws Exception
	{
		return loadByColumn("Name", name);
	}	
	
	public List<UUID> searchByName(String queryString, boolean includeCustom) throws Exception
	{
		if (queryString.length()>=3)
		{
			queryString = "%" + queryString;
		}
		queryString += "%";
		
		if (includeCustom)
		{
			return Query.queryListUUID("SELECT ID FROM Procedures WHERE Name LIKE ?", new ParameterList(queryString));
		}
		else
		{
			return Query.queryListUUID("SELECT ID FROM Procedures WHERE Name LIKE ? AND Custom=0", new ParameterList(queryString));
		}
	}
	
	/**
	 * Returns the list of procedures of a given type, that are linked to a facility in the given service area.
	 * @return
	 * @throws SQLException 
	 */
	public List<UUID> queryByServiceAreaAndType(UUID areaID, UUID procedureTypeID) throws SQLException
	{
		String sql = "SELECT DISTINCT Procedures.ID, Procedures.Name FROM Procedures, ProcedureFacilityLink, Facilities WHERE " +
					"Facilities.ServiceAreaID=? AND Facilities.ID=ProcedureFacilityLink.FacilityID AND " +
					"ProcedureFacilityLink.ProcedureID=Procedures.ID AND Procedures.TypeID=? AND " +
					"ProcedureFacilityLink.Active<>0 " +
					"ORDER BY Procedures.Name ASC";
		return Query.queryListUUID(sql, new ParameterList(areaID).plus(procedureTypeID));
	}

	/**
	 * Returns a list of the standard (non-custom) procedures, of a given type, ordered by name.
	 * @return
	 * @throws Exception
	 */
	public List<UUID> queryStandardByType(UUID procedureTypeID) throws Exception
	{
		String sql = "SELECT ID FROM Procedures WHERE Custom=0 AND TypeID=? ORDER BY NAME ASC";
		return Query.queryListUUID(sql, new ParameterList(procedureTypeID));
	}	
}
