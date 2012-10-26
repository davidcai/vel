package elert.database;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
import samoyan.database.TableDef;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class RegionStore extends DataBeanStore<Region>
{
	private static RegionStore instance = new RegionStore();

	protected RegionStore()
	{
	}

	public final static RegionStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<Region> getBeanClass()
	{
		return Region.class;
	}

	@Override
	public TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Regions", this);

		td.defineCol("Name", String.class).size(0, Region.MAXSIZE_NAME);
		
		return td;
	}

	// - - -

	public List<UUID> getAllIDs() throws Exception
	{
		return getAllBeanIDs("Name", true);
	}

	public List<UUID> searchByName(String queryString) throws SQLException
	{
		if (queryString.length()>=3)
		{
			queryString = "%" + queryString;
		}
		queryString += "%";

		return Query.queryListUUID(
				"SELECT ID FROM Regions WHERE (Name LIKE ?)", 
				new ParameterList(queryString));
	}

	public Region loadByName(String regionName) throws Exception
	{
		return getInstance().loadByColumn("Name", regionName);
	}	
}
