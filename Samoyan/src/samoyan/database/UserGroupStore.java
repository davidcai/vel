package samoyan.database;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.core.ParameterList;

public final class UserGroupStore extends DataBeanStore<UserGroup>
{
	private static UserGroupStore instance = new UserGroupStore();

	protected UserGroupStore()
	{
	}
	public final static UserGroupStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<UserGroup> getBeanClass()
	{
		return UserGroup.class;
	}	
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("UserGroups");
		
		td.defineCol("Name", String.class).size(0, UserGroup.MAXSIZE_NAME);
		
		return td;
	}

	// - - -
		
	public UserGroup loadByName(String name) throws Exception
	{
		return loadByColumn("Name", name);
	}
	
	public void remove(UUID groupID) throws Exception
	{
		// Remove permission links associated with this group
		Set<String> perms = PermissionStore.getInstance().getPermissions(groupID);
		for (String perm : perms)
		{
			PermissionStore.getInstance().deauthorize(groupID, perm);
		}
		
		super.remove(groupID);
	}
	
	public List<UUID> searchByName(String queryString) throws SQLException
	{
		if (queryString.length()>=3)
		{
			queryString = "%" + queryString;
		}
		queryString += "%";

		return Query.queryListUUID(
				"SELECT ID FROM UserGroups WHERE (Name LIKE ?)", 
				new ParameterList(queryString));
	}
	
	public List<UUID> getAllIDs() throws Exception
	{
		return queryAll("Name", true);
	}
}



