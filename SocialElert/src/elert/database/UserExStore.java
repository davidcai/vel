package elert.database;

import java.util.UUID;

import samoyan.database.DataBeanStore;
import samoyan.database.TableDef;

public class UserExStore extends DataBeanStore<UserEx>
{
	private static UserExStore instance = new UserExStore();
	
	protected UserExStore()
	{
	}

	public final static UserExStore getInstance()
	{
		return instance;		
	}

	@Override
	protected Class<UserEx> getBeanClass()
	{
		return UserEx.class;
	}

	@Override
	public TableDef defineMapping()
	{
		TableDef td = createTableDef("UsersEx");
		
		td.defineCol("UserID", UUID.class).ownedBy("Users");
		td.defineCol("MRN", String.class).size(0, UserEx.MAXSIZE_MRN);
		td.defineCol("NUID", String.class).size(UserEx.SIZE_NUID, UserEx.SIZE_NUID);
		
		return td;
	}

	// - - -

	/**
	 * Returns the UserEx object associated with the user.
	 * If a record does not exist, it will be created.
	 * @param userID
	 * @return
	 * @throws Exception
	 */
	public UserEx loadByUserID(UUID userID) throws Exception
	{
		if (userID==null) return null;
		
		UserEx userEx = loadByColumn("UserID", userID);
		if (userEx==null)
		{
			userEx = new UserEx();
			userEx.setUserID(userID);
			save(userEx);
		}
		return userEx;
	}
	
	/**
	 * Returns the UserEx object associated with the user.
	 * If a record does not exist, it will be created.
	 * @param userID
	 * @return
	 * @throws Exception
	 */
	public UserEx openByUserID(UUID userID) throws Exception
	{
		if (userID==null) return null;

		UserEx userEx = openByColumn("UserID", userID);
		if (userEx==null)
		{
			userEx = new UserEx();
			userEx.setUserID(userID);
			save(userEx);
		}
		return userEx;
	}
	
	public UserEx loadByNUID(String nuid) throws Exception
	{
		return loadByColumn("NUID", nuid);
	}

	public UserEx loadByMRN(String mrn) throws Exception
	{
		return loadByColumn("MRN", mrn);
	}	
}
