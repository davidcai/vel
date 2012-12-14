package samoyan.database;

import java.sql.SQLException;
import java.util.*;

import samoyan.core.ParameterList;
import samoyan.core.Util;
import samoyan.servlet.Setup;

public final class UserStore extends DataBeanStore<User>
{
	private static UserStore instance = new UserStore();

	protected UserStore()
	{
	}
	public final static UserStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<User> getBeanClass()
	{
		return User.class;
	}	

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("Users");
		
		td.defineCol("LoginName", String.class).size(User.MINSIZE_LOGINNAME, User.MAXSIZE_LOGINNAME);
		td.defineCol("Password", String.class).size(User.MINSIZE_PASSWORD, User.MAXSIZE_PASSWORD);
		td.defineCol("Email", String.class).size(0, User.MAXSIZE_EMAIL);
		td.defineCol("Name", String.class).size(User.MINSIZE_NAME, User.MAXSIZE_NAME);
		td.defineCol("Joined", Date.class);
		td.defineCol("LastActive", Date.class);
		td.defineCol("Suspended", Boolean.class);
		td.defineCol("Terminated", Boolean.class);
		
		td.defineProp("TimeZone", TimeZone.class);
		td.defineProp("Locale", Locale.class);

		td.defineProp("Phone", String.class);
		td.defineProp("PhoneVerifyCode", String.class);
		td.defineProp("Mobile", String.class);
		td.defineProp("MobileCarrierID", UUID.class);
		td.defineProp("MobileVerifyCode", String.class);
		td.defineProp("Facebook", String.class).size(0, User.MAXSIZE_FACEBOOK);
		td.defineProp("Twitter", String.class).size(0, User.MAXSIZE_TWITTER);
		td.defineProp("XMPP", String.class);
		td.defineProp("Gender", String.class).size(0, 1);
		td.defineProp("Birthday", Date.class);

		td.defineProp("Avatar", Image.class);
		
		td.defineProp("PasswordResetCode", String.class);
		td.defineProp("PasswordResetDate", Date.class);
	
		td.defineProp("GuidedSetupPages", String.class);
		td.defineProp("GuidedSetupStep", Integer.class);

		return td;
	}

	// - - -
	
//	/**
//	 * Verifies that the user and password match those in the database.
//	 * @param id The user ID.
//	 * @param password The password.
//	 * @param rememberUser If <code>true</code>, the cookie will be stored long term.
//	 * @return A newly assigned cookie for the user or <code>null<code> if the user was not authenticated.
//	 * @throws SQLException 
//	 */
//	public UUID authenticate(UUID id, String password, boolean rememberUser) throws Exception
//	{
//		if (id==null) return null;//
//		User user = open(id);
//		if (user==null || user.isPassword(password)==false || user.isSuspended() || user.isTerminated())
//		{
//			return null;
//		}
//		
//		Date now = new Date();
//		UUID cookie = UUID.randomUUID();
//		
//		user.setCookie(cookie);
//		user.setCookieCreated(now);
//		user.setLastLogin(now);
//		user.setLastActive(now);
//		user.setRememberLogin(rememberUser);
//		save(user);
//		
//		Cache.insert("cookie:" + cookie.toString(), user.getID());
//				
//		return cookie;
//	}
	
//	public UUID impersonate(UUID id) throws Exception
//	{
//		if (id==null) return null;
//		User user = open(id);
//		if (user==null)
//		{
//			return null;
//		}
//		
//		Date now = new Date();
//		UUID cookie = UUID.randomUUID();
//		
//		user.setCookie(cookie);
//		user.setCookieCreated(now);
//		user.setRememberLogin(false);
//		save(user);
//		
//		Cache.insert("cookie:" + cookie.toString(), user.getID());
//				
//		return cookie;
//	}
	
