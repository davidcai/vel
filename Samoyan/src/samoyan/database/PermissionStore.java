package samoyan.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.core.Cache;
import samoyan.core.ParameterList;
import samoyan.core.Util;

public final class PermissionStore extends DataBeanStore<Permission>
{
	private static PermissionStore instance = new PermissionStore();

	protected PermissionStore()
	{
	}
	public final static PermissionStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Permission> getBeanClass()
	{
		return Permission.class;
	}	
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Permissions", this);
		
		td.defineCol("Name", String.class).size(0, Permission.MAXSIZE_NAME).invariant();
		
		return td;
	}

	// - - -
	
	/**
	 * Loads the permission with the given name. A new permission object will be created if one does not already exist.
	 * @param permissionName
	 * @return
	 * @throws Exception
	 */
	private Permission loadByName(String permissionName) throws Exception
	{
		Permission p = getInstance().loadByColumn("Name", permissionName);
		if (p==null)
		{
			p = new Permission();
			p.setName(permissionName);
			try
			{
				getInstance().save(p);
			}
			catch (Exception e)
			{
				// Can be due to duplicate key, so try loading again
				p = getInstance().loadByColumn("Name", permissionName);
				if (p==null)
				{
					throw e;
				}
			}
		}
		return p;
	}
		
	public List<String> searchByName(String queryString) throws SQLException
	{
		if (queryString.length()>=3)
		{
			queryString = "%" + queryString;
		}
		queryString += "%";

		return Query.queryListString("SELECT Name FROM Permissions WHERE (Name LIKE ?)", new ParameterList(queryString));
	}

	// - - -

	/**
	 * Returns the set of permissions that are associated with the user or group.
	 * Terminated or suspended users are not excluded from result.
	 * @param groupID
	 * @return
	 * @throws SQLException 
	 */
	public Set<String> getPermissions(UUID objectID) throws SQLException
	{
		String cacheKey = "permission:" + objectID.toString();
		Set<String> cached = (Set<String>) Cache.get(cacheKey);
		if (cached!=null)
		{
			return cached;
		}
		

		Query q = new Query();
		try
		{
			ResultSet rs = q.select("SELECT Permissions.Name FROM Permissions, PermissionsLink WHERE Permissions.ID=PermissionsLink.PermissionID AND PermissionsLink.ObjectID=?",
									new ParameterList(objectID));
			Set<String> result = new HashSet<String>();
			while (rs.next())
			{
				result.add(rs.getString(1));
			}
			Cache.insert(cacheKey, result);
			return result;
		}
		finally
		{
			q.close();
		}
	}
	
	public List<UUID> getPrinciples(String permissionName) throws Exception
	{
		String cacheKey = "permission:" + permissionName;
		List<UUID> cached = (List<UUID>) Cache.get(cacheKey);
		if (cached!=null)
		{
			return cached;
		}
		
		Permission perm = (Permission) getInstance().loadByColumn("Name", permissionName);
		if (perm==null)
		{
			return new ArrayList<UUID>();
		}
		else
		{
			List<UUID> result = Query.queryListUUID("SELECT ObjectID FROM PermissionsLink WHERE PermissionID=?", new ParameterList(perm.getID()));
			Cache.insert(cacheKey, result);
			return result;
		}
	}

	/**
	 * Indicates if a user was granted a permission, also considering the groups he's a member of.
	 * @param objectID The user ID.
	 * @param permissionName The permission name.
	 * @return
	 * @throws SQLException
	 */
	public boolean isUserGrantedPermission(UUID userID, String permissionName) throws SQLException
	{
		if (userID==null || Util.isEmpty(permissionName)) return false;
		
		if (getPermissions(userID).contains(permissionName))
		{
			return true;
		}
		for (UUID groupID : UserUserGroupLinkStore.getInstance().getGroupsForUser(userID))
		{
			if (getPermissions(groupID).contains(permissionName))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Grants a permission to a user or user group.
	 * @param objectID The ID of the user or group.
	 * @param permissionName The name of the permission.
	 * @throws SQLException 
	 */
	public void authorize(UUID objectID, String permissionName) throws Exception
	{
		if (objectID==null || Util.isEmpty(permissionName)) return;

		Permission p = loadByName(permissionName);
		
		Query q = new Query();
		try
		{
			ResultSet rs = q.updatableSelect(	"SELECT PermissionID, ObjectID FROM PermissionsLink WHERE PermissionID=? AND ObjectID=?",
												new ParameterList(p.getID()).plus(objectID));
			if (!rs.next())
			{
				rs.moveToInsertRow();
				rs.updateBytes(1, Util.uuidToBytes(p.getID()));
				rs.updateBytes(2, Util.uuidToBytes(objectID));
				rs.insertRow();
				
				Cache.invalidate("permission:" + objectID.toString());
				Cache.invalidate("permission:" + permissionName);
			}
		}
		finally
		{
			q.close();
		}
	}
	
	/**
	 * Grants a permission to a user or user group.
	 * @param objectID The ID of the user or group.
	 * @param permissionName The name of the permission.
	 * @throws SQLException 
	 */
	public void deauthorize(UUID objectID, String permissionName) throws Exception
	{
		if (objectID==null) return;

		Permission p = loadByName(permissionName);

		Query q = new Query();
		try
		{
			int res = q.update(	"DELETE FROM PermissionsLink WHERE PermissionID=? AND ObjectID=?",
								new ParameterList(p.getID()).plus(objectID));
			if (res>0)
			{
				Cache.invalidate("permission:" + objectID.toString());
				Cache.invalidate("permission:" + permissionName);
			}
		}
		finally
		{
			q.close();
		}
	}
	
	public static List<String> getAll() throws SQLException
	{
		return Query.queryListString("SELECT Name From Permissions ORDER BY Name", null);
	}
	
	public void removeByName(String permissionName) throws Exception
	{
		Permission perm = (Permission) getInstance().loadByColumn("Name", permissionName);
		if (perm==null) return;
		
		List<UUID> granted = getPrinciples(permissionName);
		for (UUID uuid : granted)
		{
			deauthorize(uuid, permissionName);
		}
		
		getInstance().remove(perm.getID());
	}
}
