package elert.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
import samoyan.database.TableDef;

public class ServiceAreaStore extends DataBeanStore<ServiceArea>
{
	private static ServiceAreaStore instance = new ServiceAreaStore();

	protected ServiceAreaStore()
	{
	}

	public final static ServiceAreaStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<ServiceArea> getBeanClass()
	{
		return ServiceArea.class;
	}

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("ServiceAreas");

		td.defineCol("Name", String.class).size(0, ServiceArea.MAXSIZE_NAME);
		td.defineCol("RegionID", UUID.class).refersTo("Regions").invariant();

		return td;
	}

	// - - -

	public List<UUID> getAllIDs() throws Exception
	{
		return queryAll("Name", true);
	}

	public List<UUID> searchByName(String queryString) throws SQLException
	{
		if (queryString.length()>=3)
		{
			queryString = "%" + queryString;
		}
		queryString += "%";

		return Query.queryListUUID(
				"SELECT ID FROM ServiceAreas WHERE (Name LIKE ?)", 
				new ParameterList(queryString));
	}

	public ServiceArea loadByName(String name) throws Exception
	{
		return loadByColumn("Name", name);
	}

	public List<UUID> getByRegion(UUID regionID) throws SQLException
	{
		return queryByColumn("RegionID", regionID, "Name", true);
	}
}
