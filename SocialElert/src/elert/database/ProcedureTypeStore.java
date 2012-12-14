package elert.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
import samoyan.database.TableDef;

public class ProcedureTypeStore extends DataBeanStore<ProcedureType>
{
	private static ProcedureTypeStore instance = new ProcedureTypeStore();

	protected ProcedureTypeStore()
	{
	}

	public final static ProcedureTypeStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<ProcedureType> getBeanClass()
	{
		return ProcedureType.class;
	}

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("ProcedureTypes");

		td.defineCol("Name", String.class).size(0, ProcedureType.MAXSIZE_NAME);
		
		return td;
	}

	// - - -

	public ProcedureType loadByName(String name) throws Exception
	{
		return loadByColumn("Name", name);
	}

	public List<UUID> getAllIDs() throws Exception
	{
		return queryAll("Name", true);
	}

	public List<UUID> searchByText(String text) throws SQLException
	{
		if (text.length()>=3)
		{
			text = "%" + text;
		}
		text += "%";

		return Query.queryListUUID(
				"SELECT ID FROM ProcedureTypes WHERE (Name LIKE ?)", 
				new ParameterList(text));
	}
	
	/**
	 * Returns the list of procedure types that are linked to a facility in the given service area.
	 * @return
	 * @throws SQLException 
	 */
	public List<UUID> queryByServiceArea(UUID areaID) throws SQLException
	{
		String sql = "SELECT DISTINCT ProcedureTypes.ID, ProcedureTypes.Name FROM ProcedureTypes, Procedures, ProcedureFacilityLink, Facilities WHERE " +
					"Facilities.ServiceAreaID=? AND Facilities.ID=ProcedureFacilityLink.FacilityID AND " +
					"ProcedureFacilityLink.ProcedureID=Procedures.ID AND Procedures.TypeID=ProcedureTypes.ID AND " +
					"ProcedureFacilityLink.Active<>0 " +
					"ORDER BY ProcedureTypes.Name ASC";
		return Query.queryListUUID(sql, new ParameterList(areaID));
	}
}
