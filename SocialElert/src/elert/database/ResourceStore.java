package elert.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
import samoyan.database.TableDef;

public class ResourceStore extends DataBeanStore<Resource>
{
	private static ResourceStore instance = new ResourceStore();

	protected ResourceStore()
	{
	}

	public final static ResourceStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<Resource> getBeanClass()
	{
		return Resource.class;
	}

	@Override
	public TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Resources", this);

		td.defineCol("Name", String.class).size(0, Resource.MAXSIZE_NAME);
		
		return td;
	}

	// - - -

	public List<UUID> searchByName(String queryString) throws SQLException
	{
		if(queryString.length() >= 3)
		{
			queryString = "%" + queryString;
		}
		queryString += "%";

		return Query.queryListUUID("SELECT ID FROM Resources WHERE (Name LIKE ?)", new ParameterList(queryString));
	}

	public Resource loadByName(String name) throws Exception
	{
		return loadByColumn("Name", name);
	}	
}