//	/**
//	 * Returns the user ID matching the cookie or <code>null</code> if the cookie is invalid.
//	 * @param cookie
//	 * @return
//	 * @throws SQLException
//	 * @throws ParseException
//	 */
//	public UUID authenticateCookie(UUID cookie) throws Exception
//	{
//		if (cookie==null) return null;
//		
//		// Locate the user matching the cookie
//		boolean cacheFound = true;
//		UUID userID = (UUID) Cache.get("cookie:" + cookie.toString());
//		if (userID==null)
//		{
//			cacheFound = false;
//			
//			List<UUID> query = Query.queryListUUID("SELECT ID FROM Users WHERE Cookie=? ORDER BY LastLogin DESC", new ParameterList(cookie));
//			if (query.size()==1)
//			{
//				userID = query.get(0);
//			}
//		}
//		
//		if (userID!=null && cacheFound==false)
//		{
//			Cache.insert("cookie:" + cookie.toString(), userID);
//		}
//		
//		// Check validity of the User
//		User user = load(userID);
//		if (user==null || user.isSuspended() || user.isTerminated() ||
//			user.getCookie()==null || user.getCookie().equals(cookie)==false || user.getCookieCreated()==null)
//		{
//			return null;
//		}
//		
//		// Check if their cookie is still valid
//		Date now = new Date();
//		long cookieValidUntil = user.getCookieCreated().getTime() + (user.isRememberLogin() ? Setup.getCookieExpires() : Setup.getSessionLength());
//		if (cookieValidUntil < now.getTime())
//		{
//			// Expired cookie
//			return null;
//		}
//		
//		// Refresh cookie dates, if needed
//		if (user.getCookieCreated().getTime() + Setup.getSessionLength() < now.getTime())
//		{
//			// Update the last login date if the user returns after more than session length
//			user = open(userID);
//			user.setLastLogin(now);
//			user.setLastActive(now);
//			user.setCookieCreated(now);
//			save(user);
//		}
//		else if (user.getCookieCreated().getTime() + Setup.getSessionLength()/4L < now.getTime())
//		{
//			// Update the freshness date of the cookie every 1/4 session
//			user = open(userID);
//			user.setCookieCreated(now);
//			user.setLastActive(now);
//			save(user);
//		}
//
//		return userID;
//	}

