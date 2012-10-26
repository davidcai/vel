package samoyan.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public final class UserUserGroupLinkStore extends LinkStore
{
	private static UserUserGroupLinkStore instance = new UserUserGroupLinkStore();

	protected UserUserGroupLinkStore()
	{
	}
	public final static UserUserGroupLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = LinkTableDef.newInstance("UserGroupsLink");
		
		td.setKey1("UserID", "Users");
		td.setKey2("UserGroupID", "UserGroups");
		
		return td;
	}

	// - - -

	/**
	 * Returns the set of user IDs that are associated with this user group.
	 * Terminated or suspended users are not excluded from result.
	 * The returned list is not ordered in any particular order and in fact should be treated as a set.
	 * @param groupID
	 * @return
	 * @throws SQLException 
	 */
	public List<UUID> getUsersForGroup(UUID groupID) throws SQLException
	{
		return getByKey2(groupID);
	}

	/**
	 * Returns the set of groups that are associated with a given user.
	 * The returned list is not ordered in any particular order and in fact should be treated as a set.
	 * @param userID
	 * @return
	 * @throws SQLException 
	 */
	public List<UUID> getGroupsForUser(UUID userID) throws SQLException
	{
		return getByKey1(userID);
	}
	
	/**
	 * Joins a user to a group.
	 * @param userID
	 * @param groupID
	 * @throws SQLException 
	 */
	public void join(UUID userID, UUID groupID) throws SQLException
	{
		link(userID, groupID);
	}
		
	/**
	 * Unjoins a user from a group.
	 * @param userID
	 * @param groupID
	 * @throws SQLException 
	 */
	public void expel(UUID userID, UUID groupID) throws SQLException
	{
		unlink(userID, groupID);
	}
	
	public boolean isUserInGroup(UUID userID, UUID groupID) throws SQLException
	{
		return isLinked(userID, groupID);
	}	
}
