package samoyan.database;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.core.Util;
import samoyan.servlet.Setup;

public final class AuthTokenStore extends DataBeanStore<AuthToken>
{
	private static AuthTokenStore instance = new AuthTokenStore();

	protected AuthTokenStore()
	{
	}
	public final static AuthTokenStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<AuthToken> getBeanClass()
	{
		return AuthToken.class;
	}	
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("AuthTokens");
		
		td.defineCol("UserID", UUID.class).invariant().ownedBy("Users");
		td.defineCol("UserAgentSignature", String.class).size(0, AuthToken.MAXSIZE_SIGNATURE).invariant();
		td.defineCol("Life", Long.class).invariant();
		td.defineCol("LastAccessed", Date.class);
		td.defineCol("ApplePushToken", byte[].class);

		return td;
	}

	// - - -

	public UUID createAuthToken(UUID userID, String userAgentString, boolean longSession, String applePushToken) throws Exception
	{
		// The token may be in the form "<d742ba4f c3c22bca 140d0253 f7341a9f 75cb3c14 54adabd7 0d85049c 39e3a27c>"
		if (applePushToken!=null)
		{
			StringBuilder cleanToken = new StringBuilder(64);
			for (int i=0; i<applePushToken.length(); i++)
			{
				char ch = applePushToken.charAt(i);
				if ((ch>='0' && ch<='9') || (ch>='a' && ch<='f') || (ch>='A' && ch<='F'))
				{
					cleanToken.append(ch);
				}
			}
			applePushToken = cleanToken.toString();
		}
		
		AuthToken token = new AuthToken();
		token.setUserID(userID);
		token.setUserAgentSignature(userAgentString);
		if (longSession)
		{
			token.setLife(Setup.getCookieExpires());
		}
		token.setApplePushToken(applePushToken);
		AuthTokenStore.getInstance().save(token);
		
		return token.getID();
	}
	
	/**
	 * Returns the user ID matching the cookie or <code>null</code> if the cookie is invalid.
	 */
	public UUID validateAuthToken(UUID authTokenID, String userAgentSig) throws Exception
	{
		Date now = new Date();
		
		// Validate the token is valid
		AuthToken token = load(authTokenID);
		if (token==null || token.isUserAgentSignature(userAgentSig)==false)
		{
			return null;
		}
		
		if (token.isExpired(now))
		{
			remove(authTokenID);
			return null;
		}

		// Validate the user is valid
		User user = UserStore.getInstance().load(token.getUserID());
		if (user==null || user.isSuspended() || user.isTerminated())
		{
			remove(authTokenID);
			return null;
		}

		// Update the freshness date of the token every 1/4 session
		if (token.getLastAccessed().getTime() + Setup.getSessionLength() / 4 < now.getTime())
		{
			token = (AuthToken) token.clone(); // Clone for writing
			token.setLastAccessed(now);
			save(token);
		}
		
		return token.getUserID();
	}
	
	public void deauthenticateUser(UUID userID) throws Exception
	{
		removeMany(queryByColumn("UserID", userID));
	}
	
	public void removeExpired() throws SQLException, Exception
	{
		removeMany(Query.queryListUUID("SELECT ID FROM AuthTokens WHERE LastAccessed+Life<?", new ParameterList(new Date())));
	}
	
	public List<String> getApplePushTokensByUser(UUID userID) throws SQLException
	{
		return Query.queryListString("SELECT ApplePushToken FROM AuthTokens WHERE UserID=? AND (LastAccessed+Life>=?)", new ParameterList(userID).plus(new Date()));
	}
	
	public void invalidateApplePushToken(String token) throws SQLException
	{
		Query q = new Query();
		try
		{
			q.update("UPDATE AuthTokens SET ApplePushToken=NULL WHERE ApplePushToken=?", new ParameterList(Util.hexStringToByteArray(token)));
		}
		finally
		{
			q.close();
		}
	}
}