//	/**
//	 * Deauthenticate the cookie for the given user.
//	 * @param userID The user ID to deauthenticate.
//	 * @throws SQLException
//	 */
//	public void deauthenticateCookie(UUID userID) throws Exception
//	{
//		if (userID==null) return;
//		
//		User user = open(userID);
//		if (user==null)
//		{
//			return;
//		}
//		
//		if (user.getCookie()!=null)
//		{
//			Cache.invalidate("cookie:" + user.getCookie().toString());
//		}
//
//		user.setCookie(null);
//		user.setCookieCreated(null);
//		user.setRememberLogin(false);
//		save(user);
//	}
	
	/**
	 * Returns the user IDs that have been seen within the last session.
	 * @return
	 * @throws SQLException 
	 */
	public List<UUID> getActive() throws SQLException
	{
		List<Object> params = new ArrayList<Object>();
		params.add(System.currentTimeMillis() - Setup.getSessionLength());
		return Query.queryListUUID("SELECT ID From Users WHERE LastActive>=?", params);
	}
		
	private void terminate(UUID id) throws Exception
	{
		User user = open(id);
		if (user==null) return;
		
		// Rename username to "username-term"
		int index = 1;
		String loginName;
		do
		{
			loginName = user.getLoginName();
			String suffix = (index==1? "" : String.valueOf(index));
			if (loginName.length() > User.MAXSIZE_LOGINNAME - 5 - suffix.length())
			{
				loginName = loginName.substring(0, User.MAXSIZE_LOGINNAME - 5 - suffix.length());
			}
			loginName += "-term" + suffix;
			index++;
		}
		while (loadByLoginName(loginName)!=null);
		user.setLoginName(loginName);

		user.setTerminated(true);
		user.setPassword(Util.randomPassword(User.MINSIZE_PASSWORD));
		save(user);		
	}
	
	/**
	 * Remove the user from the database. Generally, users should be terminated via {@link #terminate(UUID)}, not removed.
	 * @param userID
	 * @throws Exception
	 */
	public void remove(UUID userID) throws Exception
	{
		// Remove permission links associated with this user
		for (String perm : PermissionStore.getInstance().getPermissions(userID))
		{
			PermissionStore.getInstance().deauthorize(userID, perm);
		}

		if (canRemove(userID))
		{
			super.remove(userID);
		}
		else
		{
			terminate(userID);
		}
	}

	public QueryIterator<User> queryAllGhost() throws Exception
	{
		return createQueryIterator("SELECT * FROM Users ORDER BY LastActive DESC", null);
	}

	/**
	 * Returns the user ID with the give user name, or <code>null</code> if not found.
	 * @param loginName
	 * @return
	 * @throws Exception 
	 */
	public User loadByLoginName(String loginName) throws Exception
	{
		if (loginName==null)
		{
			return null;
		}
		return loadByColumn("LoginName", loginName.toLowerCase(Locale.US));
	}
	
	public User openByLoginName(String loginName) throws Exception
	{
		if (loginName==null)
		{
			return null;
		}
		return openByColumn("LoginName", loginName.toLowerCase(Locale.US));
	}
	
	/**
	 * Returns the user IDs with the given email address.
	 * @param email
	 * @return
	 * @throws Exception 
	 */
	public List<UUID> getByEmail(String email) throws Exception
	{
		return queryByColumn("Email", email);
	}
	
	/**
	 * Returns the user IDs with the given phone number.
	 * @param email
	 * @return
	 * @throws Exception 
	 */
	public List<UUID> getByPhone(String phone) throws Exception
	{
		return queryByColumn("Phone", phone);
	}

	/**
	 * Returns the user IDs with the given phone number.
	 * @param email
	 * @return
	 * @throws Exception 
	 */
	public List<UUID> getByMobile(String mobile) throws Exception
	{
		return queryByColumn("Mobile", mobile);
	}

	/**
	 * Returns the user IDs with the given birthday.
	 * @param email
	 * @return
	 * @throws Exception 
	 */
	public List<UUID> getByBirthday(Date birthday) throws Exception
	{
		return queryByColumn("Birthday", birthday);
	}

	/**
	 * Runs a search on users by name. Terminated users are not returned.
	 * @param queryString
	 * @return
	 * @throws SQLException
	 */
	public List<UUID> searchByName(String queryString) throws SQLException
	{
		if (queryString.length()>=3)
		{
			queryString = "%" + queryString;
		}
		queryString += "%";

		return Query.queryListUUID(
					"SELECT ID FROM Users WHERE (Name LIKE ?) AND Terminated=0", 
					new ParameterList(queryString));
	}
	
	/**
	 * Runs a search on users by name and group. Terminated users are not returned.
	 * @param queryString
	 * @param userGroupID
	 * @return IDs of users that match the search criteria
	 * @throws SQLException
	 */
	public List<UUID> searchByNameInGroup(String queryString, UUID userGroupID) throws SQLException
	{
		if (queryString.length()>=3)
		{
			queryString = "%" + queryString;
		}
		queryString += "%";

		String sql =	"SELECT ID FROM Users WHERE Users.Name LIKE ? AND Terminated=0 " +
						"AND ID IN (SELECT UserID FROM UserGroupsLink WHERE UserGroupID=?)";

		return Query.queryListUUID(sql, new ParameterList(queryString).plus(userGroupID));
	}	

	/**
	 * Returns an iterator over all users, ordered in reverse chronological order.
	 * The <code>User</code> returned by the <code>Iterator</code> is a ghost containing only the column data.
	 * @return
	 * @throws SQLException
	 */
	public QueryIterator<User> queryJoinedGhost(Date joinedAfter, Date joinedBefore) throws SQLException
	{
		ParameterList params = new ParameterList();
		String sql = "SELECT * FROM USERS WHERE 1=1";
		if (joinedAfter!=null)
		{
			sql += " AND Joined>=?";
			params.plus(joinedAfter);
		}
		if (joinedBefore!=null)
		{
			sql += " AND Joined<?";
			params.plus(joinedBefore);
		}
		sql += " ORDER BY Joined DESC";
		return createQueryIterator(sql, params);
	}
	
	/**
	 * Returns an iterator over active users, ordered in reverse chronological order of their activity date.
	 * The <code>User</code> returned by the <code>Iterator</code> is a ghost containing only the column data.
	 * @return
	 * @throws SQLException
	 */
	public QueryIterator<User> queryActiveGhost(Date activeAfter, Date activeBefore) throws SQLException
	{
		ParameterList params = new ParameterList();
		String sql = "SELECT * FROM Users WHERE NOT LastActive IS NULL";
		if (activeAfter!=null)
		{
			sql += " AND LastActive>=?";
			params.plus(activeAfter);
		}
		if (activeBefore!=null)
		{
			sql += " AND LastActive<?";
			params.plus(activeBefore);
		}
		sql += " ORDER BY LastActive DESC";
		return createQueryIterator(sql, params);
	}
	
	/**
	 * Strips the name of any special characters to generate a username.
	 * If another user has the same username, a number will be appended.
	 * @param name
	 * @return
	 * @throws Exception 
	 */
	public String generateUniqueLoginName(String name) throws Exception
	{
		String baseLoginName = Util.urlSafe(name);
		baseLoginName = Util.strReplace(baseLoginName, "-", "");
		
		int loginNameOrigLen = baseLoginName.length();
		while (baseLoginName.length()<User.MINSIZE_LOGINNAME)
		{
			baseLoginName += baseLoginName.substring(0, loginNameOrigLen);
		}

		String proposedLoginName = null;
		int loginNameIndex = 1;
		while (true)
		{
			String indexStr = (loginNameIndex>1?String.valueOf(loginNameIndex):"");
			proposedLoginName = baseLoginName + indexStr;
			if (proposedLoginName.length()>User.MAXSIZE_LOGINNAME)
			{
				baseLoginName = proposedLoginName.substring(0, User.MAXSIZE_LOGINNAME - indexStr.length());
				proposedLoginName = baseLoginName + indexStr;
			}
			
			if (loadByLoginName(proposedLoginName)==null) break;
			loginNameIndex++;
		}
		
		return proposedLoginName.toLowerCase(Locale.US);
	}
}
